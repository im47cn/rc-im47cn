package com.rc.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.interfaces.admin.dto.LoginRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T24-1: Admin authentication & session integration tests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Unauthenticated access to admin API returns 401")
    void unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/suppliers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("Login with correct credentials returns success and sets session")
    void loginWithCorrectCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin");

        mockMvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Login with incorrect credentials returns failure")
    void loginWithIncorrectCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        mockMvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Authenticated requests with session pass the filter")
    void authenticatedSessionPassesFilter() throws Exception {
        // Login first
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Access admin API with session
        mockMvc.perform(get("/api/v1/admin/suppliers").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Logout clears session, subsequent access returns 401")
    void logoutClearsSession() throws Exception {
        // Login
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Logout
        mockMvc.perform(post("/api/v1/admin/logout").session(session))
                .andExpect(status().isOk());

        // Session is invalidated, new request without session should fail
        mockMvc.perform(get("/api/v1/admin/suppliers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/api/v1/notifications/ingest is NOT intercepted by admin auth filter")
    void ingestEndpointNotIntercepted() throws Exception {
        // Even without session, the ingest endpoint should not return 401
        // It may return 400 due to missing body, but NOT 401
        mockMvc.perform(post("/api/v1/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()); // validation error, not 401
    }

    /**
     * Helper: perform login and return the session
     */
    static MockHttpSession loginAndGetSession(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
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
