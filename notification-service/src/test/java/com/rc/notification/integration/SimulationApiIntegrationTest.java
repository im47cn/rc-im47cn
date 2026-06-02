package com.rc.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.interfaces.admin.dto.FullPreviewRequest;
import com.rc.notification.interfaces.admin.dto.SimulationRequest;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T24-6: Simulation API integration tests (transform + full-preview)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class SimulationApiIntegrationTest {

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
    @DisplayName("Valid JSONata expression returns correct transform result")
    void validExpressionReturnsResult() throws Exception {
        SimulationRequest request = new SimulationRequest();
        request.setJsonataExpression("payload.name");
        request.setMockInputContext(Map.of("payload", Map.of("name", "test-user")));

        mockMvc.perform(post("/api/v1/admin/simulation/transform")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transformedResult").value("test-user"));
    }

    @Test
    @DisplayName("Invalid JSONata expression returns error with offset")
    void invalidExpressionReturnsError() throws Exception {
        SimulationRequest request = new SimulationRequest();
        request.setJsonataExpression("$$$invalid{{{");
        request.setMockInputContext(Map.of("payload", Map.of("name", "test")));

        mockMvc.perform(post("/api/v1/admin/simulation/transform")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Full-preview returns complete HTTP request preview")
    void fullPreviewReturnsCompletePreview() throws Exception {
        FullPreviewRequest request = new FullPreviewRequest();
        request.setBaseUrl("https://api.example.com");
        request.setHttpMethod("POST");
        request.setContentTypeBehavior("APPLICATION_JSON");
        request.setBodyTemplate("'{\"msg\": \"' & payload.content & '\"}'");
        request.setMockInputContext(Map.of("payload", Map.of("content", "hello")));

        mockMvc.perform(post("/api/v1/admin/simulation/full-preview")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resolvedUrl").value("https://api.example.com"))
                .andExpect(jsonPath("$.httpMethod").value("POST"))
                .andExpect(jsonPath("$.headers['Content-Type']").value("application/json"))
                .andExpect(jsonPath("$.body").exists());
    }

    @Test
    @DisplayName("Full-preview with path template resolves URL correctly")
    void fullPreviewResolvesPathTemplate() throws Exception {
        FullPreviewRequest request = new FullPreviewRequest();
        request.setBaseUrl("https://api.example.com");
        request.setHttpMethod("POST");
        request.setPathTemplate("'/v2/send'");
        request.setBodyTemplate("'{\"data\": true}'");
        request.setMockInputContext(Map.of("payload", Map.of()));

        mockMvc.perform(post("/api/v1/admin/simulation/full-preview")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.resolvedUrl").value("https://api.example.com/v2/send"));
    }

    @Test
    @DisplayName("Simulation requires authentication")
    void simulationRequiresAuth() throws Exception {
        SimulationRequest request = new SimulationRequest();
        request.setJsonataExpression("payload.name");
        request.setMockInputContext(Map.of("payload", Map.of("name", "test")));

        mockMvc.perform(post("/api/v1/admin/simulation/transform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
