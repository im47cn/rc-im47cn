package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商配置响应 DTO
 * <p>
 * credentials_encrypted 字段脱敏返回，仅显示 key 列表不暴露 value
 */
@Schema(description = "供应商配置响应")
public class SupplierConfigDto {

    @Schema(description = "供应商ID")
    private Long id;
    @Schema(description = "供应商编码")
    private String supplierCode;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "基础URL")
    private String baseUrl;
    @Schema(description = "HTTP 方法")
    private String httpMethod;
    @Schema(description = "Content-Type 行为")
    private String contentTypeBehavior;
    /** 脱敏后的凭证 key 列表 */
    @Schema(description = "脱敏后的凭证 key 列表")
    private List<String> credentialKeys;
    @Schema(description = "路径模板 (JSONata)")
    private String pathTemplate;
    @Schema(description = "查询参数模板 (JSONata)")
    private String queryTemplate;
    @Schema(description = "请求头模板 (JSONata)")
    private String headerTemplate;
    @Schema(description = "请求体模板 (JSONata)")
    private String bodyTemplate;
    @Schema(description = "连接超时(毫秒)")
    private Integer connectTimeoutMs;
    @Schema(description = "读取超时(毫秒)")
    private Integer readTimeoutMs;
    @Schema(description = "成功 HTTP 状态码")
    private String successHttpCodes;
    @Schema(description = "成功响应体匹配模式")
    private String successBodyPattern;
    @Schema(description = "响应体匹配方式")
    private String successBodyMatchMode;
    @Schema(description = "匹配是否区分大小写: 0-否, 1-是")
    private Integer successCaseSensitive;
    @Schema(description = "最大重试次数")
    private Integer maxRetryCount;
    @Schema(description = "重试初始退避时间(毫秒)")
    private Integer retryBackoffInitialMs;
    @Schema(description = "重试退避乘数")
    private BigDecimal retryBackoffMultiplier;
    @Schema(description = "重试退避上限(毫秒)")
    private Integer retryBackoffMaxMs;
    @Schema(description = "Worker 并发数")
    private Integer workerConcurrency;
    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
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
