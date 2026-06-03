package com.rc.notification.interfaces.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 事件摄取请求 DTO
 */
@Schema(description = "事件摄取请求")
public class NotificationEventDto {

    /**
     * 业务唯一标识/幂等键
     */
    @Schema(description = "业务唯一标识/幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "evt-20240101-001")
    @NotBlank(message = "eventId 不能为空")
    private String eventId;

    /**
     * 供应商编码，用于路由到对应的 Redisson 队列
     */
    @Schema(description = "供应商编码，用于路由到对应的 Redisson 队列", requiredMode = Schema.RequiredMode.REQUIRED, example = "supplier-sms")
    @NotBlank(message = "supplierCode 不能为空")
    private String supplierCode;

    /**
     * 事件类型
     */
    @Schema(description = "事件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER_CREATED")
    @NotBlank(message = "eventType 不能为空")
    private String eventType;

    /**
     * 租户编码
     */
    @Schema(description = "租户编码")
    private String tenantCode;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private String userId;

    /**
     * 操作指令
     */
    @Schema(description = "操作指令")
    private String cmd;

    /**
     * 业务自定义载荷
     */
    @Schema(description = "业务自定义载荷", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "payload is required")
    private Map<String, Object> payload;

    /**
     * 全局链路追踪ID（可选，为空时由系统生成）
     */
    @Schema(description = "全局链路追踪ID，为空时由系统生成")
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
