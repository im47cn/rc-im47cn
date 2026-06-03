package com.rc.notification.unit.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.application.service.IngestionService;
import com.rc.notification.application.worker.DeliveryWorker;
import com.rc.notification.application.worker.SupplierWorkerManager;
import com.rc.notification.application.worker.UnifiedInputContextBuilder;
import com.rc.notification.domain.config.SupplierConfig;
import com.rc.notification.domain.config.SupplierConfigDomainService;
import com.rc.notification.domain.translation.TranslationEngineException;
import com.rc.notification.infrastructure.http.FullStackHttpRequestBuilder;
import com.rc.notification.infrastructure.metrics.NotificationMetricsRegistry;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBucket;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DeliveryWorker 单元测试
 * <p>
 * 验证投递核心流程：成功/失败重试/DLQ/JSONata 死信
 * <p>
 * 由于 processEvent 是 private 方法，通过反射调用以进行隔离测试
 */
@ExtendWith(MockitoExtension.class)
class DeliveryWorkerTest {

    @Mock private SupplierWorkerManager workerManager;
    @Mock private RedissonClient redissonClient;
    @Mock private SupplierConfigDomainService configDomainService;
    @Mock private FullStackHttpRequestBuilder requestBuilder;
    @Mock private UnifiedInputContextBuilder contextBuilder;
    @Mock private NotificationMetricsRegistry metricsRegistry;
    @Mock private RBlockingQueue<String> blockingQueue;
    @Mock private RDelayedQueue<String> delayedQueue;
    @Mock private RBucket<Object> statusBucket;
    @Mock private DeliveryWorker.DeadLetterHandler deadLetterHandler;

    private DeliveryWorker worker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        worker = new DeliveryWorker(
                "TEST_SUPPLIER", "queue:notification:TEST_SUPPLIER",
                workerManager, redissonClient, configDomainService,
                requestBuilder, contextBuilder, objectMapper, metricsRegistry);
        worker.setDeadLetterHandler(deadLetterHandler);

