package com.rc.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.interfaces.admin.dto.EventTypeCreateRequest;
import com.rc.notification.interfaces.admin.dto.EventTypeUpdateRequest;
import com.rc.notification.interfaces.admin.dto.PublisherCreateRequest;
import com.rc.notification.interfaces.admin.dto.SubscriptionCreateRequest;
import com.rc.notification.interfaces.admin.dto.SubscriptionUpdateRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Subscription CRUD API integration tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SubscriptionCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    private static final String PUBLISHER_CODE = "PUB_SUB_TEST";
    private static final String EVENT_TYPE_CODE = "ET_SUB_TEST";
    private static final String SUBSCRIBER_CODE = "SUB_TEST_01";

    @BeforeEach
    void setUp() throws Exception {
        session = AuthIntegrationTest.loginAndGetSession(mockMvc, objectMapper);
        // 1. Create publisher
        createPublisher(session, PUBLISHER_CODE);
        // 2. Create event type (DRAFT)
        Long eventTypeId = createEventType(session, EVENT_TYPE_CODE, PUBLISHER_CODE);
        // 3. Activate event type
        activateEventType(session, eventTypeId);
        // 4. Create subscriber (SupplierConfig)
        createSubscriber(session, SUBSCRIBER_CODE);
    }

    @Test
    @DisplayName("Create subscription -> 200, verify subscriberCode, eventTypeCode, status=ACTIVE, managedBy=SUBSCRIBER")
    void createSubscription() throws Exception {
        SubscriptionCreateRequest req = buildCreateRequest(SUBSCRIBER_CODE, EVENT_TYPE_CODE);

        mockMvc.perform(post("/api/v1/admin/subscriptions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriberCode").value(SUBSCRIBER_CODE))
                .andExpect(jsonPath("$.eventTypeCode").value(EVENT_TYPE_CODE))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.managedBy").value("SUBSCRIBER"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Create with override bodyTemplate -> 200, verify bodyTemplate persisted")
    void createWithBodyTemplateOverride() throws Exception {
        SubscriptionCreateRequest req = buildCreateRequest(SUBSCRIBER_CODE, EVENT_TYPE_CODE);
        req.setBodyTemplate("{\"custom\": true}");

        mockMvc.perform(post("/api/v1/admin/subscriptions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyTemplate").value("{\"custom\": true}"));
    }

    @Test
    @DisplayName("List by subscriberCode -> verify filtered results")
    void listBySubscriberCode() throws Exception {
        // Create another subscriber and subscription
        createSubscriber(session, "SUB_OTHER_01");
        Long otherEventTypeId = createEventType(session, "ET_OTHER_01", PUBLISHER_CODE);
        activateEventType(session, otherEventTypeId);

        // Create subscription for main subscriber
        mockMvc.perform(post("/api/v1/admin/subscriptions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(SUBSCRIBER_CODE, EVENT_TYPE_CODE))))
                .andExpect(status().isOk());

        // Create subscription for other subscriber
        mockMvc.perform(post("/api/v1/admin/subscriptions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("SUB_OTHER_01", "ET_OTHER_01"))))
                .andExpect(status().isOk());

        // List by subscriberCode - should only return main subscriber's subscriptions
        mockMvc.perform(get("/api/v1/admin/subscriptions")
                        .session(session)
                        .param("subscriberCode", SUBSCRIBER_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.records[0].subscriberCode").value(SUBSCRIBER_CODE));
    }

    @Test
    @DisplayName("Duplicate (subscriberCode + eventTypeCode) -> 400")
    void duplicateSubscriptionReturns400() throws Exception {
        SubscriptionCreateRequest req = buildCreateRequest(SUBSCRIBER_CODE, EVENT_TYPE_CODE);

        // First create - success
        mockMvc.perform(post("/api/v1/admin/subscriptions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Second create with same (subscriberCode, eventTypeCode) - should return 400
        mockMvc.perform(post("/api/v1/admin/subscriptions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Suspend subscription -> PUT with status=SUSPENDED")
    void suspendSubscription() throws Exception {
        // Create subscription
        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/subscriptions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(SUBSCRIBER_CODE, EVENT_TYPE_CODE))))
                .andExpect(status().isOk())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Suspend
        SubscriptionUpdateRequest updateReq = new SubscriptionUpdateRequest();
        updateReq.setStatus("SUSPENDED");

        mockMvc.perform(put("/api/v1/admin/subscriptions/{id}", id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    // --- Helpers ---

    private void createPublisher(MockHttpSession session, String code) throws Exception {
        PublisherCreateRequest req = new PublisherCreateRequest();
        req.setPublisherCode(code);
        req.setPublisherName("Test Publisher " + code);
        req.setContactInfo("contact@example.com");
        mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private Long createEventType(MockHttpSession session, String code, String publisherCode) throws Exception {
        EventTypeCreateRequest req = new EventTypeCreateRequest();
        req.setEventTypeCode(code);
        req.setPublisherCode(publisherCode);
        req.setDisplayName("Test Event " + code);
        MvcResult result = mockMvc.perform(post("/api/v1/admin/event-types")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void activateEventType(MockHttpSession session, Long id) throws Exception {
        EventTypeUpdateRequest req = new EventTypeUpdateRequest();
        req.setStatus("ACTIVE");
        mockMvc.perform(put("/api/v1/admin/event-types/{id}", id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private void createSubscriber(MockHttpSession session, String code) throws Exception {
        var req = new com.rc.notification.interfaces.admin.dto.SupplierConfigCreateRequest();
        req.setSupplierCode(code);
        req.setSupplierName("Test " + code);
        req.setBaseUrl("https://api.test.com");
        req.setBodyTemplate("'{\"test\": true}'");
        req.setStatus(1);
        req.setMaxRetryCount(3);
        req.setRetryBackoffInitialMs(1000);
        req.setRetryBackoffMultiplier(new BigDecimal("2.00"));
        req.setRetryBackoffMaxMs(30000);
        req.setWorkerConcurrency(1);
        mockMvc.perform(post("/api/v1/admin/suppliers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private SubscriptionCreateRequest buildCreateRequest(String subscriberCode, String eventTypeCode) {
        SubscriptionCreateRequest req = new SubscriptionCreateRequest();
        req.setSubscriberCode(subscriberCode);
        req.setEventTypeCode(eventTypeCode);
        return req;
    }
}
