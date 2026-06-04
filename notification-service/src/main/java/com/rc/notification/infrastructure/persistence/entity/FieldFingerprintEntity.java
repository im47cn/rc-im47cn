package com.rc.notification.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 字段指纹实体，与 field_fingerprint 表一一映射
 */
@TableName("field_fingerprint")
public class FieldFingerprintEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_type_code")
    private String eventTypeCode;

    @TableField("field_path")
    private String fieldPath;

    @TableField("observed_type")
    private String observedType;

    @TableField("first_seen_at")
    private LocalDateTime firstSeenAt;

    @TableField("last_seen_at")
    private LocalDateTime lastSeenAt;

    @TableField("sample_count")
    private Integer sampleCount;

    @TableField("status")
    private String status;

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