        // 默认 mock: status bucket (lenient + doReturn 避免泛型推断问题)
        lenient().doReturn(statusBucket).when(redissonClient).getBucket(anyString());
        lenient().doReturn(blockingQueue).when(redissonClient).getBlockingQueue(anyString());
        lenient().doReturn(delayedQueue).when(redissonClient).getDelayedQueue(any());
    }

    private String buildEventJson(String eventId, int retryCount) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "eventId", eventId,
                "traceId", "T-123",
                "supplierCode", "TEST_SUPPLIER",
                "retryCount", retryCount
        ));
    }

    private SupplierConfig buildConfig() {
        SupplierConfig config = new SupplierConfig();
        config.setSupplierCode("TEST_SUPPLIER");
        config.setStatus(1);
        config.setHttpMethod("POST");
        config.setSuccessHttpCodes("200");
        config.setMaxRetryCount(3);
        config.setRetryBackoffInitialMs(1000);
        config.setRetryBackoffMultiplier(new BigDecimal("2.0"));
        config.setRetryBackoffMaxMs(30000);
        return config;
    }

    /**
     * 通过反射调用 processEvent
     */
    private void invokeProcessEvent(String eventJson) throws Exception {
        Method method = DeliveryWorker.class.getDeclaredMethod(
                "processEvent", String.class, RBlockingQueue.class, RDelayedQueue.class);
        method.setAccessible(true);
        method.invoke(worker, eventJson, blockingQueue, delayedQueue);
    }

    @Test
    @DisplayName("成功投递: 状态更新为 SUCCESS")
    void processEvent_success_updatesStatusToSuccess() throws Exception {
        String eventJson = buildEventJson("EVT-001", 0);
        SupplierConfig config = buildConfig();

        when(statusBucket.get()).thenReturn(null);
        when(configDomainService.getBySupplierCode("TEST_SUPPLIER")).thenReturn(config);
        when(contextBuilder.build(anyString(), any())).thenReturn(Map.of("event", Map.of()));

        Request okRequest = new Request.Builder().url("https://api.test.com/notify").build();
        OkHttpClient okClient = mock(OkHttpClient.class);
        when(requestBuilder.buildRequest(eq(config), any())).thenReturn(okRequest);
        when(requestBuilder.deriveClient(config)).thenReturn(okClient);

        // mock HTTP 200 成功响应
        Response successResponse = new Response.Builder()
                .request(okRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("", MediaType.parse("text/plain")))
                .build();

        Call call = mock(Call.class);
        when(okClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(successResponse);

        invokeProcessEvent(eventJson);

        // 验证状态设置为 SUCCESS
        verify(statusBucket).set(eq(IngestionService.STATUS_SUCCESS), any(Duration.class));
        verify(metricsRegistry).recordDelivery("TEST_SUPPLIER", "success");
    }

    @Test
    @DisplayName("瞬态失败(未达重试上限): 触发指数退避重试")
    void processEvent_transientFailure_triggersRetry() throws Exception {
        String eventJson = buildEventJson("EVT-002", 1); // retryCount=1, max=3
        SupplierConfig config = buildConfig();

        when(statusBucket.get()).thenReturn(null);
        when(configDomainService.getBySupplierCode("TEST_SUPPLIER")).thenReturn(config);
        when(contextBuilder.build(anyString(), any())).thenReturn(Map.of("event", Map.of()));

        Request okRequest = new Request.Builder().url("https://api.test.com/notify").build();
        OkHttpClient okClient = mock(OkHttpClient.class);
        when(requestBuilder.buildRequest(eq(config), any())).thenReturn(okRequest);
        when(requestBuilder.deriveClient(config)).thenReturn(okClient);

        // mock HTTP 500 错误
        Response failResponse = new Response.Builder()
                .request(okRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body(ResponseBody.create("server error", MediaType.parse("text/plain")))
                .build();

        Call call = mock(Call.class);
        when(okClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(failResponse);

        invokeProcessEvent(eventJson);

        // 验证延迟队列重入队（重试）
        verify(delayedQueue).offer(anyString(), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(metricsRegistry).recordDelivery("TEST_SUPPLIER", "failed");

        // 不应进入死信
        verify(deadLetterHandler, never()).handleDeadLetter(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("重试耗尽: 触发 DLQ")
    void processEvent_maxRetryExhausted_triggersDlq() throws Exception {
        String eventJson = buildEventJson("EVT-003", 3); // retryCount=3 >= maxRetry=3
        SupplierConfig config = buildConfig();

        when(statusBucket.get()).thenReturn(null);
        when(configDomainService.getBySupplierCode("TEST_SUPPLIER")).thenReturn(config);
        when(contextBuilder.build(anyString(), any())).thenReturn(Map.of("event", Map.of()));
        when(contextBuilder.serialize(any())).thenReturn("{}");

        Request okRequest = new Request.Builder().url("https://api.test.com/notify").build();
        OkHttpClient okClient = mock(OkHttpClient.class);
        when(requestBuilder.buildRequest(eq(config), any())).thenReturn(okRequest);
        when(requestBuilder.deriveClient(config)).thenReturn(okClient);

        // mock HTTP 500
        Response failResponse = new Response.Builder()
                .request(okRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body(ResponseBody.create("server error", MediaType.parse("text/plain")))
                .build();

        Call call = mock(Call.class);
        when(okClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(failResponse);

        invokeProcessEvent(eventJson);

        // 验证进入死信
        verify(deadLetterHandler).handleDeadLetter(
                eq("EVT-003"), eq("T-123"), eq("TEST_SUPPLIER"),
                anyString(), contains("Max retry exhausted"), eq(3));
        verify(metricsRegistry).recordDelivery("TEST_SUPPLIER", "dlq");

        // 不应再重试
        verify(delayedQueue, never()).offer(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("JSONata 错误: 直接进入 DLQ, 不重试")
    void processEvent_jsonataError_directlyDlq() throws Exception {
        String eventJson = buildEventJson("EVT-004", 0);
        SupplierConfig config = buildConfig();

        when(statusBucket.get()).thenReturn(null);
        when(configDomainService.getBySupplierCode("TEST_SUPPLIER")).thenReturn(config);
        when(contextBuilder.build(anyString(), any())).thenReturn(Map.of("event", Map.of()));
        when(contextBuilder.serialize(any())).thenReturn("{}");

        // JSONata 转换时抛出异常
        when(requestBuilder.buildRequest(eq(config), any()))
                .thenThrow(new TranslationEngineException("Invalid expression", 5));

        invokeProcessEvent(eventJson);

        // 验证直接进入死信
        verify(deadLetterHandler).handleDeadLetter(
                eq("EVT-004"), eq("T-123"), eq("TEST_SUPPLIER"),
                anyString(), contains("JSONata"), eq(0));
        verify(metricsRegistry).recordDelivery("TEST_SUPPLIER", "dlq");

        // 不应重试
        verify(delayedQueue, never()).offer(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("已成功的事件: 跳过重复投递")
    void processEvent_alreadySuccess_skipsDelivery() throws Exception {
        String eventJson = buildEventJson("EVT-005", 0);

        when(statusBucket.get()).thenReturn(IngestionService.STATUS_SUCCESS);

        invokeProcessEvent(eventJson);

        // 不应调用任何投递逻辑
        verify(configDomainService, never()).getBySupplierCode(anyString());
        verify(requestBuilder, never()).buildRequest(any(), any());
    }

    @Test
    @DisplayName("供应商配置不存在: 跳过投递")
    void processEvent_supplierNotFound_skips() throws Exception {
        String eventJson = buildEventJson("EVT-006", 0);

        when(statusBucket.get()).thenReturn(null);
        when(configDomainService.getBySupplierCode("TEST_SUPPLIER")).thenReturn(null);

        invokeProcessEvent(eventJson);

        // 不应构建请求
        verify(requestBuilder, never()).buildRequest(any(), any());
        verify(deadLetterHandler, never()).handleDeadLetter(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyInt());
    }
}
