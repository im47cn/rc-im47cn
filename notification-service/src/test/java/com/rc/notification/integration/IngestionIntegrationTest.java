package com.rc.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.interfaces.api.dto.NotificationEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T24-1,2,3,4: Event ingestion integration tests covering:
 * - Happy path (ingest returns ACCEPTED)
 * - Idempotent duplicate rejection
 * - DEAD_LETTERED state rejection
 * - Redis degradation returns 503 + Retry-After
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class IngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TestConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig.clearAll();
    }

    private void insertTestSupplier(String code) throws Exception {
        var session = AuthIntegrationTest.loginAndGetSession(mockMvc, objectMapper);

        var createReq = new com.rc.notification.interfaces.admin.dto.SupplierConfigCreateRequest();
        createReq.setSupplierCode(code);
        createReq.setSupplierName("Test " + code);
        createReq.setBaseUrl("https://api.test.com");
        createReq.setBodyTemplate("'{\"test\": true}'");
        createReq.setStatus(1);
        createReq.setMaxRetryCount(3);
        createReq.setRetryBackoffInitialMs(1000);
        createReq.setRetryBackoffMultiplier(new BigDecimal("2.00"));
        createReq.setRetryBackoffMaxMs(30000);
        createReq.setWorkerConcurrency(1);

        mockMvc.perform(post("/api/v1/admin/suppliers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Happy path: ingest event returns 200 ACCEPTED")
    void happyPathIngestReturnsAccepted() throws Exception {
        insertTestSupplier("INGEST_HAPPY");

        NotificationEventDto event = buildEvent("evt-happy-001", "INGEST_HAPPY");

        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.eventId").value("evt-happy-001"));
    }

    @Test
    @DisplayName("Idempotent: duplicate biz_sign returns IDEMPOTENT_HIT")
    void duplicateBizSignReturnsIdempotentHit() throws Exception {
        insertTestSupplier("INGEST_IDEMPOTENT");

        NotificationEventDto event = buildEvent("evt-dup-001", "INGEST_IDEMPOTENT");

        // First ingest - ACCEPTED
        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Second ingest - same biz_sign, should be IDEMPOTENT_HIT
        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IDEMPOTENT_HIT"));
    }

    @Test
    @DisplayName("DEAD_LETTERED state rejects new ingest with 409")
    void deadLetteredStateRejectsIngest() throws Exception {
        insertTestSupplier("INGEST_DL");

        // Pre-set status to DEAD_LETTERED in mock redis
        testConfig.getRedisStore().put("status:dispatch:evt-dl-001", "DEAD_LETTERED");

        NotificationEventDto event = buildEvent("evt-dl-001", "INGEST_DL");

        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("DEAD_LETTERED"));
    }

    @Test
    @DisplayName("Rejected: supplier not found or disabled")
    void supplierNotFoundReturnsRejected() throws Exception {
        NotificationEventDto event = buildEvent("evt-nf-001", "NON_EXISTENT_SUPPLIER");

        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Validation: missing required fields returns 400")
    void missingRequiredFieldsReturns400() throws Exception {
        NotificationEventDto event = new NotificationEventDto();
        event.setEventId("evt-validation-001");

        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Redis degradation: returns 503 with Retry-After header")
    void redisUnavailableReturns503() throws Exception {
        insertTestSupplier("INGEST_REDIS_DOWN");

        // Get the shared lock mock via RedissonClient and temporarily make it throw
        RLock lockMock = redissonClient.getLock("any-key");
        try {
            // Reconfigure to throw RedisException
            when(lockMock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                    .thenThrow(new RedisException("Connection refused"));

            NotificationEventDto event = buildEvent("evt-redis-down", "INGEST_REDIS_DOWN");

            mockMvc.perform(post("/api/v1/notifications/ingest")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(event)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(header().string("Retry-After", "5"));
        } finally {
            // Restore normal behavior
            when(lockMock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);
        }
    }

    private NotificationEventDto buildEvent(String eventId, String supplierCode) {
        NotificationEventDto dto = new NotificationEventDto();
        dto.setEventId(eventId);
        dto.setSupplierCode(supplierCode);
        dto.setEventType("ORDER_CREATED");
        dto.setPayload(Map.of("orderId", "123", "amount", 99.99));
        return dto;
    }
}
