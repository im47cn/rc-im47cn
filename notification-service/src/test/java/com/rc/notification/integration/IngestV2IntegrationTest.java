package com.rc.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.interfaces.admin.dto.PublisherCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v2 Ingest API integration tests covering:
 * - Fan-out to multiple subscribers
 * - Targeted delivery to single subscriber
 * - Missing X-Publisher-Key returns 401
 * - No subscribers returns 200 ACCEPTED with empty dispatches
 * - Idempotent: same eventId twice returns IDEMPOTENT_HIT
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class IngestV2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestConfig testConfig;

    private MockHttpSession session;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        testConfig.clearAll();
        session = AuthIntegrationTest.loginAndGetSession(mockMvc, objectMapper);
    }

    /**
     * Create a publisher via admin API and return its apiKey.
     */
    private String createPublisher(String publisherCode, String publisherName) throws Exception {
        var req = new PublisherCreateRequest();
        req.setPublisherCode(publisherCode);
        req.setPublisherName(publisherName);

        String body = mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("apiKey").asText();
    }

    /**
     * Create an event type and activate it. Returns the event type id.
     */
    private Long createAndActivateEventType(String eventTypeCode, String publisherCode) throws Exception {
        var etReq = Map.of("eventTypeCode", eventTypeCode, "publisherCode", publisherCode, "displayName", "Test " + eventTypeCode);
        String etBody = mockMvc.perform(post("/api/v1/admin/event-types")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(etReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long etId = objectMapper.readTree(etBody).get("id").asLong();

        // Activate
        mockMvc.perform(put("/api/v1/admin/event-types/" + etId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk());

        return etId;
    }

    /**
     * Create a supplier config (subscriber) via admin API.
     */
    private void createSupplier(String code) throws Exception {
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

    /**
     * Create a subscription via admin API.
     */
    private void createSubscription(String subscriberCode, String eventTypeCode) throws Exception {
        var subReq = Map.of("subscriberCode", subscriberCode, "eventTypeCode", eventTypeCode);
        mockMvc.perform(post("/api/v1/admin/subscriptions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Fan-out: v2 ingest dispatches to all active subscribers")
    void fanOutDispatchesToAllSubscribers() throws Exception {
        String key = createPublisher("pub-fanout", "Fan-out Publisher");
        createAndActivateEventType("ORDER_CREATED", "pub-fanout");

        createSupplier("sub-a");
        createSupplier("sub-b");
        createSubscription("sub-a", "ORDER_CREATED");
        createSubscription("sub-b", "ORDER_CREATED");

        var ingestReq = Map.of(
                "eventId", "evt-fanout-001",
                "eventType", "ORDER_CREATED",
                "payload", Map.of("orderId", "123")
        );

        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .header("X-Publisher-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.eventId").value("evt-fanout-001"))
                .andExpect(jsonPath("$.dispatches.length()").value(2))
                .andExpect(jsonPath("$.dispatches[0].status").value("QUEUED"))
                .andExpect(jsonPath("$.dispatches[1].status").value("QUEUED"));
    }

    @Test
    @DisplayName("Targeted: v2 ingest with subscriberCode dispatches to one subscriber")
    void targetedDispatchesToOneSubscriber() throws Exception {
        String key = createPublisher("pub-targeted", "Targeted Publisher");
        createAndActivateEventType("ORDER_SHIPPED", "pub-targeted");

        createSupplier("sub-x");
        createSupplier("sub-y");
        createSubscription("sub-x", "ORDER_SHIPPED");
        createSubscription("sub-y", "ORDER_SHIPPED");

        var ingestReq = Map.of(
                "eventId", "evt-targeted-001",
                "eventType", "ORDER_SHIPPED",
                "payload", Map.of("orderId", "456"),
                "subscriberCode", "sub-x"
        );

        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .header("X-Publisher-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.dispatches.length()").value(1))
                .andExpect(jsonPath("$.dispatches[0].subscriberCode").value("sub-x"))
                .andExpect(jsonPath("$.dispatches[0].status").value("QUEUED"));
    }

    @Test
    @DisplayName("Missing X-Publisher-Key returns 401")
    void missingPublisherKeyReturns401() throws Exception {
        var ingestReq = Map.of(
                "eventId", "evt-nokey-001",
                "eventType", "ORDER_CREATED",
                "payload", Map.of("orderId", "123")
        );

        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("No subscribers: returns 200 ACCEPTED with empty dispatches")
    void noSubscribersReturnsAcceptedWithEmptyDispatches() throws Exception {
        String key = createPublisher("pub-nosub", "No-sub Publisher");
        createAndActivateEventType("ORDER_CANCELLED", "pub-nosub");

        var ingestReq = Map.of(
                "eventId", "evt-nosub-001",
                "eventType", "ORDER_CANCELLED",
                "payload", Map.of("orderId", "789")
        );

        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .header("X-Publisher-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.dispatches.length()").value(0));
    }

    @Test
    @DisplayName("Unknown event type: auto-registers as DRAFT and returns ACCEPTED with empty dispatches")
    void unknownEventTypeAutoRegistersDraft() throws Exception {
        String key = createPublisher("pub-auto", "Auto-register Publisher");

        var ingestReq = Map.of(
                "eventId", "evt-auto-001",
                "eventType", "NEW_UNKNOWN_EVENT",
                "payload", Map.of("orderId", "auto-123")
        );

        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .header("X-Publisher-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.dispatches.length()").value(0));

        // Verify the event type was created as DRAFT via admin API
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/admin/event-types")
                        .session(session)
                        .param("keyword", "NEW_UNKNOWN_EVENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].eventTypeCode").value("NEW_UNKNOWN_EVENT"))
                .andExpect(jsonPath("$.records[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.records[0].publisherCode").value("pub-auto"));
    }

    @Test
    @DisplayName("Idempotent: same eventId twice to same subscriber returns IDEMPOTENT_HIT")
    void idempotentHitOnDuplicateEvent() throws Exception {
        String key = createPublisher("pub-idem", "Idempotent Publisher");
        createAndActivateEventType("ORDER_PAID", "pub-idem");

        createSupplier("sub-idem");
        createSubscription("sub-idem", "ORDER_PAID");

        var ingestReq = Map.of(
                "eventId", "evt-idem-001",
                "eventType", "ORDER_PAID",
                "payload", Map.of("orderId", "999")
        );

        // First ingest - QUEUED
        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .header("X-Publisher-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatches[0].status").value("QUEUED"));

        // Second ingest - same eventId, should be IDEMPOTENT_HIT
        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .header("X-Publisher-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatches[0].status").value("IDEMPOTENT_HIT"));
    }
}
