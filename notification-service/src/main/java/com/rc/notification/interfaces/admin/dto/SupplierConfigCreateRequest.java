package com.rc.notification.interfaces.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 新增供应商配置请求
 */
public class SupplierConfigCreateRequest {

    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    @NotBlank(message = "供应商名称不能为空")
    private String supplierName;

    private String description;

    @NotBlank(message = "基础URL不能为空")
    private String baseUrl;

    private String httpMethod = "POST";
    private String contentTypeBehavior = "APPLICATION_JSON";

    /** 明文凭证 KV（提交时加密存储） */
    private Map<String, Object> credentials;

    private String pathTemplate;
    private String queryTemplate;
    private String headerTemplate;

    @NotBlank(message = "Body模板不能为空")
    private String bodyTemplate;

    private Integer connectTimeoutMs = 3000;
    private Integer readTimeoutMs = 5000;
    private String successHttpCodes = "200";
    private String successBodyPattern;
    private String successBodyMatchMode = "EQUALS";
    private Integer successCaseSensitive = 1;
    private Integer maxRetryCount = 3;
    private Integer retryBackoffInitialMs = 1000;
    private BigDecimal retryBackoffMultiplier = new BigDecimal("2.00");
    private Integer retryBackoffMaxMs = 30000;
    private Integer workerConcurrency = 1;

    @NotNull(message = "状态不能为空")
    private Integer status = 1;

    // --- Getters & Setters ---

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

    public Map<String, Object> getCredentials() { return credentials; }
    public void setCredentials(Map<String, Object> credentials) { this.credentials = credentials; }

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
}
