package com.rc.notification.domain.event;

import com.rc.notification.infrastructure.persistence.entity.EventTypeEntity;

import java.time.LocalDateTime;

/**
 * 事件类型领域模型
 * <p>
 * 纯 POJO，不依赖任何持久化框架注解
 */
public class EventType {

    private Long id;
    private String eventTypeCode;
    private String publisherCode;
    private String displayName;
    private String description;
    private String payloadSchema;
    private String status;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从持久化实体转换为领域模型
     */
    public static EventType fromEntity(EventTypeEntity entity) {
        if (entity == null) {
            return null;
        }
        EventType eventType = new EventType();
        eventType.setId(entity.getId());
        eventType.setEventTypeCode(entity.getEventTypeCode());
        eventType.setPublisherCode(entity.getPublisherCode());
        eventType.setDisplayName(entity.getDisplayName());
        eventType.setDescription(entity.getDescription());
        eventType.setPayloadSchema(entity.getPayloadSchema());
        eventType.setStatus(entity.getStatus());
        eventType.setVersion(entity.getVersion());
        eventType.setCreateTime(entity.getCreateTime());
        eventType.setUpdateTime(entity.getUpdateTime());
        return eventType;
    }

    /**
     * 转换为持久化实体
     */
    public EventTypeEntity toEntity() {
        EventTypeEntity entity = new EventTypeEntity();
        entity.setId(this.id);
        entity.setEventTypeCode(this.eventTypeCode);
        entity.setPublisherCode(this.publisherCode);
        entity.setDisplayName(this.displayName);
        entity.setDescription(this.description);
        entity.setPayloadSchema(this.payloadSchema);
        entity.setStatus(this.status);
        entity.setVersion(this.version);
        entity.setCreateTime(this.createTime);
        entity.setUpdateTime(this.updateTime);
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

    public String getPublisherCode() {
        return publisherCode;
    }

    public void setPublisherCode(String publisherCode) {
        this.publisherCode = publisherCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPayloadSchema() {
        return payloadSchema;
    }

    public void setPayloadSchema(String payloadSchema) {
        this.payloadSchema = payloadSchema;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
