package com.rc.notification.interfaces.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.integration.TestConfig;
import com.rc.notification.interfaces.admin.dto.LoginRequest;
import com.rc.notification.interfaces.admin.dto.SupplierConfigCreateRequest;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SupplierConfigController MockMvc integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class SupplierConfigControllerTest {

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
    @DisplayName("List suppliers with pagination returns records")
    void listSuppliersWithPagination() throws Exception {
        createSupplier("TC_LIST_A", "Supplier A");
        createSupplier("TC_LIST_B", "Supplier B");

        mockMvc.perform(get("/api/v1/admin/suppliers")
                        .session(session)
                        .param("keyword", "TC_LIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Create supplier returns created supplier with id")
    void createSupplierReturnsCreated() throws Exception {
        SupplierConfigCreateRequest req = buildCreateRequest("TC_CREATE_OK", "Create Test");

        mockMvc.perform(post("/api/v1/admin/suppliers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierCode").value("TC_CREATE_OK"))
                .andExpect(jsonPath("$.supplierName").value("Create Test"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Duplicate supplier_code throws error")
    void duplicateSupplierCodeFails() throws Exception {
        createSupplier("TC_DUP_CODE", "First");

        SupplierConfigCreateRequest req2 = buildCreateRequest("TC_DUP_CODE", "Second");

        Exception ex = assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/v1/admin/suppliers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2))));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    @DisplayName("Get supplier returns credential keys (masked)")
    void getSupplierReturnsCredentialKeys() throws Exception {
        SupplierConfigCreateRequest req = buildCreateRequest("TC_CRED_MASK", "Credential Test");
        req.setCredentials(Map.of("appKey", "test-key", "appSecret", "secret-value"));

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/suppliers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long supplierId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/v1/admin/suppliers/{id}", supplierId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierCode").value("TC_CRED_MASK"))
                .andExpect(jsonPath("$.credentialKeys").isArray());
    }

    private void createSupplier(String code, String name) throws Exception {
        SupplierConfigCreateRequest req = buildCreateRequest(code, name);
        mockMvc.perform(post("/api/v1/admin/suppliers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private SupplierConfigCreateRequest buildCreateRequest(String code, String name) {
        SupplierConfigCreateRequest req = new SupplierConfigCreateRequest();
        req.setSupplierCode(code);
        req.setSupplierName(name);
        req.setBaseUrl("https://api.test.com");
        req.setHttpMethod("POST");
        req.setBodyTemplate("'{\"test\": true}'");
        req.setStatus(1);
        req.setMaxRetryCount(3);
        req.setRetryBackoffInitialMs(1000);
        req.setRetryBackoffMultiplier(new BigDecimal("2.00"));
        req.setRetryBackoffMaxMs(30000);
        req.setWorkerConcurrency(1);
        return req;
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
