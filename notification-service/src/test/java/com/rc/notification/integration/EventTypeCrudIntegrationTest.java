package com.rc.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.interfaces.admin.dto.EventTypeCreateRequest;
import com.rc.notification.interfaces.admin.dto.EventTypeUpdateRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EventType CRUD API integration tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EventTypeCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;
    private static final String PUBLISHER_CODE = "PUB_ET_TEST";

    @BeforeEach
    void setUp() throws Exception {
        session = AuthIntegrationTest.loginAndGetSession(mockMvc, objectMapper);
        // Create a publisher to use in event type tests
        PublisherCreateRequest pubReq = new PublisherCreateRequest();
        pubReq.setPublisherCode(PUBLISHER_CODE);
        pubReq.setPublisherName("EventType Test Publisher");
        pubReq.setContactInfo("et@example.com");
        mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pubReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Create event type -> 200, verify eventTypeCode, status=DRAFT, version=1")
    void createEventType() throws Exception {
        EventTypeCreateRequest req = buildCreateRequest("ET_CREATE", PUBLISHER_CODE, "Create Event");

        mockMvc.perform(post("/api/v1/admin/event-types")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTypeCode").value("ET_CREATE"))
                .andExpect(jsonPath("$.publisherCode").value(PUBLISHER_CODE))
                .andExpect(jsonPath("$.displayName").value("Create Event"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("List by publisherCode -> verify filtered results")
    void listByPublisherCode() throws Exception {
        // Create two event types under the publisher
        mockMvc.perform(post("/api/v1/admin/event-types")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("ET_LIST_1", PUBLISHER_CODE, "List Event 1"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/event-types")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("ET_LIST_2", PUBLISHER_CODE, "List Event 2"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/event-types")
                        .session(session)
                        .param("publisherCode", PUBLISHER_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("Duplicate eventTypeCode -> 400")
    void duplicateEventTypeCodeReturns400() throws Exception {
        EventTypeCreateRequest req = buildCreateRequest("ET_DUP", PUBLISHER_CODE, "Dup Event");

        // First create - success
        mockMvc.perform(post("/api/v1/admin/event-types")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Second create with same code - should return 400
        mockMvc.perform(post("/api/v1/admin/event-types")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("ET_DUP", PUBLISHER_CODE, "Dup Event 2"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update with new payloadSchema -> version increments to 2")
    void updatePayloadSchemaIncrementsVersion() throws Exception {
        // Create event type with initial schema
        EventTypeCreateRequest createReq = buildCreateRequest("ET_SCHEMA", PUBLISHER_CODE, "Schema Event");
        createReq.setPayloadSchema("{\"type\":\"object\"}");

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/event-types")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Update with new payloadSchema
        EventTypeUpdateRequest updateReq = new EventTypeUpdateRequest();
        updateReq.setPayloadSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");

        mockMvc.perform(put("/api/v1/admin/event-types/{id}", id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("Activate event type -> PUT with status=ACTIVE")
    void activateEventType() throws Exception {
        // Create event type
        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/event-types")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("ET_ACTIVATE", PUBLISHER_CODE, "Activate Event"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Activate
        EventTypeUpdateRequest updateReq = new EventTypeUpdateRequest();
        updateReq.setStatus("ACTIVE");

        mockMvc.perform(put("/api/v1/admin/event-types/{id}", id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private EventTypeCreateRequest buildCreateRequest(String code, String publisherCode, String displayName) {
        EventTypeCreateRequest req = new EventTypeCreateRequest();
        req.setEventTypeCode(code);
        req.setPublisherCode(publisherCode);
        req.setDisplayName(displayName);
        return req;
    }
}
