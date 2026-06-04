package com.rc.notification.interfaces.admin.dto;

/**
 * 事件类型更新请求
 */
public class EventTypeUpdateRequest {

    private String displayName;
    private String description;
    private String payloadSchema;
    private String status;

    // --- Getters & Setters ---

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPayloadSchema() { return payloadSchema; }
    public void setPayloadSchema(String payloadSchema) { this.payloadSchema = payloadSchema; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
