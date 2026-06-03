package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 死信记录响应 DTO
 */
@Schema(description = "死信记录响应")
public class DlqLogDto {

    @Schema(description = "记录ID")
    private Long id;
    @Schema(description = "业务标识")
    private String bizSign;
    @Schema(description = "链路追踪ID")
    private String traceId;
    @Schema(description = "供应商编码")
    private String supplierCode;
    @Schema(description = "错误消息")
    private String errorMsg;
    @Schema(description = "已重试次数")
    private Integer retryCount;
    @Schema(description = "死信状态: 0-待处理, 1-已重试, 2-已忽略")
    private Integer dlqStatus;
    @Schema(description = "最后操作人")
    private String updatedBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBizSign() { return bizSign; }
    public void setBizSign(String bizSign) { this.bizSign = bizSign; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getDlqStatus() { return dlqStatus; }
    public void setDlqStatus(Integer dlqStatus) { this.dlqStatus = dlqStatus; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
