package com.rc.notification.domain.detection;

import com.rc.notification.infrastructure.persistence.entity.FieldFingerprintEntity;

import java.time.LocalDateTime;

/**
 * 字段指纹领域模型
 * <p>
 * 纯 POJO，不依赖任何持久化框架注解
 */
public class FieldFingerprint {

    private Long id;
    private String eventTypeCode;
    private String fieldPath;
    private String observedType;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private Integer sampleCount;
    private String status;

    /**
     * 从持久化实体转换为领域模型
     */
    public static FieldFingerprint fromEntity(FieldFingerprintEntity entity) {
        if (entity == null) {
            return null;
        }
        FieldFingerprint fingerprint = new FieldFingerprint();
        fingerprint.setId(entity.getId());
        fingerprint.setEventTypeCode(entity.getEventTypeCode());
        fingerprint.setFieldPath(entity.getFieldPath());
        fingerprint.setObservedType(entity.getObservedType());
        fingerprint.setFirstSeenAt(entity.getFirstSeenAt());
        fingerprint.setLastSeenAt(entity.getLastSeenAt());
        fingerprint.setSampleCount(entity.getSampleCount());
        fingerprint.setStatus(entity.getStatus());
        return fingerprint;
    }

    /**
     * 转换为持久化实体
     */
    public FieldFingerprintEntity toEntity() {
        FieldFingerprintEntity entity = new FieldFingerprintEntity();
        entity.setId(this.id);
        entity.setEventTypeCode(this.eventTypeCode);
        entity.setFieldPath(this.fieldPath);
        entity.setObservedType(this.observedType);
        entity.setFirstSeenAt(this.firstSeenAt);
        entity.setLastSeenAt(this.lastSeenAt);
        entity.setSampleCount(this.sampleCount);
        entity.setStatus(this.status);
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

    public String getFieldPath() {
        return fieldPath;
    }

    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public String getObservedType() {
        return observedType;
    }

    public void setObservedType(String observedType) {
        this.observedType = observedType;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
