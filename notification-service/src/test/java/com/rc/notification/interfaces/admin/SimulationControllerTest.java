package com.rc.notification.interfaces.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.integration.TestConfig;
import com.rc.notification.interfaces.admin.dto.FullPreviewRequest;
import com.rc.notification.interfaces.admin.dto.LoginRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SimulationController MockMvc integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        session = loginAndGetSession();
    }

    @Test
    @DisplayName("Valid JSONata transform returns correct result")
    void validTransformReturnsResult() throws Exception {
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
    @DisplayName("Syntax error returns error with details")
    void syntaxErrorReturnsErrorWithOffset() throws Exception {
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
    @DisplayName("Full preview returns complete HTTP request preview")
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
                .andExpect(jsonPath("$.body").exists());
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
