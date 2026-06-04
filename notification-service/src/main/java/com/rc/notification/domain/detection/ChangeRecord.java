package com.rc.notification.domain.detection;

import com.rc.notification.infrastructure.persistence.entity.ChangeRecordEntity;

import java.time.LocalDateTime;

/**
 * 变更记录领域模型
 * <p>
 * 纯 POJO，不依赖任何持久化框架注解
 */
public class ChangeRecord {

    private Long id;
    private String eventTypeCode;
    private String changeType;
    private String fieldPath;
    private String oldValue;
    private String newValue;
    private String detectionSource;
    private String confidence;
    private String status;
    private String affectedSubscriptions;
    private LocalDateTime createdAt;

    /**
     * 从持久化实体转换为领域模型
     */
    public static ChangeRecord fromEntity(ChangeRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        ChangeRecord record = new ChangeRecord();
        record.setId(entity.getId());
        record.setEventTypeCode(entity.getEventTypeCode());
        record.setChangeType(entity.getChangeType());
        record.setFieldPath(entity.getFieldPath());
        record.setOldValue(entity.getOldValue());
        record.setNewValue(entity.getNewValue());
        record.setDetectionSource(entity.getDetectionSource());
        record.setConfidence(entity.getConfidence());
        record.setStatus(entity.getStatus());
        record.setAffectedSubscriptions(entity.getAffectedSubscriptions());
        record.setCreatedAt(entity.getCreatedAt());
        return record;
    }

    /**
     * 转换为持久化实体
     */
    public ChangeRecordEntity toEntity() {
        ChangeRecordEntity entity = new ChangeRecordEntity();
        entity.setId(this.id);
        entity.setEventTypeCode(this.eventTypeCode);
        entity.setChangeType(this.changeType);
        entity.setFieldPath(this.fieldPath);
        entity.setOldValue(this.oldValue);
        entity.setNewValue(this.newValue);
        entity.setDetectionSource(this.detectionSource);
        entity.setConfidence(this.confidence);
        entity.setStatus(this.status);
        entity.setAffectedSubscriptions(this.affectedSubscriptions);
        entity.setCreatedAt(this.createdAt);
        return entity;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventTypeCode() {
        return eventTypeCode;
    }

    public void setEventTypeCode(String eventTypeCode) {
        this.eventTypeCode = eventTypeCode;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getDetectionSource() {
        return detectionSource;
    }

    public void setDetectionSource(String detectionSource) {
        this.detectionSource = detectionSource;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAffectedSubscriptions() {
        return affectedSubscriptions;
    }

    public void setAffectedSubscriptions(String affectedSubscriptions) {
        this.affectedSubscriptions = affectedSubscriptions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
