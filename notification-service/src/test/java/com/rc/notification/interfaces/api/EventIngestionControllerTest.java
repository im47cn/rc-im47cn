package com.rc.notification.interfaces.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.integration.TestConfig;
import com.rc.notification.interfaces.admin.dto.LoginRequest;
import com.rc.notification.interfaces.admin.dto.SupplierConfigCreateRequest;
import com.rc.notification.interfaces.api.dto.NotificationEventDto;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EventIngestionController MockMvc integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class EventIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() throws Exception {
        adminSession = loginAndGetSession();
    }

    @Test
    @DisplayName("Valid event returns 200 ACCEPTED")
    void validEventReturnsAccepted() throws Exception {
        createSupplier("TC_INGEST_OK");

        NotificationEventDto event = buildEvent("tc-evt-001", "TC_INGEST_OK");

        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.eventId").value("tc-evt-001"));
    }

    @Test
    @DisplayName("Missing required fields returns 400")
    void missingRequiredFieldsReturns400() throws Exception {
        NotificationEventDto event = new NotificationEventDto();
        // eventId is set, but supplierCode, eventType, payload are missing
        event.setEventId("tc-evt-validation");

        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Idempotent duplicate returns IDEMPOTENT_HIT (202)")
    void idempotentDuplicateReturnsHit() throws Exception {
        createSupplier("TC_INGEST_IDEM");

        NotificationEventDto event = buildEvent("tc-evt-idem-001", "TC_INGEST_IDEM");

        // First ingest
        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Second ingest - same eventId, should be idempotent hit
        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("IDEMPOTENT_HIT"));
    }

    @Test
    @DisplayName("Non-existent supplier returns REJECTED (400)")
    void nonExistentSupplierReturnsRejected() throws Exception {
        NotificationEventDto event = buildEvent("tc-evt-nosup", "NON_EXISTENT_SUPPLIER");

        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    private void createSupplier(String code) throws Exception {
        SupplierConfigCreateRequest req = new SupplierConfigCreateRequest();
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
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private NotificationEventDto buildEvent(String eventId, String supplierCode) {
        NotificationEventDto dto = new NotificationEventDto();
        dto.setEventId(eventId);
        dto.setSupplierCode(supplierCode);
        dto.setEventType("ORDER_CREATED");
        dto.setPayload(Map.of("orderId", "123", "amount", 99.99));
        return dto;
    }

    private MockHttpSession loginAndGetSession() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin");

        MvcResult result = mockMvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
