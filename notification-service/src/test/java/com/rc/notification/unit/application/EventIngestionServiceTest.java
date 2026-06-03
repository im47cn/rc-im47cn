package com.rc.notification.unit.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.application.service.IngestionService;
import com.rc.notification.domain.config.SupplierConfig;
import com.rc.notification.domain.config.SupplierConfigDomainService;
import com.rc.notification.infrastructure.metrics.NotificationMetricsRegistry;
import com.rc.notification.interfaces.api.dto.IngestResponse;
import com.rc.notification.interfaces.api.dto.NotificationEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IngestionService (EventIngestionService) 单元测试
 * <p>
 * 验证幂等入队逻辑：分布式锁、状态判重、队列入队
 */
@ExtendWith(MockitoExtension.class)
class EventIngestionServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private SupplierConfigDomainService configDomainService;

    @Mock
    private NotificationMetricsRegistry metricsRegistry;

    @Mock
    private RLock lock;

    @Mock
    private RBucket<Object> bucket;

    @Mock
    private RQueue<String> queue;

    private IngestionService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        service = new IngestionService(redissonClient, configDomainService, objectMapper, metricsRegistry);

        // 默认 mock：锁获取成功 (lenient 避免部分测试未使用这些 stub 的报错)
        lenient().when(redissonClient.getLock(anyString())).thenReturn(lock);
        lenient().when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(lock.isHeldByCurrentThread()).thenReturn(true);

        // 默认 mock：bucket 和 queue (lenient + doReturn 避免泛型推断问题)
        lenient().doReturn(bucket).when(redissonClient).getBucket(anyString());
        lenient().doReturn(queue).when(redissonClient).getQueue(anyString());
    }

    private NotificationEventDto buildEvent(String eventId, String supplierCode) {
        NotificationEventDto dto = new NotificationEventDto();
        dto.setEventId(eventId);
        dto.setSupplierCode(supplierCode);
        dto.setEventType("ORDER_CREATED");
        dto.setPayload(Map.of("orderId", "ORD-001"));
        return dto;
    }

    private SupplierConfig buildActiveConfig(String code) {
        SupplierConfig config = new SupplierConfig();
        config.setSupplierCode(code);
        config.setStatus(1);
        return config;
    }

    @Test
    @DisplayName("新事件: 获取锁, 设置 PROCESSING, 入队成功")
    void ingest_newEvent_accepted() {
        NotificationEventDto event = buildEvent("EVT-001", "WECHAT");
        when(configDomainService.getBySupplierCode("WECHAT")).thenReturn(buildActiveConfig("WECHAT"));
        when(bucket.get()).thenReturn(null); // 无已有状态
        when(queue.add(anyString())).thenReturn(true);

        IngestResponse response = service.ingest(event);

        assertEquals("ACCEPTED", response.getStatus());
        assertEquals("EVT-001", response.getEventId());

        // 验证设置状态为 PROCESSING
        verify(bucket).set(eq(IngestionService.STATUS_PROCESSING), any(Duration.class));

        // 验证入队
        verify(queue).add(anyString());

        // 验证记录指标
        verify(metricsRegistry).recordIngest("WECHAT", "accepted");
    }

    @Test
    @DisplayName("重复事件(PROCESSING): 幂等命中, 拒绝入队")
    void ingest_duplicateProcessing_rejected() {
        NotificationEventDto event = buildEvent("EVT-001", "WECHAT");
        when(configDomainService.getBySupplierCode("WECHAT")).thenReturn(buildActiveConfig("WECHAT"));
        when(bucket.get()).thenReturn("PROCESSING");

        IngestResponse response = service.ingest(event);

        assertEquals("IDEMPOTENT_HIT", response.getStatus());
        verify(queue, never()).add(anyString());
        verify(metricsRegistry).recordIngest("WECHAT", "idempotent_hit");
    }

    @Test
    @DisplayName("重复事件(SUCCESS): 幂等命中, 拒绝入队")
    void ingest_duplicateSuccess_rejected() {
        NotificationEventDto event = buildEvent("EVT-001", "WECHAT");
        when(configDomainService.getBySupplierCode("WECHAT")).thenReturn(buildActiveConfig("WECHAT"));
        when(bucket.get()).thenReturn("SUCCESS");

        IngestResponse response = service.ingest(event);

        assertEquals("IDEMPOTENT_HIT", response.getStatus());
        verify(queue, never()).add(anyString());
    }

    @Test
    @DisplayName("重复事件(DEAD_LETTERED): 返回死信状态提示")
    void ingest_duplicateDeadLettered_returnsDlqHint() {
        NotificationEventDto event = buildEvent("EVT-001", "WECHAT");
        when(configDomainService.getBySupplierCode("WECHAT")).thenReturn(buildActiveConfig("WECHAT"));
        when(bucket.get()).thenReturn("DEAD_LETTERED");

        IngestResponse response = service.ingest(event);

        assertEquals("DEAD_LETTERED", response.getStatus());
        assertTrue(response.getMessage().contains("死信"));
        verify(queue, never()).add(anyString());
    }

    @Test
    @DisplayName("供应商不存在或未启用时应拒绝")
    void ingest_supplierNotActive_rejected() {
        NotificationEventDto event = buildEvent("EVT-001", "UNKNOWN");
        when(configDomainService.getBySupplierCode("UNKNOWN")).thenReturn(null);

        IngestResponse response = service.ingest(event);

        assertEquals("REJECTED", response.getStatus());
        verify(metricsRegistry).recordIngest("UNKNOWN", "rejected");
    }

    @Test
    @DisplayName("供应商已禁用时应拒绝")
    void ingest_supplierDisabled_rejected() {
        NotificationEventDto event = buildEvent("EVT-001", "DISABLED");
        SupplierConfig disabledConfig = new SupplierConfig();
        disabledConfig.setSupplierCode("DISABLED");
        disabledConfig.setStatus(0);
        when(configDomainService.getBySupplierCode("DISABLED")).thenReturn(disabledConfig);

        IngestResponse response = service.ingest(event);

        assertEquals("REJECTED", response.getStatus());
    }

    @Test
    @DisplayName("获取锁失败时返回幂等命中")
    void ingest_lockFailed_returnsIdempotentHit() throws Exception {
        NotificationEventDto event = buildEvent("EVT-001", "WECHAT");
        when(configDomainService.getBySupplierCode("WECHAT")).thenReturn(buildActiveConfig("WECHAT"));
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        IngestResponse response = service.ingest(event);

        assertEquals("IDEMPOTENT_HIT", response.getStatus());
        verify(queue, never()).add(anyString());
    }

    @Test
    @DisplayName("traceId 为空时应自动生成")
    void ingest_noTraceId_autoGenerated() {
        NotificationEventDto event = buildEvent("EVT-001", "WECHAT");
        event.setTraceId(null);
        when(configDomainService.getBySupplierCode("WECHAT")).thenReturn(buildActiveConfig("WECHAT"));
        when(bucket.get()).thenReturn(null);
        when(queue.add(anyString())).thenReturn(true);

        IngestResponse response = service.ingest(event);

        assertEquals("ACCEPTED", response.getStatus());
        assertNotNull(event.getTraceId());
        assertTrue(event.getTraceId().startsWith("T-"));
    }
}
