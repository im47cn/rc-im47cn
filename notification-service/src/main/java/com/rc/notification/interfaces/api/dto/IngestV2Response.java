package com.rc.notification.interfaces.api.dto;

import java.util.List;

public class IngestV2Response {

    private String eventId;
    private String status;   // ACCEPTED / REJECTED
    private String message;
    private List<DispatchDetail> dispatches;

    public IngestV2Response() {
    }

    public IngestV2Response(String eventId, String status, String message, List<DispatchDetail> dispatches) {
        this.eventId = eventId;
        this.status = status;
        this.message = message;
        this.dispatches = dispatches;
    }

    public static IngestV2Response accepted(String eventId, List<DispatchDetail> dispatches) {
        return new IngestV2Response(eventId, "ACCEPTED", "事件已接收", dispatches);
    }

    public static IngestV2Response rejected(String eventId, String reason) {
        return new IngestV2Response(eventId, "REJECTED", reason, List.of());
    }

    // --- Getters & Setters ---

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<DispatchDetail> getDispatches() { return dispatches; }
    public void setDispatches(List<DispatchDetail> dispatches) { this.dispatches = dispatches; }
}
