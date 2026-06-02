package com.rc.notification.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.application.worker.DeliveryWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 高性能异步流式审计组件
 * <p>
 * 通过 Logback AsyncAppender RingBuffer 输出单行 JSON 审计日志，
 * 对履约主线程产生 0 阻断。
 * <p>
 * 同时实现 DeliveryWorker.AuditLogger 接口，供 Worker 直接调用。
 * <p>
 * 敏感数据脱敏规则：
 * - auth 凭证字段完全剥离（由 Worker 层控制，不传入审计记录）
 * - URL 中查询参数匹配敏感模式（sign, key, token, secret）时值替换为 ***
 */
@Component
public class AuditLoggerAppender implements DeliveryWorker.AuditLogger {

    /**
     * 使用独立的 AUDIT logger，对应 logback-spring.xml 中的 ASYNC_AUDIT appender
     */
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    /**
     * URL 敏感参数名匹配模式
     */
    private static final Pattern SENSITIVE_PARAM_PATTERN =
            Pattern.compile("([?&])(sign|key|token|secret|password|credential)=([^&]*)", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public AuditLoggerAppender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 统一审计入口（记录模型方式）
     */
    public void logAudit(AuditLogRecord record) {
        try {
            // 脱敏 URL
            String sanitizedUrl = sanitizeUrl(record.getActualUrl());

            // 构建单行 JSON
            Map<String, Object> logMap = new LinkedHashMap<>();
            logMap.put("timestamp", record.getTimestamp() != null
                    ? record.getTimestamp() : ISO_FORMATTER.format(Instant.now()));
            logMap.put("log_level", record.getLogLevel());
            logMap.put("trace_id", record.getTraceId());
            logMap.put("biz_sign", record.getBizSign());
            logMap.put("supplier_code", record.getSupplierCode());
            logMap.put("http_method", record.getHttpMethod());
            logMap.put("actual_url", sanitizedUrl);
            logMap.put("http_code", record.getHttpCode());
            logMap.put("elapsed_time_ms", record.getElapsedTimeMs());
            logMap.put("retry_count", record.getRetryCount());
            logMap.put("audit_status", record.getAuditStatus());

            // 失败和死信时附加错误摘要
            if (record.getErrorSummary() != null && !record.getErrorSummary().isEmpty()) {
                logMap.put("error_summary", record.getErrorSummary());
            }

            // 失败重试时附加下次重试延迟
            if ("DELIVER_FAILED".equals(record.getAuditStatus()) && record.getNextRetryDelayMs() > 0) {
                logMap.put("next_retry_delay_ms", record.getNextRetryDelayMs());
            }

            String jsonLine = objectMapper.writeValueAsString(logMap);
            auditLog.info(jsonLine);

        } catch (JsonProcessingException e) {
            // 序列化失败不应阻断主流程
            auditLog.error("审计日志序列化失败: bizSign={}", record.getBizSign(), e);
        }
    }

    /**
     * DeliveryWorker.AuditLogger 接口实现
     */
    @Override
    public void logAudit(String bizSign, String traceId, String supplierCode,
                         String httpMethod, String actualUrl, int httpCode,
                         long elapsedTimeMs, int retryCount, String auditStatus,
                         String errorSummary, long nextRetryDelayMs) {

        String logLevel = switch (auditStatus) {
            case "DELIVER_SUCCESS" -> "INFO";
            case "DELIVER_FAILED" -> "WARN";
            case "DELIVER_DLQ" -> "ERROR";
            default -> "INFO";
        };

        AuditLogRecord record = AuditLogRecord.builder()
                .timestamp(ISO_FORMATTER.format(Instant.now()))
                .logLevel(logLevel)
                .traceId(traceId)
                .bizSign(bizSign)
                .supplierCode(supplierCode)
                .httpMethod(httpMethod)
                .actualUrl(actualUrl)
                .httpCode(httpCode)
                .elapsedTimeMs(elapsedTimeMs)
                .retryCount(retryCount)
                .auditStatus(auditStatus)
                .errorSummary(errorSummary)
                .nextRetryDelayMs(nextRetryDelayMs);

        logAudit(record);
    }

    /**
     * URL 敏感参数脱敏
     * <p>
     * 匹配 sign, key, token, secret 等参数名，将值替换为 ***
     */
    private String sanitizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        Matcher matcher = SENSITIVE_PARAM_PATTERN.matcher(url);
        return matcher.replaceAll("$1$2=***");
    }
}
