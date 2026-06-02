package com.rc.notification.interfaces.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商配置响应 DTO
 * <p>
 * credentials_encrypted 字段脱敏返回，仅显示 key 列表不暴露 value
 */
public class SupplierConfigDto {

    private Long id;
    private String supplierCode;
    private String supplierName;
    private String description;
    private String baseUrl;
    private String httpMethod;
    private String contentTypeBehavior;
    /** 脱敏后的凭证 key 列表 */
    private List<String> credentialKeys;
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

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getContentTypeBehavior() { return contentTypeBehavior; }
    public void setContentTypeBehavior(String contentTypeBehavior) { this.contentTypeBehavior = contentTypeBehavior; }

    public List<String> getCredentialKeys() { return credentialKeys; }
    public void setCredentialKeys(List<String> credentialKeys) { this.credentialKeys = credentialKeys; }

    public String getPathTemplate() { return pathTemplate; }
    public void setPathTemplate(String pathTemplate) { this.pathTemplate = pathTemplate; }

    public String getQueryTemplate() { return queryTemplate; }
    public void setQueryTemplate(String queryTemplate) { this.queryTemplate = queryTemplate; }

    public String getHeaderTemplate() { return headerTemplate; }
    public void setHeaderTemplate(String headerTemplate) { this.headerTemplate = headerTemplate; }

    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }

    public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(Integer connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public Integer getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(Integer readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public String getSuccessHttpCodes() { return successHttpCodes; }
    public void setSuccessHttpCodes(String successHttpCodes) { this.successHttpCodes = successHttpCodes; }

    public String getSuccessBodyPattern() { return successBodyPattern; }
    public void setSuccessBodyPattern(String successBodyPattern) { this.successBodyPattern = successBodyPattern; }

    public String getSuccessBodyMatchMode() { return successBodyMatchMode; }
    public void setSuccessBodyMatchMode(String successBodyMatchMode) { this.successBodyMatchMode = successBodyMatchMode; }

    public Integer getSuccessCaseSensitive() { return successCaseSensitive; }
    public void setSuccessCaseSensitive(Integer successCaseSensitive) { this.successCaseSensitive = successCaseSensitive; }

    public Integer getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(Integer maxRetryCount) { this.maxRetryCount = maxRetryCount; }

    public Integer getRetryBackoffInitialMs() { return retryBackoffInitialMs; }
    public void setRetryBackoffInitialMs(Integer retryBackoffInitialMs) { this.retryBackoffInitialMs = retryBackoffInitialMs; }

    public BigDecimal getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public void setRetryBackoffMultiplier(BigDecimal retryBackoffMultiplier) { this.retryBackoffMultiplier = retryBackoffMultiplier; }

    public Integer getRetryBackoffMaxMs() { return retryBackoffMaxMs; }
    public void setRetryBackoffMaxMs(Integer retryBackoffMaxMs) { this.retryBackoffMaxMs = retryBackoffMaxMs; }

    public Integer getWorkerConcurrency() { return workerConcurrency; }
    public void setWorkerConcurrency(Integer workerConcurrency) { this.workerConcurrency = workerConcurrency; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
