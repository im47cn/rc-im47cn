package com.rc.notification.application.detection;

import com.rc.notification.domain.detection.ChangeRecord;
import com.rc.notification.domain.detection.ChangeRecordRepository;
import com.rc.notification.domain.detection.FieldFingerprint;
import com.rc.notification.domain.detection.FieldFingerprintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 运行时字段采样服务
 * <p>
 * 异步采样事件 payload，提取字段路径与类型，
 * 与已知 FieldFingerprint 对比，检测结构漂移。
 */
@Service
public class FieldSamplingService {

    private static final Logger log = LoggerFactory.getLogger(FieldSamplingService.class);

    /** 采样率阈值：前 100 条 100% 采样，之后 1% */
    private static final long FULL_SAMPLE_THRESHOLD = 100;

    private final FieldFingerprintRepository fingerprintRepo;
    private final ChangeRecordRepository changeRecordRepo;
    private final ConcurrentHashMap<String, AtomicLong> sampleCounters = new ConcurrentHashMap<>();

    public FieldSamplingService(FieldFingerprintRepository fingerprintRepo,
                                 ChangeRecordRepository changeRecordRepo) {
        this.fingerprintRepo = fingerprintRepo;
        this.changeRecordRepo = changeRecordRepo;
    }

    /**
     * 异步采样：提取 payload 字段路径，与已知指纹对比
     * <p>
     * 采样率：前 100 条事件 100%，之后 1% 随机采样。
     * 不阻塞主路径（入队逻辑）。
     */
    @Async
    public void sampleAsync(String eventTypeCode, Map<String, Object> payload) {
        try {
            // 采样率控制
            long count = sampleCounters
                    .computeIfAbsent(eventTypeCode, k -> new AtomicLong(0))
                    .incrementAndGet();
            if (count > FULL_SAMPLE_THRESHOLD && ThreadLocalRandom.current().nextInt(100) != 0) {
                return;
            }

            if (payload == null || payload.isEmpty()) {
                return;
            }

            // 提取所有字段路径
            Map<String, String> fieldPaths = extractFieldPaths("payload", payload);

            // 与已知指纹对比
            for (Map.Entry<String, String> entry : fieldPaths.entrySet()) {
                upsertFingerprint(eventTypeCode, entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            // 采样失败不影响主流程，仅记录日志
            log.warn("字段采样异常: eventTypeCode={}, error={}", eventTypeCode, e.getMessage());
        }
    }

    /**
     * 递归提取 payload 中所有字段路径及其类型
     *
     * @param prefix 当前路径前缀
     * @param data   当前层级数据
     * @return Map<fieldPath, observedType>
     */
    public Map<String, String> extractFieldPaths(String prefix, Map<String, Object> data) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String path = prefix + "." + entry.getKey();
            Object value = entry.getValue();
            String type = detectType(value);
            result.put(path, type);

            // 递归处理嵌套对象
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) value;
                result.putAll(extractFieldPaths(path, nested));
            }
        }
        return result;
    }

    /**
     * 检测值的 JSON 类型
     */
    private String detectType(Object value) {
        if (value == null) return "NULL";
        if (value instanceof String) return "STRING";
        if (value instanceof Number) return "NUMBER";
        if (value instanceof Boolean) return "BOOLEAN";
        if (value instanceof Map) return "OBJECT";
        if (value instanceof Collection || value.getClass().isArray()) return "ARRAY";
        return "STRING"; // 默认
    }

    /**
     * 插入或更新字段指纹，新字段时创建变更记录
     */
    private void upsertFingerprint(String eventTypeCode, String fieldPath, String observedType) {
        FieldFingerprint existing = fingerprintRepo.findByEventTypeCodeAndFieldPath(eventTypeCode, fieldPath);
        LocalDateTime now = LocalDateTime.now();

        if (existing == null) {
            // 新字段：插入指纹 + 创建变更记录
            FieldFingerprint fp = new FieldFingerprint();
            fp.setEventTypeCode(eventTypeCode);
            fp.setFieldPath(fieldPath);
            fp.setObservedType(observedType);
            fp.setFirstSeenAt(now);
            fp.setLastSeenAt(now);
            fp.setSampleCount(1);
            fp.setStatus("ACTIVE");
            fingerprintRepo.save(fp);

            // 创建 FIELD_ADDED 变更记录
            ChangeRecord cr = new ChangeRecord();
            cr.setEventTypeCode(eventTypeCode);
            cr.setChangeType("FIELD_ADDED");
            cr.setFieldPath(fieldPath);
            cr.setOldValue(null);
            cr.setNewValue(observedType);
            cr.setDetectionSource("RUNTIME_INFERRED");
            cr.setConfidence("MEDIUM");
            cr.setStatus("PENDING_REVIEW");
            cr.setCreatedAt(now);
            changeRecordRepo.save(cr);

            log.info("检测到新字段: eventType={}, field={}, type={}", eventTypeCode, fieldPath, observedType);

        } else {
            // 已知字段：更新采样计数和最后出现时间
            existing.setLastSeenAt(now);
            existing.setSampleCount(existing.getSampleCount() + 1);

            // 类型变化检测
            if (!observedType.equals(existing.getObservedType())) {
                ChangeRecord cr = new ChangeRecord();
                cr.setEventTypeCode(eventTypeCode);
                cr.setChangeType("FIELD_TYPE_CHANGED");
                cr.setFieldPath(fieldPath);
                cr.setOldValue(existing.getObservedType());
                cr.setNewValue(observedType);
                cr.setDetectionSource("RUNTIME_INFERRED");
                cr.setConfidence("LOW");
                cr.setStatus("PENDING_REVIEW");
                cr.setCreatedAt(now);
                changeRecordRepo.save(cr);

                log.info("检测到字段类型变化: eventType={}, field={}, {} -> {}",
                        eventTypeCode, fieldPath, existing.getObservedType(), observedType);
                existing.setObservedType(observedType);
            }

            fingerprintRepo.update(existing);
        }
    }
}
