package com.rc.notification.interfaces.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 事件摄取响应 DTO
 */
@Schema(description = "事件摄取响应")
public class IngestResponse {

    @Schema(description = "事件ID")
    private String eventId;
    @Schema(description = "处理状态: ACCEPTED/IDEMPOTENT_HIT/DEAD_LETTERED/REJECTED", example = "ACCEPTED")
    private String status;
    @Schema(description = "响应消息")
    private String message;

    public IngestResponse() {
    }

    public IngestResponse(String eventId, String status, String message) {
        this.eventId = eventId;
        this.status = status;
        this.message = message;
    }

    public static IngestResponse accepted(String eventId) {
        return new IngestResponse(eventId, "ACCEPTED", "事件已接收并入队");
    }

    public static IngestResponse idempotentHit(String eventId, String currentStatus) {
        return new IngestResponse(eventId, "IDEMPOTENT_HIT", "事件已存在，当前状态: " + currentStatus);
    }

    public static IngestResponse deadLettered(String eventId) {
        return new IngestResponse(eventId, "DEAD_LETTERED", "该事件已进入死信队列，请通过管理后台处理");
    }

    public static IngestResponse rejected(String eventId, String reason) {
        return new IngestResponse(eventId, "REJECTED", reason);
    }

    // --- Getters & Setters ---

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
