package com.rc.notification.domain.subscription;

import com.rc.notification.domain.config.SupplierConfig;

import java.math.BigDecimal;

/**
 * 合并后的有效配置值对象（不可变）
 * <p>
 * 通过 Builder 构造，所有字段只读。
 */
public final class EffectiveConfig {

    // Channel-level: always from SubscriberConfig
    private final String subscriberCode;
    private final String baseUrl;
    private final String httpMethod;
    private final String contentTypeBehavior;
    private final String credentialsEncrypted;
    private final Integer workerConcurrency;

    // Overridable: Subscription > SubscriberConfig
    private final String pathTemplate;
    private final String queryTemplate;
    private final String headerTemplate;
    private final String bodyTemplate;
    private final Integer connectTimeoutMs;
    private final Integer readTimeoutMs;
    private final String successHttpCodes;
    private final String successBodyPattern;
    private final String successBodyMatchMode;
    private final Integer successCaseSensitive;
    private final Integer maxRetryCount;
    private final Integer retryBackoffInitialMs;
    private final Integer retryBackoffMaxMs;
    private final BigDecimal retryBackoffMultiplier;

    private EffectiveConfig(Builder builder) {
        this.subscriberCode = builder.subscriberCode;
        this.baseUrl = builder.baseUrl;
        this.httpMethod = builder.httpMethod;
        this.contentTypeBehavior = builder.contentTypeBehavior;
        this.credentialsEncrypted = builder.credentialsEncrypted;
        this.workerConcurrency = builder.workerConcurrency;
        this.pathTemplate = builder.pathTemplate;
        this.queryTemplate = builder.queryTemplate;
        this.headerTemplate = builder.headerTemplate;
        this.bodyTemplate = builder.bodyTemplate;
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;
        this.successHttpCodes = builder.successHttpCodes;
        this.successBodyPattern = builder.successBodyPattern;
        this.successBodyMatchMode = builder.successBodyMatchMode;
        this.successCaseSensitive = builder.successCaseSensitive;
        this.maxRetryCount = builder.maxRetryCount;
        this.retryBackoffInitialMs = builder.retryBackoffInitialMs;
        this.retryBackoffMaxMs = builder.retryBackoffMaxMs;
        this.retryBackoffMultiplier = builder.retryBackoffMultiplier;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建兼容旧有 Worker/RequestBuilder 代码的 SupplierConfig 对象
     */
    public SupplierConfig toSupplierConfigCompat() {
        SupplierConfig c = new SupplierConfig();
        c.setSupplierCode(this.subscriberCode);
        c.setBaseUrl(this.baseUrl);
        c.setHttpMethod(this.httpMethod);
        c.setContentTypeBehavior(this.contentTypeBehavior);
        c.setCredentialsEncrypted(this.credentialsEncrypted);
        c.setPathTemplate(this.pathTemplate);
        c.setQueryTemplate(this.queryTemplate);
        c.setHeaderTemplate(this.headerTemplate);
        c.setBodyTemplate(this.bodyTemplate);
        c.setConnectTimeoutMs(this.connectTimeoutMs);
        c.setReadTimeoutMs(this.readTimeoutMs);
        c.setSuccessHttpCodes(this.successHttpCodes);
        c.setSuccessBodyPattern(this.successBodyPattern);
        c.setSuccessBodyMatchMode(this.successBodyMatchMode);
        c.setSuccessCaseSensitive(this.successCaseSensitive);
        c.setMaxRetryCount(this.maxRetryCount);
        c.setRetryBackoffInitialMs(this.retryBackoffInitialMs);
        c.setRetryBackoffMultiplier(this.retryBackoffMultiplier);
        c.setRetryBackoffMaxMs(this.retryBackoffMaxMs);
        c.setWorkerConcurrency(this.workerConcurrency);
        c.setStatus(1);
        return c;
    }

    // --- Getters ---

    public String getSubscriberCode() { return subscriberCode; }
    public String getBaseUrl() { return baseUrl; }
    public String getHttpMethod() { return httpMethod; }
    public String getContentTypeBehavior() { return contentTypeBehavior; }
    public String getCredentialsEncrypted() { return credentialsEncrypted; }
    public Integer getWorkerConcurrency() { return workerConcurrency; }
    public String getPathTemplate() { return pathTemplate; }
    public String getQueryTemplate() { return queryTemplate; }
    public String getHeaderTemplate() { return headerTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
    public Integer getReadTimeoutMs() { return readTimeoutMs; }
    public String getSuccessHttpCodes() { return successHttpCodes; }
    public String getSuccessBodyPattern() { return successBodyPattern; }
    public String getSuccessBodyMatchMode() { return successBodyMatchMode; }
    public Integer getSuccessCaseSensitive() { return successCaseSensitive; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public Integer getRetryBackoffInitialMs() { return retryBackoffInitialMs; }
    public Integer getRetryBackoffMaxMs() { return retryBackoffMaxMs; }
    public BigDecimal getRetryBackoffMultiplier() { return retryBackoffMultiplier; }

    // --- Builder ---

    public static final class Builder {
        private String subscriberCode;
        private String baseUrl;
        private String httpMethod;
        private String contentTypeBehavior;
        private String credentialsEncrypted;
        private Integer workerConcurrency;
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
        private Integer retryBackoffMaxMs;
        private BigDecimal retryBackoffMultiplier;

        private Builder() {}

        public Builder subscriberCode(String v) { this.subscriberCode = v; return this; }
        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        public Builder httpMethod(String v) { this.httpMethod = v; return this; }
        public Builder contentTypeBehavior(String v) { this.contentTypeBehavior = v; return this; }
        public Builder credentialsEncrypted(String v) { this.credentialsEncrypted = v; return this; }
        public Builder workerConcurrency(Integer v) { this.workerConcurrency = v; return this; }
        public Builder pathTemplate(String v) { this.pathTemplate = v; return this; }
        public Builder queryTemplate(String v) { this.queryTemplate = v; return this; }
        public Builder headerTemplate(String v) { this.headerTemplate = v; return this; }
        public Builder bodyTemplate(String v) { this.bodyTemplate = v; return this; }
        public Builder connectTimeoutMs(Integer v) { this.connectTimeoutMs = v; return this; }
        public Builder readTimeoutMs(Integer v) { this.readTimeoutMs = v; return this; }
        public Builder successHttpCodes(String v) { this.successHttpCodes = v; return this; }
        public Builder successBodyPattern(String v) { this.successBodyPattern = v; return this; }
        public Builder successBodyMatchMode(String v) { this.successBodyMatchMode = v; return this; }
        public Builder successCaseSensitive(Integer v) { this.successCaseSensitive = v; return this; }
        public Builder maxRetryCount(Integer v) { this.maxRetryCount = v; return this; }
        public Builder retryBackoffInitialMs(Integer v) { this.retryBackoffInitialMs = v; return this; }
        public Builder retryBackoffMaxMs(Integer v) { this.retryBackoffMaxMs = v; return this; }
        public Builder retryBackoffMultiplier(BigDecimal v) { this.retryBackoffMultiplier = v; return this; }

        public EffectiveConfig build() {
            return new EffectiveConfig(this);
        }
    }
}
