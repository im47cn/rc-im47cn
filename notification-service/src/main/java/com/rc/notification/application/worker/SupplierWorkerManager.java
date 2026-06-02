package com.rc.notification.application.worker;

import com.rc.notification.application.event.SupplierConfigActivatedEvent;
import com.rc.notification.domain.config.SupplierConfigDomainService;
import com.rc.notification.infrastructure.persistence.entity.SupplierConfigEntity;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Value("${notification.worker.max-worker-threads:200}")
    private int maxWorkerThreads;

    @Value("${notification.worker.shutdown-await-seconds:30}")
    private int shutdownAwaitSeconds;

    /**
     * 常驻线程注册表：supplierCode -> Worker 线程列表
     */
    private final ConcurrentHashMap<String, List<Thread>> workerRegistry = new ConcurrentHashMap<>();

    /**
     * 当前活跃 Worker 线程总数
     */
    private final AtomicInteger activeWorkerCount = new AtomicInteger(0);

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
                                 RedissonClient redissonClient) {
        this.configDomainService = configDomainService;
        this.redissonClient = redissonClient;
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
        activeWorkerCount.set(0);
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
        List<SupplierConfigEntity> activeConfigs = configDomainService.getAllActive();
        log.info("初始化 Worker，发现 {} 个活跃供应商", activeConfigs.size());

        for (SupplierConfigEntity config : activeConfigs) {
            try {
                launchWorkersForSupplier(config);
            } catch (WorkerCapacityExhaustedException e) {
                log.error("Worker 容量耗尽，无法为供应商 {} 拉起线程: {}",
                        config.getSupplierCode(), e.getMessage());
            }
        }
    }

    /**
     * 响应新渠道激活事件，动态拉起新 Worker
     */
    @EventListener
    public void handleConfigActivatedEvent(SupplierConfigActivatedEvent event) {
        SupplierConfigEntity config = event.getConfig();
        if (config == null || config.getStatus() == null || config.getStatus() != 1) {
            // 禁用的供应商，停止其 Worker
            if (config != null) {
                stopWorkersForSupplier(config.getSupplierCode());
            }
            return;
        }

        log.info("收到配置激活事件: supplierCode={}", config.getSupplierCode());

        // 先停止旧 Worker（如果存在），再拉起新的
        stopWorkersForSupplier(config.getSupplierCode());

        try {
            launchWorkersForSupplier(config);
        } catch (WorkerCapacityExhaustedException e) {
            log.error("Worker 容量耗尽，无法为供应商 {} 拉起线程: {}",
                    config.getSupplierCode(), e.getMessage());
        }
    }

    /**
     * 查询当前活跃 Worker 线程总数
     */
    public int getActiveWorkerCount() {
        return activeWorkerCount.get();
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
    private void launchWorkersForSupplier(SupplierConfigEntity config) {
        String supplierCode = config.getSupplierCode();
        int concurrency = config.getWorkerConcurrency() != null ? config.getWorkerConcurrency() : 1;

        // 容量检查
        if (activeWorkerCount.get() + concurrency > maxWorkerThreads) {
            throw new WorkerCapacityExhaustedException(
                    String.format("活跃线程总数 %d + 请求 %d 超过硬上限 %d",
                            activeWorkerCount.get(), concurrency, maxWorkerThreads));
        }

        List<Thread> threads = new ArrayList<>();
        String queueName = "queue:notification:" + supplierCode;

        for (int i = 0; i < concurrency; i++) {
            String threadName = "worker-" + supplierCode + "-" + i;
            Runnable workerTask;

            if (deliveryWorkerFactory != null) {
                workerTask = deliveryWorkerFactory.create(supplierCode, queueName, this);
            } else {
                // T9 尚未实现时的占位 Worker
                final int idx = i;
                workerTask = () -> {
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

            Thread thread = new Thread(workerTask, threadName);
            thread.setDaemon(true);
            threads.add(thread);
        }

        workerRegistry.put(supplierCode, threads);
        activeWorkerCount.addAndGet(concurrency);

        // 提交到线程池执行
        for (Thread thread : threads) {
            if (workerExecutor != null && !workerExecutor.isShutdown()) {
                workerExecutor.submit(thread);
            } else {
                thread.start();
            }
        }

        log.info("为供应商 {} 拉起 {} 个 Worker 线程，当前总数: {}",
                supplierCode, concurrency, activeWorkerCount.get());
    }

    /**
     * 停止指定供应商的 Worker 线程
     */
    private void stopWorkersForSupplier(String supplierCode) {
        List<Thread> threads = workerRegistry.remove(supplierCode);
        if (threads != null && !threads.isEmpty()) {
            int count = threads.size();
            for (Thread thread : threads) {
                thread.interrupt();
            }
            activeWorkerCount.addAndGet(-count);
            log.info("停止供应商 {} 的 {} 个 Worker 线程，当前总数: {}",
                    supplierCode, count, activeWorkerCount.get());
        }
    }

    /**
     * DeliveryWorker 工厂接口（T9 实现后提供）
     */
    public interface DeliveryWorkerFactory {
        Runnable create(String supplierCode, String queueName, SupplierWorkerManager manager);
    }
}
