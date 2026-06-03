package com.rc.notification.domain.config;

import com.rc.notification.infrastructure.persistence.entity.SupplierConfigEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商配置领域模型
 * <p>
 * 纯 POJO，不依赖任何持久化框架注解
 */
public class SupplierConfig {

    private Long id;
    private String supplierCode;
    private String supplierName;
    private String description;
    private String baseUrl;
    private String httpMethod;
    private String contentTypeBehavior;
    private String credentialsEncrypted;
    private String pathTemplate;
    private String queryTemplate;
    private String headerTemplate;
    private String bodyTemplate;
    private Integer connectTimeoutMs;
    private Integer readTimeoutMs;
    private String successHttpCodes;
    private String successBodyPattern;
    private String successBodyMatchMode;
    private Integer successCaseSensitive;
    private Integer maxRetryCount;
    private Integer retryBackoffInitialMs;
    private BigDecimal retryBackoffMultiplier;
    private Integer retryBackoffMaxMs;
    private Integer workerConcurrency;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从持久化实体转换为领域模型
     */
    public static SupplierConfig fromEntity(SupplierConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        SupplierConfig config = new SupplierConfig();
        config.setId(entity.getId());
        config.setSupplierCode(entity.getSupplierCode());
        config.setSupplierName(entity.getSupplierName());
        config.setDescription(entity.getDescription());
        config.setBaseUrl(entity.getBaseUrl());
        config.setHttpMethod(entity.getHttpMethod());
        config.setContentTypeBehavior(entity.getContentTypeBehavior());
        config.setCredentialsEncrypted(entity.getCredentialsEncrypted());
        config.setPathTemplate(entity.getPathTemplate());
        config.setQueryTemplate(entity.getQueryTemplate());
        config.setHeaderTemplate(entity.getHeaderTemplate());
        config.setBodyTemplate(entity.getBodyTemplate());
        config.setConnectTimeoutMs(entity.getConnectTimeoutMs());
        config.setReadTimeoutMs(entity.getReadTimeoutMs());
        config.setSuccessHttpCodes(entity.getSuccessHttpCodes());
        config.setSuccessBodyPattern(entity.getSuccessBodyPattern());
        config.setSuccessBodyMatchMode(entity.getSuccessBodyMatchMode());
        config.setSuccessCaseSensitive(entity.getSuccessCaseSensitive());
        config.setMaxRetryCount(entity.getMaxRetryCount());
        config.setRetryBackoffInitialMs(entity.getRetryBackoffInitialMs());
        config.setRetryBackoffMultiplier(entity.getRetryBackoffMultiplier());
        config.setRetryBackoffMaxMs(entity.getRetryBackoffMaxMs());
        config.setWorkerConcurrency(entity.getWorkerConcurrency());
        config.setStatus(entity.getStatus());
        config.setCreateTime(entity.getCreateTime());
        config.setUpdateTime(entity.getUpdateTime());
        return config;
    }

    /**
     * 转换为持久化实体
     */
    public SupplierConfigEntity toEntity() {
        SupplierConfigEntity entity = new SupplierConfigEntity();
        entity.setId(this.id);
        entity.setSupplierCode(this.supplierCode);
        entity.setSupplierName(this.supplierName);
        entity.setDescription(this.description);
        entity.setBaseUrl(this.baseUrl);
        entity.setHttpMethod(this.httpMethod);
        entity.setContentTypeBehavior(this.contentTypeBehavior);
        entity.setCredentialsEncrypted(this.credentialsEncrypted);
        entity.setPathTemplate(this.pathTemplate);
        entity.setQueryTemplate(this.queryTemplate);
        entity.setHeaderTemplate(this.headerTemplate);
        entity.setBodyTemplate(this.bodyTemplate);
        entity.setConnectTimeoutMs(this.connectTimeoutMs);
        entity.setReadTimeoutMs(this.readTimeoutMs);
        entity.setSuccessHttpCodes(this.successHttpCodes);
        entity.setSuccessBodyPattern(this.successBodyPattern);
        entity.setSuccessBodyMatchMode(this.successBodyMatchMode);
        entity.setSuccessCaseSensitive(this.successCaseSensitive);
        entity.setMaxRetryCount(this.maxRetryCount);
        entity.setRetryBackoffInitialMs(this.retryBackoffInitialMs);
        entity.setRetryBackoffMultiplier(this.retryBackoffMultiplier);
        entity.setRetryBackoffMaxMs(this.retryBackoffMaxMs);
        entity.setWorkerConcurrency(this.workerConcurrency);
        entity.setStatus(this.status);
        entity.setCreateTime(this.createTime);
        entity.setUpdateTime(this.updateTime);
        return entity;
    }

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
