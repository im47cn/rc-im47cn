package com.rc.notification.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 变更记录实体，与 change_record 表一一映射
 */
@TableName("change_record")
public class ChangeRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_type_code")
    private String eventTypeCode;

    @TableField("change_type")
    private String changeType;

    @TableField("field_path")
    private String fieldPath;

    @TableField("old_value")
    private String oldValue;

    @TableField("new_value")
    private String newValue;

    @TableField("detection_source")
    private String detectionSource;

    @TableField("confidence")
    private String confidence;

    @TableField("status")
    private String status;

    @TableField("affected_subscriptions")
    private String affectedSubscriptions;

    @TableField("created_at")
    private LocalDateTime createdAt;

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
