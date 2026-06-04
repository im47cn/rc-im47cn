package com.rc.notification.application.detection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.domain.detection.ChangeRecord;
import com.rc.notification.domain.detection.ChangeRecordRepository;
import com.rc.notification.domain.subscription.Subscription;
import com.rc.notification.domain.subscription.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 事件变更检测与影响分析服务
 * <p>
 * Track 1: Schema 精确 Diff（payloadSchema 存在时）
 * 对比新旧 Schema 的顶层属性差异，生成高置信度变更记录。
 */
@Service
public class ChangeDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ChangeDetectionService.class);

    private final ChangeRecordRepository changeRecordRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final ObjectMapper objectMapper;

    public ChangeDetectionService(ChangeRecordRepository changeRecordRepo,
                                   SubscriptionRepository subscriptionRepo,
                                   ObjectMapper objectMapper) {
        this.changeRecordRepo = changeRecordRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * 检测 Schema 变更
     * <p>
     * 解析新旧 Schema JSON，对比顶层 properties 的 key 集合差异。
     * 新增 key → FIELD_ADDED，删除 key → FIELD_REMOVED。
     *
     * @param eventTypeCode 事件类型编码
     * @param oldSchema     旧 Schema JSON 字符串（可能为 null）
     * @param newSchema     新 Schema JSON 字符串
     */
    public void detectSchemaChange(String eventTypeCode, String oldSchema, String newSchema) {
        try {
            Set<String> oldKeys = extractTopLevelKeys(oldSchema);
            Set<String> newKeys = extractTopLevelKeys(newSchema);

            LocalDateTime now = LocalDateTime.now();

            // 新增字段
            Set<String> added = new HashSet<>(newKeys);
            added.removeAll(oldKeys);
            for (String key : added) {
                ChangeRecord cr = createSchemaChangeRecord(eventTypeCode, "FIELD_ADDED",
                        "payload." + key, null, key, now);
                ChangeRecord saved = changeRecordRepo.save(cr);
                analyzeImpact(saved);
            }

            // 删除字段
            Set<String> removed = new HashSet<>(oldKeys);
            removed.removeAll(newKeys);
            for (String key : removed) {
                ChangeRecord cr = createSchemaChangeRecord(eventTypeCode, "FIELD_REMOVED",
                        "payload." + key, key, null, now);
                ChangeRecord saved = changeRecordRepo.save(cr);
                analyzeImpact(saved);
            }

            if (!added.isEmpty() || !removed.isEmpty()) {
                log.info("Schema 变更检测完成: eventType={}, added={}, removed={}",
                        eventTypeCode, added.size(), removed.size());
            }
        } catch (Exception e) {
            log.warn("Schema 变更检测失败: eventTypeCode={}, error={}", eventTypeCode, e.getMessage());
        }
    }

    /**
     * 影响分析：检查哪些订阅的模板引用了变更字段
     */
    public void analyzeImpact(ChangeRecord record) {
        if (record == null || record.getFieldPath() == null) {
            return;
        }

        try {
            List<Subscription> subs = subscriptionRepo.findActiveByEventTypeCode(record.getEventTypeCode());
            List<String> affected = new ArrayList<>();

            // 提取短字段名用于模板匹配（如 "payload.orderId" → "orderId"）
            String fieldName = record.getFieldPath();
            String shortName = fieldName.contains(".")
                    ? fieldName.substring(fieldName.lastIndexOf('.') + 1) : fieldName;

            for (Subscription sub : subs) {
                if (templateReferencesField(sub, shortName) || templateReferencesField(sub, fieldName)) {
                    affected.add(sub.getSubscriberCode());
                }
            }

            if (!affected.isEmpty()) {
                record.setAffectedSubscriptions(objectMapper.writeValueAsString(affected));
                changeRecordRepo.update(record);
                log.info("影响分析完成: changeRecordId={}, affected={}",
                        record.getId(), affected);
            }
        } catch (Exception e) {
            log.warn("影响分析失败: changeRecordId={}, error={}", record.getId(), e.getMessage());
        }
    }

    /**
     * 检查订阅的模板是否引用了指定字段
     */
    private boolean templateReferencesField(Subscription sub, String fieldRef) {
        return containsField(sub.getBodyTemplate(), fieldRef)
                || containsField(sub.getPathTemplate(), fieldRef)
                || containsField(sub.getQueryTemplate(), fieldRef)
                || containsField(sub.getHeaderTemplate(), fieldRef);
    }

    private boolean containsField(String template, String fieldRef) {
        return template != null && template.contains(fieldRef);
    }

    /**
     * 提取 Schema JSON 的顶层 key（从 properties 或直接从根对象）
     */
    private Set<String> extractTopLevelKeys(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return Set.of();
        }
        try {
            Map<String, Object> schema = objectMapper.readValue(schemaJson,
                    new TypeReference<Map<String, Object>>() {});

            // JSON Schema 格式: { "properties": { "field1": {...}, "field2": {...} } }
            Object properties = schema.get("properties");
            if (properties instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> props = (Map<String, Object>) properties;
                return props.keySet();
            }

            // 简单 key-value 格式: { "field1": "type1", "field2": "type2" }
            return schema.keySet();
        } catch (Exception e) {
            log.warn("Schema 解析失败: {}", e.getMessage());
            return Set.of();
        }
    }

    private ChangeRecord createSchemaChangeRecord(String eventTypeCode, String changeType,
                                                   String fieldPath, String oldValue,
                                                   String newValue, LocalDateTime now) {
        ChangeRecord cr = new ChangeRecord();
        cr.setEventTypeCode(eventTypeCode);
        cr.setChangeType(changeType);
        cr.setFieldPath(fieldPath);
        cr.setOldValue(oldValue);
        cr.setNewValue(newValue);
        cr.setDetectionSource("SCHEMA_DIFF");
        cr.setConfidence("HIGH");
        cr.setStatus("PENDING_REVIEW");
        cr.setCreatedAt(now);
        return cr;
    }
}
