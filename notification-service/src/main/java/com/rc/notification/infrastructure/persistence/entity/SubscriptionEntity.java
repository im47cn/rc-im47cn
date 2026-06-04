package com.rc.notification.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订阅关系实体，与 subscription 表一一映射
 */
@TableName("subscription")
public class SubscriptionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("subscriber_code")
    private String subscriberCode;

    @TableField("event_type_code")
    private String eventTypeCode;

    @TableField("status")
    private String status;

    @TableField("managed_by")
    private String managedBy;

    @TableField("path_template")
    private String pathTemplate;

    @TableField("query_template")
    private String queryTemplate;

    @TableField("header_template")
    private String headerTemplate;

    @TableField("body_template")
    private String bodyTemplate;

    @TableField("connect_timeout_ms")
    private Integer connectTimeoutMs;

    @TableField("read_timeout_ms")
    private Integer readTimeoutMs;

    @TableField("max_retry_count")
    private Integer maxRetryCount;

    @TableField("retry_backoff_initial_ms")
    private Integer retryBackoffInitialMs;

    @TableField("retry_backoff_multiplier")
    private BigDecimal retryBackoffMultiplier;

    @TableField("retry_backoff_max_ms")
    private Integer retryBackoffMaxMs;

    @TableField("success_http_codes")
    private String successHttpCodes;

    @TableField("success_body_pattern")
    private String successBodyPattern;

    @TableField("success_body_match_mode")
    private String successBodyMatchMode;

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

    public String getSubscriberCode() {
        return subscriberCode;
    }

    public void setSubscriberCode(String subscriberCode) {
        this.subscriberCode = subscriberCode;
    }

    public String getEventTypeCode() {
        return eventTypeCode;
    }

    public void setEventTypeCode(String eventTypeCode) {
        this.eventTypeCode = eventTypeCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getManagedBy() {
        return managedBy;
    }

    public void setManagedBy(String managedBy) {
        this.managedBy = managedBy;
    }

    public String getPathTemplate() {
        return pathTemplate;
    }

    public void setPathTemplate(String pathTemplate) {
        this.pathTemplate = pathTemplate;
    }

    public String getQueryTemplate() {
        return queryTemplate;
    }

    public void setQueryTemplate(String queryTemplate) {
        this.queryTemplate = queryTemplate;
    }

    public String getHeaderTemplate() {
        return headerTemplate;
    }

    public void setHeaderTemplate(String headerTemplate) {
        this.headerTemplate = headerTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(Integer connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(Integer readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public Integer getRetryBackoffInitialMs() {
        return retryBackoffInitialMs;
    }

    public void setRetryBackoffInitialMs(Integer retryBackoffInitialMs) {
        this.retryBackoffInitialMs = retryBackoffInitialMs;
    }

    public BigDecimal getRetryBackoffMultiplier() {
        return retryBackoffMultiplier;
    }

    public void setRetryBackoffMultiplier(BigDecimal retryBackoffMultiplier) {
        this.retryBackoffMultiplier = retryBackoffMultiplier;
    }

    public Integer getRetryBackoffMaxMs() {
        return retryBackoffMaxMs;
    }

    public void setRetryBackoffMaxMs(Integer retryBackoffMaxMs) {
        this.retryBackoffMaxMs = retryBackoffMaxMs;
    }

    public String getSuccessHttpCodes() {
        return successHttpCodes;
    }

    public void setSuccessHttpCodes(String successHttpCodes) {
        this.successHttpCodes = successHttpCodes;
    }

    public String getSuccessBodyPattern() {
        return successBodyPattern;
    }

    public void setSuccessBodyPattern(String successBodyPattern) {
        this.successBodyPattern = successBodyPattern;
    }

    public String getSuccessBodyMatchMode() {
        return successBodyMatchMode;
    }

    public void setSuccessBodyMatchMode(String successBodyMatchMode) {
        this.successBodyMatchMode = successBodyMatchMode;
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
