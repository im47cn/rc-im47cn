package com.rc.notification.application.worker;

import com.rc.notification.application.event.SupplierConfigActivatedEvent;
import com.rc.notification.application.event.SupplierConfigDeactivatedEvent;
import com.rc.notification.domain.config.SupplierConfigDomainService;
import com.rc.notification.infrastructure.metrics.NotificationMetricsRegistry;
import com.rc.notification.domain.config.SupplierConfig;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 供应商 Worker 管理器
 * <p>
 * 系统核心驱动，负责动态孵化各供应商的常驻轮询线程，
 * 维护红线隔离舱壁，实现 SmartLifecycle 优雅停机
 */
@Component
public class SupplierWorkerManager implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(SupplierWorkerManager.class);

    private final SupplierConfigDomainService configDomainService;
    private final RedissonClient redissonClient;
    private final NotificationMetricsRegistry metricsRegistry;

    @Value("${notification.worker.max-worker-threads:200}")
    private int maxWorkerThreads;

    @Value("${notification.worker.shutdown-await-seconds:30}")
    private int shutdownAwaitSeconds;

    /**
     * 常驻线程注册表：supplierCode -> Worker Future 列表
     */
    private final ConcurrentHashMap<String, List<Future<?>>> workerRegistry = new ConcurrentHashMap<>();

    /**
     * 停机标志位
     */
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    /**
     * 运行状态
     */
    private volatile boolean running = false;

    /**
     * Worker 线程池
     */
    private ExecutorService workerExecutor;

    /**
     * DeliveryWorker 工厂（由 Spring 注入，T9 实现后替换）
     */
    private DeliveryWorkerFactory deliveryWorkerFactory;

    public SupplierWorkerManager(SupplierConfigDomainService configDomainService,
                                 RedissonClient redissonClient,
                                 NotificationMetricsRegistry metricsRegistry) {
        this.configDomainService = configDomainService;
        this.redissonClient = redissonClient;
        this.metricsRegistry = metricsRegistry;
    }

    /**
     * 注入 DeliveryWorker 工厂（可选依赖，T9 实现后自动注入）
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setDeliveryWorkerFactory(DeliveryWorkerFactory deliveryWorkerFactory) {
        this.deliveryWorkerFactory = deliveryWorkerFactory;
    }

    // ==================== SmartLifecycle ====================

    @Override
    public void start() {
        log.info("SupplierWorkerManager 启动，最大线程数: {}", maxWorkerThreads);
        this.workerExecutor = Executors.newFixedThreadPool(maxWorkerThreads, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        this.running = true;
        initAndLaunchWorkers();
    }

    @Override
    public void stop() {
        stop(() -> {});
    }

    @Override
    public void stop(Runnable callback) {
        log.info("SupplierWorkerManager 开始优雅停机...");

        // 1. 设置停机标志位
        shutdownRequested.set(true);
        running = false;

        // 2. 等待在途请求完成
        if (workerExecutor != null) {
            workerExecutor.shutdown();
            try {
                if (!workerExecutor.awaitTermination(shutdownAwaitSeconds, TimeUnit.SECONDS)) {
                    // 3. 超时强制中断
                    log.warn("超过等待窗口 {}s，强制中断 Worker 线程", shutdownAwaitSeconds);
                    workerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                workerExecutor.shutdownNow();
            }
        }

        // 4. 资源释放
        workerRegistry.clear();
        log.info("SupplierWorkerManager 优雅停机完成");

        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // 确保 Worker 在其他 Bean 之前停机
        return Integer.MAX_VALUE - 1;
    }

    // ==================== 核心方法 ====================

    /**
     * 系统启动时初始化各渠道 Worker
     */
    public void initAndLaunchWorkers() {
        List<SupplierConfig> activeConfigs = configDomainService.getAllActive();
        log.info("初始化 Worker，发现 {} 个活跃供应商", activeConfigs.size());

        for (SupplierConfig config : activeConfigs) {
            try {
                launchWorkersForSupplier(config);
            } catch (WorkerCapacityExhaustedException e) {
                log.error("Worker 容量耗尽，无法为供应商 {} 拉起线程: {}",
                        config.getSupplierCode(), e.getMessage());
            }
        }
    }

    /**
     * 响应供应商启用事件，按需调整 Worker 线程数
     * <p>
     * 策略：
     * - 新供应商首次启用：全量拉起
     * - 并发数增加：仅补齐新增线程
     * - 并发数减少：优雅退出多余线程
     * - 并发数不变：不重启（配置变更通过缓存刷新自动生效）
     */
    @EventListener
    public void handleConfigActivatedEvent(SupplierConfigActivatedEvent event) {
        String supplierCode = event.getConfig() != null ? event.getConfig().getSupplierCode() : null;
        if (supplierCode == null) return;

        // 从领域服务重新获取最新配置
        SupplierConfig config = configDomainService.getBySupplierCode(supplierCode);
        if (config == null) return;

        int desiredConcurrency = config.getWorkerConcurrency() != null ? config.getWorkerConcurrency() : 1;
        List<Future<?>> currentFutures = workerRegistry.get(supplierCode);
        int currentCount = currentFutures != null ? currentFutures.size() : 0;

        if (currentCount == 0) {
            // 新供应商首次启用，全量拉起
            log.info("收到配置启用事件: supplierCode={}, 首次拉起 {} 个 Worker", supplierCode, desiredConcurrency);
            try {
                launchWorkersForSupplier(config);
            } catch (WorkerCapacityExhaustedException e) {
                log.error("Worker 容量耗尽，无法为供应商 {} 拉起线程: {}", supplierCode, e.getMessage());
            }
        } else if (desiredConcurrency > currentCount) {
            // 扩容：补齐新增线程
            int toAdd = desiredConcurrency - currentCount;
            log.info("供应商 {} 扩容: {} -> {}，补齐 {} 个 Worker", supplierCode, currentCount, desiredConcurrency, toAdd);
            try {
                scaleUpWorkers(supplierCode, currentFutures, toAdd);
            } catch (WorkerCapacityExhaustedException e) {
                log.error("Worker 容量耗尽，无法为供应商 {} 扩容: {}", supplierCode, e.getMessage());
            }
        } else if (desiredConcurrency < currentCount) {
            // 缩容：优雅退出多余线程
            int toRemove = currentCount - desiredConcurrency;
            log.info("供应商 {} 缩容: {} -> {}，优雅退出 {} 个 Worker", supplierCode, currentCount, desiredConcurrency, toRemove);
            scaleDownWorkers(supplierCode, currentFutures, toRemove);
        } else {
            // 并发数不变，其他配置变更通过缓存刷新自动生效，无需重启
            log.info("供应商 {} 配置变更，Worker 数量不变({}), 无需重启", supplierCode, currentCount);
        }
    }

    /**
     * 响应供应商停用事件，停止对应 Worker
     */
    @EventListener
    public void handleConfigDeactivatedEvent(SupplierConfigDeactivatedEvent event) {
        String supplierCode = event.getSupplierCode();
        log.info("收到配置停用事件: supplierCode={}", supplierCode);
        stopWorkersForSupplier(supplierCode);
    }

    /**
     * 查询当前活跃 Worker 线程总数（基于 workerRegistry 实际大小计算）
     */
    public int getActiveWorkerCount() {
        return workerRegistry.values().stream().mapToInt(List::size).sum();
    }

    /**
     * 查询线程池硬上限
     */
    public int getMaxWorkerThreads() {
        return maxWorkerThreads;
    }

    /**
     * 获取停机标志（供 DeliveryWorker 查询）
     */
    public boolean isShutdownRequested() {
        return shutdownRequested.get();
    }

    // ==================== 内部方法 ====================

    /**
     * 为指定供应商拉起 Worker 线程
     */
    private void launchWorkersForSupplier(SupplierConfig config) {
        String supplierCode = config.getSupplierCode();
        int concurrency = config.getWorkerConcurrency() != null ? config.getWorkerConcurrency() : 1;

        // 容量检查（基于 workerRegistry 实际大小）
        int currentCount = getActiveWorkerCount();
        if (currentCount + concurrency > maxWorkerThreads) {
            throw new WorkerCapacityExhaustedException(
                    String.format("活跃线程总数 %d + 请求 %d 超过硬上限 %d",
                            currentCount, concurrency, maxWorkerThreads));
        }

        List<Future<?>> futures = Collections.synchronizedList(new ArrayList<>());
        String queueName = "queue:notification:" + supplierCode;

        for (int i = 0; i < concurrency; i++) {
            String threadName = "worker-" + supplierCode + "-" + i;
            Runnable workerTask;

            if (deliveryWorkerFactory != null) {
                workerTask = deliveryWorkerFactory.create(supplierCode, queueName, this);
            } else {
                workerTask = createPlaceholderWorker(threadName);
            }

            // 包装 Runnable 以设置线程名
            Runnable namedTask = () -> {
                Thread.currentThread().setName(threadName);
                workerTask.run();
            };

            Future<?> future = workerExecutor.submit(namedTask);
            futures.add(future);
        }

        workerRegistry.put(supplierCode, futures);

        // 注册队列深度 Gauge
        metricsRegistry.registerQueueDepthGauge(supplierCode);

        log.info("为供应商 {} 拉起 {} 个 Worker 线程，当前总数: {}",
                supplierCode, concurrency, getActiveWorkerCount());
    }

    /**
     * 停止指定供应商的全部 Worker 线程
     */
    private void stopWorkersForSupplier(String supplierCode) {
        List<Future<?>> futures = workerRegistry.remove(supplierCode);
        if (futures != null && !futures.isEmpty()) {
            int count = futures.size();
            for (Future<?> future : futures) {
                future.cancel(true);
            }
            log.info("停止供应商 {} 的 {} 个 Worker 线程，当前总数: {}",
                    supplierCode, count, getActiveWorkerCount());
        }
    }

    /**
     * 扩容：为指定供应商补齐新增 Worker 线程
     */
    private void scaleUpWorkers(String supplierCode, List<Future<?>> currentFutures, int toAdd) {
        // 容量检查
        int currentTotal = getActiveWorkerCount();
        if (currentTotal + toAdd > maxWorkerThreads) {
            throw new WorkerCapacityExhaustedException(
                    String.format("活跃线程总数 %d + 请求 %d 超过硬上限 %d",
                            currentTotal, toAdd, maxWorkerThreads));
        }

        String queueName = "queue:notification:" + supplierCode;
        int baseIndex = currentFutures.size();

        for (int i = 0; i < toAdd; i++) {
            String threadName = "worker-" + supplierCode + "-" + (baseIndex + i);
            Runnable workerTask;

            if (deliveryWorkerFactory != null) {
                workerTask = deliveryWorkerFactory.create(supplierCode, queueName, this);
            } else {
                workerTask = createPlaceholderWorker(threadName);
            }

            Runnable namedTask = () -> {
                Thread.currentThread().setName(threadName);
                workerTask.run();
            };

            Future<?> future = workerExecutor.submit(namedTask);
            currentFutures.add(future);
        }

        log.info("供应商 {} 扩容完成，当前线程数: {}，全局总数: {}",
                supplierCode, currentFutures.size(), getActiveWorkerCount());
    }

    /**
     * 缩容：优雅退出指定供应商的多余 Worker 线程（从列表尾部移除）
     * <p>
     * 通过 cancel(true) 发送中断信号，Worker 在当前投递完成后检测中断并退出
     */
    private void scaleDownWorkers(String supplierCode, List<Future<?>> currentFutures, int toRemove) {
        // 从尾部逐个优雅取消（中断信号让 Worker 完成当前任务后退出）
        for (int i = 0; i < toRemove; i++) {
            int lastIndex = currentFutures.size() - 1;
            Future<?> future = currentFutures.remove(lastIndex);
            future.cancel(true);
        }

        log.info("供应商 {} 缩容完成，当前线程数: {}，全局总数: {}",
                supplierCode, currentFutures.size(), getActiveWorkerCount());
    }

    /**
     * 创建占位 Worker（DeliveryWorkerFactory 未注入时使用）
     */
    private Runnable createPlaceholderWorker(String threadName) {
        return () -> {
            log.info("占位 Worker 启动: {} (等待 DeliveryWorker 实现)", threadName);
            while (!shutdownRequested.get()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            log.info("占位 Worker 停止: {}", threadName);
        };
    }

    /**
     * DeliveryWorker 工厂接口（T9 实现后提供）
     */
    public interface DeliveryWorkerFactory {
        Runnable create(String supplierCode, String queueName, SupplierWorkerManager manager);
    }
}
