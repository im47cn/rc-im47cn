package com.rc.notification.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 事件摄取请求 DTO
 */
public class NotificationEventDto {

    /**
     * 业务唯一标识/幂等键
     */
    @NotBlank(message = "eventId 不能为空")
    private String eventId;

    /**
     * 供应商编码，用于路由到对应的 Redisson 队列
     */
    @NotBlank(message = "supplierCode 不能为空")
    private String supplierCode;

    /**
     * 事件类型
     */
    @NotBlank(message = "eventType 不能为空")
    private String eventType;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 操作指令
     */
    private String cmd;

    /**
     * 业务自定义载荷
     */
    @NotNull(message = "payload is required")
    private Map<String, Object> payload;

    /**
     * 全局链路追踪ID（可选，为空时由系统生成）
     */
    private String traceId;

    // --- Getters & Setters ---

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
