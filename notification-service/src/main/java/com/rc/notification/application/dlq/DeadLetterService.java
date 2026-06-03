package com.rc.notification.application.dlq;

import com.rc.notification.application.service.IngestionService;
import com.rc.notification.application.worker.DeliveryWorker;
import com.rc.notification.infrastructure.persistence.entity.NotificationDlqLogEntity;
import com.rc.notification.infrastructure.persistence.mapper.NotificationDlqLogMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 死信队列服务
 * <p>
 * 重试耗尽后执行：
 * 1. DLQ 落盘（INSERT notification_dlq_log）
 * 2. Redis 状态原子改写为 DEAD_LETTERED（TTL 不变）
 * 3. P1 告警日志输出
 * <p>
 * 同时实现 DeliveryWorker.DeadLetterHandler 接口，供 Worker 直接调用
 */
@Service
public class DeadLetterService implements DeliveryWorker.DeadLetterHandler {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterService.class);

    /**
     * P1 告警日志标识，供外部告警系统（飞书/钉钉）正则匹配拦截
     */
    private static final String P1_ALERT_MARKER = "[P1-DISASTER-ALERT]";

    private static final String STATUS_PREFIX = "status:dispatch:";

    private final NotificationDlqLogMapper dlqLogMapper;
    private final RedissonClient redissonClient;

    public DeadLetterService(NotificationDlqLogMapper dlqLogMapper,
                             RedissonClient redissonClient) {
        this.dlqLogMapper = dlqLogMapper;
        this.redissonClient = redissonClient;
    }

    /**
     * DeliveryWorker.DeadLetterHandler 接口实现
     */
    @Override
    public void handleDeadLetter(String bizSign, String traceId, String supplierCode,
                                 String unifiedContextJson, String errorMsg, int retryCount) {

        // 1. DLQ 落盘
        try {
            NotificationDlqLogEntity entity = new NotificationDlqLogEntity();
            entity.setBizSign(bizSign);
            entity.setTraceId(traceId != null ? traceId : "");
            entity.setSupplierCode(supplierCode);
            entity.setUnifiedContext(unifiedContextJson != null ? unifiedContextJson : "{}");
            entity.setErrorMsg(truncate(errorMsg, 65535));
            entity.setRetryCount(retryCount);
            entity.setDlqStatus(0); // 待处理

            dlqLogMapper.insert(entity);
            log.info("死信落盘成功: bizSign={}, supplierCode={}, retryCount={}",
                    bizSign, supplierCode, retryCount);
        } catch (Exception e) {
            // 唯一索引冲突（biz_sign 已存在）属正常防御
            log.warn("死信落盘异常（可能重复）: bizSign={}, error={}", bizSign, e.getMessage());
        }

        // 2. Redis 状态原子改写为 DEAD_LETTERED，保留原 TTL
        try {
            String statusKey = STATUS_PREFIX + bizSign;
            RBucket<String> bucket = redissonClient.getBucket(statusKey);

            // 读取当前剩余 TTL
            long remainTtlMs = bucket.remainTimeToLive();

            // 原子改写状态，保留原 TTL
            if (remainTtlMs > 0) {
                bucket.set(IngestionService.STATUS_DEAD_LETTERED, java.time.Duration.ofMillis(remainTtlMs));
            } else {
                bucket.set(IngestionService.STATUS_DEAD_LETTERED, java.time.Duration.ofHours(24));
            }

            log.info("Redis 状态改写为 DEAD_LETTERED: bizSign={}, remainTtlMs={}",
                    bizSign, remainTtlMs);
        } catch (Exception e) {
            log.error("Redis 状态改写失败: bizSign={}", bizSign, e);
        }

        // 3. P1 告警日志（供外部告警系统拦截）
        log.error("{} 死信降级触发！bizSign={}, supplierCode={}, retryCount={}, error={}",
                P1_ALERT_MARKER, bizSign, supplierCode, retryCount, truncate(errorMsg, 500));
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
