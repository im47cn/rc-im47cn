package com.rc.notification.infrastructure.metrics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rc.notification.application.worker.SupplierWorkerManager;
import com.rc.notification.infrastructure.persistence.entity.NotificationDlqLogEntity;
import com.rc.notification.infrastructure.persistence.mapper.NotificationDlqLogMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知系统 Micrometer 指标注册中心
 * <p>
 * 暴露 6 类核心业务 Metrics 至 /actuator/prometheus:
 * 1. notification.ingest.total - 入队请求总数
 * 2. notification.delivery.total - 投递尝试总数
 * 3. notification.delivery.duration - 单次投递耗时
 * 4. notification.queue.depth - 各供应商队列积压深度
 * 5. notification.worker.active - 当前活跃 Worker 线程数
 * 6. notification.dlq.pending - 待处理死信数量
 */
@Component
public class NotificationMetricsRegistry {

    private static final String QUEUE_PREFIX = "queue:notification:";

    private final MeterRegistry meterRegistry;
    private final SupplierWorkerManager workerManager;
    private final RedissonClient redissonClient;
    private final NotificationDlqLogMapper dlqLogMapper;

    /**
     * Counter 缓存，避免重复注册
     */
    private final ConcurrentHashMap<String, Counter> ingestCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> deliveryCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> deliveryTimers = new ConcurrentHashMap<>();

    public NotificationMetricsRegistry(MeterRegistry meterRegistry,
                                       SupplierWorkerManager workerManager,
                                       RedissonClient redissonClient,
                                       NotificationDlqLogMapper dlqLogMapper) {
        this.meterRegistry = meterRegistry;
        this.workerManager = workerManager;
        this.redissonClient = redissonClient;
        this.dlqLogMapper = dlqLogMapper;
    }

    @PostConstruct
    public void registerGauges() {
        // 5. notification.worker.active - 当前活跃 Worker 线程数
        meterRegistry.gauge("notification.worker.active", workerManager,
                SupplierWorkerManager::getActiveWorkerCount);

        // 6. notification.dlq.pending - 待处理死信数量
        meterRegistry.gauge("notification.dlq.pending", this, self -> {
            try {
                LambdaQueryWrapper<NotificationDlqLogEntity> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(NotificationDlqLogEntity::getDlqStatus, 0);
                return self.dlqLogMapper.selectCount(wrapper);
            } catch (Exception e) {
                return 0;
            }
        });
    }

    /**
     * 1. 记录入队请求指标
     *
     * @param supplierCode 供应商编码
     * @param result       结果: accepted / rejected / idempotent_hit
     */
    public void recordIngest(String supplierCode, String result) {
        String key = supplierCode + ":" + result;
        ingestCounters.computeIfAbsent(key, k ->
                Counter.builder("notification.ingest.total")
                        .tag("supplier_code", supplierCode)
                        .tag("result", result)
                        .register(meterRegistry)
        ).increment();
    }

    /**
     * 2. 记录投递尝试指标
     *
     * @param supplierCode 供应商编码
     * @param outcome      结果: success / failed / dlq
     */
    public void recordDelivery(String supplierCode, String outcome) {
        String key = supplierCode + ":" + outcome;
        deliveryCounters.computeIfAbsent(key, k ->
                Counter.builder("notification.delivery.total")
                        .tag("supplier_code", supplierCode)
                        .tag("outcome", outcome)
                        .register(meterRegistry)
        ).increment();
    }

    /**
     * 3. 记录投递耗时指标
     *
     * @param supplierCode  供应商编码
     * @param durationMs    耗时（毫秒）
     */
    public void recordDeliveryDuration(String supplierCode, long durationMs) {
        deliveryTimers.computeIfAbsent(supplierCode, k ->
                Timer.builder("notification.delivery.duration")
                        .tag("supplier_code", supplierCode)
                        .register(meterRegistry)
        ).record(Duration.ofMillis(durationMs));
    }

    /**
     * 4. 获取指定供应商队列积压深度
     * <p>
     * 注意：此方法注册为 Gauge，由 Micrometer 周期性采样
     */
    public void registerQueueDepthGauge(String supplierCode) {
        String queueName = QUEUE_PREFIX + supplierCode;
        meterRegistry.gauge("notification.queue.depth",
                io.micrometer.core.instrument.Tags.of("supplier_code", supplierCode),
                redissonClient,
                client -> {
                    try {
                        RQueue<String> queue = client.getQueue(queueName);
                        return queue.size();
                    } catch (Exception e) {
                        return 0;
                    }
                });
    }
}
