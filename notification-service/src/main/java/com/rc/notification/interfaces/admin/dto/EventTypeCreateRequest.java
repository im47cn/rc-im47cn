package com.rc.notification.interfaces.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 事件类型创建请求
 */
public class EventTypeCreateRequest {

    @NotBlank
    private String eventTypeCode;
    @NotBlank
    private String publisherCode;
    @NotBlank
    private String displayName;
    private String description;
    private String payloadSchema;

    // --- Getters & Setters ---

    public String getEventTypeCode() { return eventTypeCode; }
    public void setEventTypeCode(String eventTypeCode) { this.eventTypeCode = eventTypeCode; }

    public String getPublisherCode() { return publisherCode; }
    public void setPublisherCode(String publisherCode) { this.publisherCode = publisherCode; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPayloadSchema() { return payloadSchema; }
    public void setPayloadSchema(String payloadSchema) { this.payloadSchema = payloadSchema; }
}
