package com.rc.notification.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.domain.config.SupplierConfigDomainService;
import com.rc.notification.infrastructure.metrics.NotificationMetricsRegistry;
import com.rc.notification.domain.config.SupplierConfig;
import com.rc.notification.interfaces.api.dto.IngestResponse;
import com.rc.notification.interfaces.api.dto.NotificationEventDto;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 事件摄取服务
 * <p>
 * 负责分布式锁拦截、幂等状态判重、压入 Redisson 队列
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private static final String LOCK_PREFIX = "lock:dispatch:";
    private static final String STATUS_PREFIX = "status:dispatch:";
    private static final long LOCK_TTL_SECONDS = 5;
    private static final long STATUS_TTL_HOURS = 24;
    private static final long SUCCESS_TTL_HOURS = 1;
    private static final String QUEUE_PREFIX = "queue:notification:";

    /** 幂等状态常量 */
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_DEAD_LETTERED = "DEAD_LETTERED";

    private final RedissonClient redissonClient;
    private final SupplierConfigDomainService configDomainService;
    private final ObjectMapper objectMapper;
    private final NotificationMetricsRegistry metricsRegistry;

    public IngestionService(RedissonClient redissonClient,
                            SupplierConfigDomainService configDomainService,
                            ObjectMapper objectMapper,
                            NotificationMetricsRegistry metricsRegistry) {
        this.redissonClient = redissonClient;
        this.configDomainService = configDomainService;
        this.objectMapper = objectMapper;
        this.metricsRegistry = metricsRegistry;
    }

    /**
     * 执行事件摄取入队
     *
     * @param eventDto 事件请求
     * @return 摄取响应
     * @throws RedisUnavailableException Redis 不可用时抛出
     */
    public IngestResponse ingest(NotificationEventDto eventDto) {
        String bizSign = eventDto.getEventId();
        String supplierCode = eventDto.getSupplierCode();

        // 校验供应商是否存在且启用
        SupplierConfig config = configDomainService.getBySupplierCode(supplierCode);
        if (config == null || config.getStatus() == null || config.getStatus() != 1) {
            metricsRegistry.recordIngest(supplierCode, "rejected");
            return IngestResponse.rejected(bizSign, "供应商不存在或未启用: " + supplierCode);
        }

        // 补充 traceId
        if (eventDto.getTraceId() == null || eventDto.getTraceId().isEmpty()) {
            eventDto.setTraceId("T-" + UUID.randomUUID());
        }

        String lockKey = LOCK_PREFIX + bizSign;
        String statusKey = STATUS_PREFIX + bizSign;

        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 1. 获取分布式锁，防并发击穿
            boolean acquired = lock.tryLock(0, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("获取分布式锁失败，可能存在并发请求: bizSign={}", bizSign);
                return IngestResponse.idempotentHit(bizSign, "LOCK_CONTENTION");
            }

            try {
                // 2. 检查幂等状态
                var bucket = redissonClient.getBucket(statusKey);
                Object currentStatus = bucket.get();
                if (currentStatus != null) {
                    String statusStr = currentStatus.toString();
                    log.info("幂等命中，跳过重复入队: bizSign={}, supplierCode={}, currentStatus={}",
                            bizSign, supplierCode, statusStr);
                    metricsRegistry.recordIngest(supplierCode, "idempotent_hit");
                    if (STATUS_DEAD_LETTERED.equals(statusStr)) {
                        return IngestResponse.deadLettered(bizSign);
                    }
                    return IngestResponse.idempotentHit(bizSign, statusStr);
                }

                // 3. 设置状态为 PROCESSING，TTL 24h
                bucket.set(STATUS_PROCESSING, Duration.ofHours(STATUS_TTL_HOURS));

                // 4. 构建队列消息并压入 Redisson 队列
                String queueName = QUEUE_PREFIX + supplierCode;
                RQueue<String> queue = redissonClient.getQueue(queueName);
                String message = serializeEvent(eventDto);
                try {
                    queue.add(message);
                } catch (Exception e) {
                    // 入队失败，回滚已设置的 PROCESSING 状态
                    bucket.delete();
                    throw e;
                }

                log.info("事件入队成功: bizSign={}, supplierCode={}, traceId={}",
                        bizSign, supplierCode, eventDto.getTraceId());

                metricsRegistry.recordIngest(supplierCode, "accepted");
                return IngestResponse.accepted(bizSign);
            } finally {
                // 5. 释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (RedisException e) {
            log.error("Redis 操作异常，触发降级: bizSign={}", bizSign, e);
            throw new RedisUnavailableException("Redis 不可用，请稍后重试", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisUnavailableException("获取锁被中断", e);
        }
    }

    /**
     * 序列化事件为 JSON 字符串（用于队列存储）
     */
    private String serializeEvent(NotificationEventDto eventDto) {
        try {
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("eventId", eventDto.getEventId());
            eventMap.put("traceId", eventDto.getTraceId());
            eventMap.put("supplierCode", eventDto.getSupplierCode());
            eventMap.put("eventType", eventDto.getEventType());
            eventMap.put("tenantCode", eventDto.getTenantCode());
            eventMap.put("userId", eventDto.getUserId());
            eventMap.put("cmd", eventDto.getCmd());
            eventMap.put("payload", eventDto.getPayload());
            eventMap.put("timestamp", System.currentTimeMillis());
            return objectMapper.writeValueAsString(eventMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("事件序列化失败", e);
        }
    }

    /**
     * Redis 不可用异常
     */
    public static class RedisUnavailableException extends RuntimeException {
        public RedisUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
