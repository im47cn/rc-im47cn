package com.rc.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.interfaces.admin.dto.SupplierConfigCreateRequest;
import com.rc.notification.interfaces.admin.dto.SupplierConfigUpdateRequest;
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
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T24-5: Supplier CRUD API integration tests (Admin API full lifecycle)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SupplierCrudIntegrationTest {

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
    @DisplayName("Create supplier -> list -> get -> update -> toggle status: full CRUD lifecycle")
    void supplierCrudFullLifecycle() throws Exception {
        // 1. Create supplier
        SupplierConfigCreateRequest createReq = buildCreateRequest("CRUD_TEST", "CRUD Test Supplier");

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/suppliers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierCode").value("CRUD_TEST"))
                .andExpect(jsonPath("$.supplierName").value("CRUD Test Supplier"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        // Extract created supplier ID
        String responseJson = createResult.getResponse().getContentAsString();
        Long supplierId = objectMapper.readTree(responseJson).get("id").asLong();

        // 2. List suppliers - should contain the new one
        mockMvc.perform(get("/api/v1/admin/suppliers")
                        .session(session)
                        .param("keyword", "CRUD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.records[0].supplierCode").value("CRUD_TEST"));

        // 3. Get single supplier - credentials should be desensitized
        mockMvc.perform(get("/api/v1/admin/suppliers/{id}", supplierId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierCode").value("CRUD_TEST"))
                .andExpect(jsonPath("$.credentialKeys").isArray());

        // 4. Update supplier
        SupplierConfigUpdateRequest updateReq = new SupplierConfigUpdateRequest();
        updateReq.setSupplierName("Updated CRUD Supplier");
        updateReq.setBaseUrl("https://updated.example.com");
        updateReq.setBodyTemplate("'{\"updated\": true}'");

        mockMvc.perform(put("/api/v1/admin/suppliers/{id}", supplierId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierName").value("Updated CRUD Supplier"));

        // 5. Toggle status to disabled
        mockMvc.perform(patch("/api/v1/admin/suppliers/{id}/status", supplierId)
                        .session(session)
                        .param("status", "0"))
                .andExpect(status().isOk());

        // Verify disabled
        mockMvc.perform(get("/api/v1/admin/suppliers/{id}", supplierId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0));

        // 6. Toggle back to enabled
        mockMvc.perform(patch("/api/v1/admin/suppliers/{id}/status", supplierId)
                        .session(session)
                        .param("status", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Create duplicate supplier_code returns error")
    void createDuplicateSupplierCodeFails() throws Exception {
        SupplierConfigCreateRequest req = buildCreateRequest("DUPLICATE_TEST", "First");

        // Create first - success
        mockMvc.perform(post("/api/v1/admin/suppliers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Create second with same code - should fail with IllegalArgumentException
        SupplierConfigCreateRequest req2 = buildCreateRequest("DUPLICATE_TEST", "Second");

        Exception ex = assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/v1/admin/suppliers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2))));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    @DisplayName("List suppliers with status filter works")
    void listSuppliersWithStatusFilter() throws Exception {
        // Create an enabled supplier
        SupplierConfigCreateRequest req = buildCreateRequest("STATUS_FILTER_TEST", "Status Filter");
        req.setStatus(1);

        mockMvc.perform(post("/api/v1/admin/suppliers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Filter by status=1 should include it
        mockMvc.perform(get("/api/v1/admin/suppliers")
                        .session(session)
                        .param("status", "1")
                        .param("keyword", "STATUS_FILTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(1)));

        // Filter by status=0 should not include it
        mockMvc.perform(get("/api/v1/admin/suppliers")
                        .session(session)
                        .param("status", "0")
                        .param("keyword", "STATUS_FILTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(0)));
    }

    @Test
    @DisplayName("Get non-existent supplier throws IllegalArgumentException")
    void getNonExistentSupplierFails() throws Exception {
        Exception ex = assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/v1/admin/suppliers/{id}", 99999L)
                        .session(session)));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
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
        req.setCredentials(Map.of("appKey", "test-key", "appSecret", "test-secret"));
        return req;
    }
}
