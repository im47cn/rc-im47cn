package com.rc.notification.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class IngestV2Request {

    @NotBlank(message = "eventId 不能为空")
    private String eventId;

    @NotBlank(message = "eventType 不能为空")
    private String eventType;

    @NotNull(message = "payload 不能为空")
    private Map<String, Object> payload;

    private String subscriberCode;  // optional: targeted delivery

    private String traceId;         // optional

    // --- Getters & Setters ---

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getSubscriberCode() { return subscriberCode; }
    public void setSubscriberCode(String subscriberCode) { this.subscriberCode = subscriberCode; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
