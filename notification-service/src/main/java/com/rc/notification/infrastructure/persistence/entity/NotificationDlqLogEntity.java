package com.rc.notification.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 死信保险库实体，与 notification_dlq_log 表一一映射
 */
@TableName("notification_dlq_log")
public class NotificationDlqLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("biz_sign")
    private String bizSign;

    @TableField("trace_id")
    private String traceId;

    @TableField("supplier_code")
    private String supplierCode;

    @TableField("unified_context")
    private String unifiedContext;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("dlq_status")
    private Integer dlqStatus;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizSign() {
        return bizSign;
    }

    public void setBizSign(String bizSign) {
        this.bizSign = bizSign;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getUnifiedContext() {
        return unifiedContext;
    }

    public void setUnifiedContext(String unifiedContext) {
        this.unifiedContext = unifiedContext;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getDlqStatus() {
        return dlqStatus;
    }

    public void setDlqStatus(Integer dlqStatus) {
        this.dlqStatus = dlqStatus;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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
