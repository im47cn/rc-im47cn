package com.rc.notification.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商核心配置实体，与 supplier_config 表一一映射
 */
@TableName("supplier_config")
public class SupplierConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("supplier_code")
    private String supplierCode;

    @TableField("supplier_name")
    private String supplierName;

    @TableField("description")
    private String description;

    @TableField("base_url")
    private String baseUrl;

    @TableField("http_method")
    private String httpMethod;

    @TableField("content_type_behavior")
    private String contentTypeBehavior;

    @TableField("credentials_encrypted")
    private String credentialsEncrypted;

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

    @TableField("success_http_codes")
    private String successHttpCodes;

    @TableField("success_body_pattern")
    private String successBodyPattern;

    @TableField("success_body_match_mode")
    private String successBodyMatchMode;

    @TableField("success_case_sensitive")
    private Integer successCaseSensitive;

    @TableField("max_retry_count")
    private Integer maxRetryCount;

    @TableField("retry_backoff_initial_ms")
    private Integer retryBackoffInitialMs;

    @TableField("retry_backoff_multiplier")
    private BigDecimal retryBackoffMultiplier;

    @TableField("retry_backoff_max_ms")
    private Integer retryBackoffMaxMs;

    @TableField("worker_concurrency")
    private Integer workerConcurrency;

    @TableField("status")
    private Integer status;

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

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getContentTypeBehavior() {
        return contentTypeBehavior;
    }

    public void setContentTypeBehavior(String contentTypeBehavior) {
        this.contentTypeBehavior = contentTypeBehavior;
    }

    public String getCredentialsEncrypted() {
        return credentialsEncrypted;
    }

    public void setCredentialsEncrypted(String credentialsEncrypted) {
        this.credentialsEncrypted = credentialsEncrypted;
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

    public Integer getSuccessCaseSensitive() {
        return successCaseSensitive;
    }

    public void setSuccessCaseSensitive(Integer successCaseSensitive) {
        this.successCaseSensitive = successCaseSensitive;
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

    public Integer getWorkerConcurrency() {
        return workerConcurrency;
    }

    public void setWorkerConcurrency(Integer workerConcurrency) {
        this.workerConcurrency = workerConcurrency;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
