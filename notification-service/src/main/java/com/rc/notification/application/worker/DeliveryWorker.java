package com.rc.notification.application.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.application.service.IngestionService;
import com.rc.notification.domain.config.SupplierConfigDomainService;
import com.rc.notification.domain.translation.TranslationEngineException;
import com.rc.notification.infrastructure.persistence.entity.SupplierConfigEntity;
import com.rc.notification.infrastructure.http.FullStackHttpRequestBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 投递执行 Worker
 * <p>
 * 核心投递循环：阻塞拉取 -> 构建 UnifiedInputContext -> JSONata 转换 ->
 * HTTP 执行 -> 成功判定 -> 失败时指数退避重新入队
 */
public class DeliveryWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);

    private static final String STATUS_PREFIX = "status:dispatch:";

    private final String supplierCode;
    private final String queueName;
    private final SupplierWorkerManager workerManager;
    private final RedissonClient redissonClient;
    private final SupplierConfigDomainService configDomainService;
    private final FullStackHttpRequestBuilder requestBuilder;
    private final UnifiedInputContextBuilder contextBuilder;
    private final ObjectMapper objectMapper;

    /** 审计日志接口（T10 实现后注入） */
    private AuditLogger auditLogger;

    /** 死信服务接口（T11 实现后注入） */
    private DeadLetterHandler deadLetterHandler;

    public DeliveryWorker(String supplierCode,
                          String queueName,
                          SupplierWorkerManager workerManager,
                          RedissonClient redissonClient,
                          SupplierConfigDomainService configDomainService,
                          FullStackHttpRequestBuilder requestBuilder,
                          UnifiedInputContextBuilder contextBuilder,
                          ObjectMapper objectMapper) {
        this.supplierCode = supplierCode;
        this.queueName = queueName;
        this.workerManager = workerManager;
        this.redissonClient = redissonClient;
        this.configDomainService = configDomainService;
        this.requestBuilder = requestBuilder;
        this.contextBuilder = contextBuilder;
        this.objectMapper = objectMapper;
    }

    public void setAuditLogger(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void setDeadLetterHandler(DeadLetterHandler deadLetterHandler) {
        this.deadLetterHandler = deadLetterHandler;
    }

    @Override
    public void run() {
        log.info("DeliveryWorker 启动: supplierCode={}, queue={}", supplierCode, queueName);
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(queueName);
        // 创建延迟队列用于重试
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);

        try {
            while (!workerManager.isShutdownRequested() && !Thread.currentThread().isInterrupted()) {
                String eventJson = null;
                try {
                    // 阻塞拉取，超时3秒后重新检查停机标志
                    eventJson = blockingQueue.poll(3, TimeUnit.SECONDS);
                    if (eventJson == null) {
                        continue;
                    }

                    processEvent(eventJson, blockingQueue, delayedQueue);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // 被中断的 Worker 将当前任务重新压回队列
                    if (eventJson != null) {
                        try {
                            blockingQueue.add(eventJson);
                            log.info("Worker 被中断，当前任务重新入队: supplierCode={}", supplierCode);
                        } catch (Exception requeueEx) {
                            log.error("中断后重入队失败: supplierCode={}", supplierCode, requeueEx);
                        }
                    }
                    break;
                } catch (Exception e) {
                    log.error("Worker 循环异常: supplierCode={}", supplierCode, e);
                }
            }
        } finally {
            log.info("DeliveryWorker 退出: supplierCode={}", supplierCode);
        }
    }

    /**
     * 处理单个事件
     */
    private void processEvent(String eventJson, RBlockingQueue<String> blockingQueue,
                              RDelayedQueue<String> delayedQueue) {
        long startTime = System.currentTimeMillis();
        String bizSign = null;
        String traceId = null;
        int retryCount = 0;
        Map<String, Object> unifiedContext = null;

        try {
            // 解析事件元数据
            Map<String, Object> eventMeta = objectMapper.readValue(eventJson, new TypeReference<>() {});
            bizSign = (String) eventMeta.get("eventId");
            traceId = (String) eventMeta.get("traceId");
            retryCount = eventMeta.containsKey("retryCount") ? ((Number) eventMeta.get("retryCount")).intValue() : 0;

            // 检查幂等状态（SUCCESS 状态直接丢弃）
            var statusBucket = redissonClient.getBucket(STATUS_PREFIX + bizSign);
            Object currentStatus = statusBucket.get();
            if (currentStatus != null && IngestionService.STATUS_SUCCESS.equals(currentStatus.toString())) {
                log.info("事件已成功投递，跳过长尾幽灵重试: bizSign={}", bizSign);
                return;
            }

            // 获取供应商配置
            SupplierConfigEntity config = configDomainService.getBySupplierCode(supplierCode);
            if (config == null || config.getStatus() == null || config.getStatus() != 1) {
                log.warn("供应商配置不存在或已禁用，跳过: supplierCode={}", supplierCode);
                return;
            }

            // 构建 UnifiedInputContext
            unifiedContext = contextBuilder.build(eventJson, config.getCredentialsEncrypted());

            // JSONata 转换 + HTTP 请求构建
            Request request;
            OkHttpClient client;
            try {
                request = requestBuilder.buildRequest(config, unifiedContext);
                client = requestBuilder.deriveClient(config);
            } catch (TranslationEngineException e) {
                // JSONata 错误为不可恢复的逻辑死信，直接进 DLQ
                log.error("JSONata 转换失败，直接进入死信: bizSign={}, error={}", bizSign, e.getMessage());
                handleDeadLetter(bizSign, traceId, unifiedContext, retryCount,
                        "JSONata 转换失败: " + e.getMessage());
                logAudit(bizSign, traceId, config, null, 0,
                        System.currentTimeMillis() - startTime, retryCount,
                        "DELIVER_DLQ", "JSONata error: " + e.getMessage(), 0);
                return;
            }

            // 执行 HTTP 请求
            try (Response response = client.newCall(request).execute()) {
                int httpCode = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                long elapsed = System.currentTimeMillis() - startTime;

                // 成功判定
                if (isSuccess(config, httpCode, responseBody)) {
                    // 成功：更新 Redis 状态为 SUCCESS，原子级缩短 TTL 至 1h
                    statusBucket.set(IngestionService.STATUS_SUCCESS, Duration.ofHours(1));
                    log.info("投递成功: bizSign={}, httpCode={}, elapsed={}ms",
                            bizSign, httpCode, elapsed);
                    logAudit(bizSign, traceId, config, request.url().toString(), httpCode,
                            elapsed, retryCount, "DELIVER_SUCCESS", null, 0);
                } else {
                    // 失败：检查是否达到重试上限
                    handleFailure(eventJson, eventMeta, config, blockingQueue, delayedQueue,
                            bizSign, traceId, unifiedContext, retryCount,
                            httpCode, responseBody, elapsed, request.url().toString());
                }
            }

        } catch (TranslationEngineException e) {
            // JSONata 异常兜底
            log.error("JSONata 转换异常: bizSign={}", bizSign, e);
            handleDeadLetter(bizSign, traceId, unifiedContext, retryCount,
                    "JSONata 转换失败: " + e.getMessage());
        } catch (Exception e) {
            // 网络异常等其他错误
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("投递异常: bizSign={}, error={}", bizSign, e.getMessage());

            if (bizSign != null) {
                try {
                    Map<String, Object> eventMeta = objectMapper.readValue(eventJson, new TypeReference<>() {});
                    SupplierConfigEntity config = configDomainService.getBySupplierCode(supplierCode);
                    if (config != null) {
                        handleFailure(eventJson, eventMeta, config, blockingQueue, delayedQueue,
                                bizSign, traceId, unifiedContext, retryCount,
                                0, e.getMessage(), elapsed, "");
                    }
                } catch (Exception innerEx) {
                    log.error("异常处理中再次异常: bizSign={}", bizSign, innerEx);
                }
            }
        }
    }

    /**
     * 处理投递失败：指数退避重试或进入死信
     */
    private void handleFailure(String eventJson, Map<String, Object> eventMeta,
                               SupplierConfigEntity config,
                               RBlockingQueue<String> blockingQueue,
                               RDelayedQueue<String> delayedQueue,
                               String bizSign, String traceId,
                               Map<String, Object> unifiedContext, int retryCount,
                               int httpCode, String errorMsg, long elapsed, String actualUrl) {

        int maxRetry = config.getMaxRetryCount() != null ? config.getMaxRetryCount() : 3;

        if (retryCount >= maxRetry) {
            // 达到重试上限，进入死信
            log.error("重试耗尽，进入死信: bizSign={}, retryCount={}", bizSign, retryCount);
            String errorSummary = String.format("Max retry exhausted: %d %s",
                    httpCode, truncate(errorMsg, 200));
            handleDeadLetter(bizSign, traceId, unifiedContext, retryCount, errorSummary);
            logAudit(bizSign, traceId, config, actualUrl, httpCode,
                    elapsed, retryCount, "DELIVER_DLQ", errorSummary, 0);
        } else {
            // 计算指数退避延迟
            long delay = calculateBackoffDelay(config, retryCount);

            // 更新重试计数并重新入队
            try {
                eventMeta.put("retryCount", retryCount + 1);
                String updatedJson = objectMapper.writeValueAsString(eventMeta);
                delayedQueue.offer(updatedJson, delay, TimeUnit.MILLISECONDS);

                String errorSummary = String.format("%d %s", httpCode, truncate(errorMsg, 200));
                log.warn("投递失败，{}ms 后重试: bizSign={}, retryCount={}, error={}",
                        delay, bizSign, retryCount, errorSummary);
                logAudit(bizSign, traceId, config, actualUrl, httpCode,
                        elapsed, retryCount, "DELIVER_FAILED", errorSummary, delay);
            } catch (Exception e) {
                log.error("重试入队失败: bizSign={}", bizSign, e);
            }
        }
    }

    /**
     * 计算指数退避延迟
     * T_delay = min(initial_ms * multiplier ^ retry_count, max_ms)
     */
    private long calculateBackoffDelay(SupplierConfigEntity config, int retryCount) {
        int initialMs = config.getRetryBackoffInitialMs() != null ? config.getRetryBackoffInitialMs() : 1000;
        BigDecimal multiplier = config.getRetryBackoffMultiplier() != null
                ? config.getRetryBackoffMultiplier() : BigDecimal.valueOf(2.0);
        int maxMs = config.getRetryBackoffMaxMs() != null ? config.getRetryBackoffMaxMs() : 30000;

        double delay = initialMs * Math.pow(multiplier.doubleValue(), retryCount);
        return Math.min((long) delay, maxMs);
    }

    /**
     * 成功判定逻辑
     */
    private boolean isSuccess(SupplierConfigEntity config, int httpCode, String responseBody) {
        // 1. 检查 HTTP 状态码
        String successCodes = config.getSuccessHttpCodes() != null ? config.getSuccessHttpCodes() : "200";
        Set<Integer> codeSet = Arrays.stream(successCodes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());

        if (!codeSet.contains(httpCode)) {
            return false;
        }

        // 2. 检查响应体匹配（可选）
        String bodyPattern = config.getSuccessBodyPattern();
        if (bodyPattern == null || bodyPattern.isBlank()) {
            return true;
        }

        String matchMode = config.getSuccessBodyMatchMode() != null
                ? config.getSuccessBodyMatchMode() : "EQUALS";
        boolean caseSensitive = config.getSuccessCaseSensitive() != null
                && config.getSuccessCaseSensitive() == 1;

        String body = caseSensitive ? responseBody : responseBody.toLowerCase();
        String pattern = caseSensitive ? bodyPattern : bodyPattern.toLowerCase();

        return switch (matchMode.toUpperCase()) {
            case "CONTAINS" -> body.contains(pattern);
            case "EQUALS" -> body.equals(pattern);
            default -> body.equals(pattern);
        };
    }

    /**
     * 处理死信
     */
    private void handleDeadLetter(String bizSign, String traceId,
                                  Map<String, Object> unifiedContext, int retryCount,
                                  String errorMsg) {
        if (deadLetterHandler != null) {
            String contextJson = unifiedContext != null
                    ? contextBuilder.serialize(unifiedContext) : "{}";
            deadLetterHandler.handleDeadLetter(bizSign, traceId, supplierCode,
                    contextJson, errorMsg, retryCount);
        } else {
            log.error("[DLQ占位] 死信服务尚未实现: bizSign={}, error={}", bizSign, errorMsg);
        }
    }

    /**
     * 记录审计日志
     */
    private void logAudit(String bizSign, String traceId, SupplierConfigEntity config,
                          String actualUrl, int httpCode, long elapsed, int retryCount,
                          String auditStatus, String errorSummary, long nextRetryDelayMs) {
        if (auditLogger != null) {
            auditLogger.logAudit(bizSign, traceId, supplierCode,
                    config.getHttpMethod(), actualUrl, httpCode,
                    elapsed, retryCount, auditStatus, errorSummary, nextRetryDelayMs);
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    // ==================== 跨任务接口定义 ====================

    /**
     * 审计日志接口（T10 实现后提供）
     */
    public interface AuditLogger {
        void logAudit(String bizSign, String traceId, String supplierCode,
                      String httpMethod, String actualUrl, int httpCode,
                      long elapsedTimeMs, int retryCount, String auditStatus,
                      String errorSummary, long nextRetryDelayMs);
    }

    /**
     * 死信处理接口（T11 实现后提供）
     */
    public interface DeadLetterHandler {
        void handleDeadLetter(String bizSign, String traceId, String supplierCode,
                              String unifiedContextJson, String errorMsg, int retryCount);
    }
}
