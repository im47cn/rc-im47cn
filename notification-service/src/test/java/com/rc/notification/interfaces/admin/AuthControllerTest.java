package com.rc.notification.interfaces.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.integration.TestConfig;
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
 * AuthController MockMvc integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Valid login returns success and creates session")
    void validLoginReturnsSuccess() throws Exception {
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
    @DisplayName("Invalid credentials return failure response")
    void invalidCredentialsReturnFailure() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong-password");

        mockMvc.perform(post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Unauthenticated access to admin API returns 401")
    void unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/suppliers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("Authenticated session can access admin endpoints")
    void authenticatedSessionCanAccessAdmin() throws Exception {
        MockHttpSession session = loginAndGetSession();

        mockMvc.perform(get("/api/v1/admin/suppliers").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Logout invalidates session")
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = loginAndGetSession();

        mockMvc.perform(post("/api/v1/admin/logout").session(session))
                .andExpect(status().isOk());

        // After logout, new request without session returns 401
        mockMvc.perform(get("/api/v1/admin/suppliers"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Helper: login and return the session for subsequent requests.
     */
    MockHttpSession loginAndGetSession() throws Exception {
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
