package com.rc.notification.interfaces.admin.dto;

import java.time.LocalDateTime;

/**
 * 死信记录响应 DTO
 */
public class DlqLogDto {

    private Long id;
    private String bizSign;
    private String traceId;
    private String supplierCode;
    private String errorMsg;
    private Integer retryCount;
    private Integer dlqStatus;
    private String updatedBy;
    private LocalDateTime createTime;
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
