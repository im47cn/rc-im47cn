package com.rc.notification.infrastructure.audit;

/**
 * 审计日志记录模型
 * <p>
 * 单行扁平化 JSON 格式，严禁包含内部换行符
 */
public class AuditLogRecord {

    private String timestamp;
    private String logLevel;
    private String traceId;
    private String bizSign;
    private String supplierCode;
    private String httpMethod;
    private String actualUrl;
    private int httpCode;
    private long elapsedTimeMs;
    private int retryCount;
    private String auditStatus;
    private String errorSummary;
    private long nextRetryDelayMs;

    // --- Builder Pattern ---

    public static AuditLogRecord builder() {
        return new AuditLogRecord();
    }

    public AuditLogRecord timestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public AuditLogRecord logLevel(String logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public AuditLogRecord traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public AuditLogRecord bizSign(String bizSign) {
        this.bizSign = bizSign;
        return this;
    }

    public AuditLogRecord supplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
        return this;
    }

    public AuditLogRecord httpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public AuditLogRecord actualUrl(String actualUrl) {
        this.actualUrl = actualUrl;
        return this;
    }

    public AuditLogRecord httpCode(int httpCode) {
        this.httpCode = httpCode;
        return this;
    }

    public AuditLogRecord elapsedTimeMs(long elapsedTimeMs) {
        this.elapsedTimeMs = elapsedTimeMs;
        return this;
    }

    public AuditLogRecord retryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public AuditLogRecord auditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
        return this;
    }

    public AuditLogRecord errorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
        return this;
    }

    public AuditLogRecord nextRetryDelayMs(long nextRetryDelayMs) {
        this.nextRetryDelayMs = nextRetryDelayMs;
        return this;
    }

    // --- Getters ---

    public String getTimestamp() { return timestamp; }
    public String getLogLevel() { return logLevel; }
    public String getTraceId() { return traceId; }
    public String getBizSign() { return bizSign; }
    public String getSupplierCode() { return supplierCode; }
    public String getHttpMethod() { return httpMethod; }
    public String getActualUrl() { return actualUrl; }
    public int getHttpCode() { return httpCode; }
    public long getElapsedTimeMs() { return elapsedTimeMs; }
    public int getRetryCount() { return retryCount; }
    public String getAuditStatus() { return auditStatus; }
    public String getErrorSummary() { return errorSummary; }
    public long getNextRetryDelayMs() { return nextRetryDelayMs; }
}
