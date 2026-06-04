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
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Publisher CRUD API integration tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PublisherCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        session = AuthIntegrationTest.loginAndGetSession(mockMvc, objectMapper);
    }

    @Test
    @DisplayName("Create publisher -> verify publisherCode, apiKey not empty, status=1")
    void createPublisher() throws Exception {
        PublisherCreateRequest req = buildCreateRequest("PUB_TEST", "Test Publisher");

        mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publisherCode").value("PUB_TEST"))
                .andExpect(jsonPath("$.publisherName").value("Test Publisher"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.apiKey").value(not(emptyString())))
                .andExpect(jsonPath("$.apiKey").value(startsWith("pk_")))
                .andExpect(jsonPath("$.status").value(1));
    }

    @Test
    @DisplayName("List publishers with pagination -> verify total count")
    void listPublishersWithPagination() throws Exception {
        // Create two publishers
        mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("PUB_LIST_1", "List Publisher 1"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("PUB_LIST_2", "List Publisher 2"))))
                .andExpect(status().isOk());

        // List with keyword filter - total should be 2
        mockMvc.perform(get("/api/v1/admin/publishers")
                        .session(session)
                        .param("keyword", "PUB_LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("Duplicate publisherCode -> 400")
    void duplicatePublisherCodeReturns400() throws Exception {
        PublisherCreateRequest req = buildCreateRequest("PUB_DUP", "First Publisher");

        // Create first - success
        mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Create second with same code - should return 400
        PublisherCreateRequest req2 = buildCreateRequest("PUB_DUP", "Second Publisher");
        mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Rotate API key -> 200, new apiKey returned")
    void rotateApiKey() throws Exception {
        // Create publisher
        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("PUB_ROTATE", "Rotate Publisher"))))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(responseJson).get("id").asLong();
        String originalApiKey = objectMapper.readTree(responseJson).get("apiKey").asText();

        // Rotate key
        MvcResult rotateResult = mockMvc.perform(post("/api/v1/admin/publishers/{id}/rotate-key", id)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").value(not(emptyString())))
                .andExpect(jsonPath("$.apiKey").value(startsWith("pk_")))
                .andReturn();

        String newApiKey = objectMapper.readTree(rotateResult.getResponse().getContentAsString())
                .get("apiKey").asText();

        // New key should differ from original
        org.junit.jupiter.api.Assertions.assertNotEquals(originalApiKey, newApiKey);
    }

    private PublisherCreateRequest buildCreateRequest(String code, String name) {
        PublisherCreateRequest req = new PublisherCreateRequest();
        req.setPublisherCode(code);
        req.setPublisherName(name);
        req.setContactInfo("contact@example.com");
        return req;
    }
}
