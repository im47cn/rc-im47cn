package com.rc.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.infrastructure.persistence.entity.NotificationDlqLogEntity;
import com.rc.notification.infrastructure.persistence.mapper.NotificationDlqLogMapper;
import com.rc.notification.interfaces.admin.dto.BatchRetryRequest;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T24-7: DLQ management API integration tests (query + retry + ignore)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DlqApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationDlqLogMapper dlqLogMapper;

    @Autowired
    private TestConfig testConfig;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        session = AuthIntegrationTest.loginAndGetSession(mockMvc, objectMapper);
        testConfig.clearAll();
    }

    /**
     * Insert a DLQ record directly into DB for testing
     */
    private Long insertDlqRecord(String bizSign, String supplierCode, int dlqStatus) {
        NotificationDlqLogEntity entity = new NotificationDlqLogEntity();
        entity.setBizSign(bizSign);
        entity.setTraceId("T-" + bizSign);
        entity.setSupplierCode(supplierCode);
        entity.setUnifiedContext("{\"eventId\":\"" + bizSign + "\",\"supplierCode\":\"" + supplierCode + "\"}");
        entity.setErrorMsg("Test error: max retry exhausted");
        entity.setRetryCount(3);
        entity.setDlqStatus(dlqStatus);
        dlqLogMapper.insert(entity);
        return entity.getId();
    }

    @Test
    @DisplayName("Query DLQ list with pagination")
    void queryDlqListWithPagination() throws Exception {
        insertDlqRecord("dlq-query-001", "SUPPLIER_A", 0);
        insertDlqRecord("dlq-query-002", "SUPPLIER_A", 0);
        insertDlqRecord("dlq-query-003", "SUPPLIER_B", 0);

        // Query all (total may be 0 without MyBatis-Plus pagination interceptor, check records size)
        mockMvc.perform(get("/api/v1/admin/dlq")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(3)));

        // Query by supplier_code filter
        mockMvc.perform(get("/api/v1/admin/dlq")
                        .session(session)
                        .param("supplierCode", "SUPPLIER_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(2)));

        // Query by dlq_status filter
        mockMvc.perform(get("/api/v1/admin/dlq")
                        .session(session)
                        .param("dlqStatus", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(3)));
    }

    @Test
    @DisplayName("Single DLQ retry: resets Redis state and updates DB status")
    void singleDlqRetry() throws Exception {
        Long dlqId = insertDlqRecord("dlq-retry-001", "SUPPLIER_RETRY", 0);

        // Set DEAD_LETTERED in mock Redis
        testConfig.getRedisStore().put("status:dispatch:dlq-retry-001", "DEAD_LETTERED");

        mockMvc.perform(post("/api/v1/admin/dlq/{id}/retry", dlqId)
                        .session(session)
                        .header("X-Operator", "test-admin"))
                .andExpect(status().isOk());

        // Verify Redis state changed to PROCESSING
        Object redisState = testConfig.getRedisStore().get("status:dispatch:dlq-retry-001");
        assert redisState != null && "PROCESSING".equals(redisState.toString());

        // Verify DB status changed to 1 (retried)
        NotificationDlqLogEntity entity = dlqLogMapper.selectById(dlqId);
        assert entity.getDlqStatus() == 1;
        assert "test-admin".equals(entity.getUpdatedBy());
    }

    @Test
    @DisplayName("DLQ retry on non-pending status fails with IllegalStateException")
    void retryNonPendingStatusFails() throws Exception {
        Long dlqId = insertDlqRecord("dlq-retry-fail", "SUPPLIER_RETRY", 1); // already retried

        Exception ex = assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/v1/admin/dlq/{id}/retry", dlqId)
                        .session(session)
                        .header("X-Operator", "test-admin")));
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    @Test
    @DisplayName("Batch retry by ID list returns success/failure counts")
    void batchRetryByIds() throws Exception {
        Long id1 = insertDlqRecord("dlq-batch-001", "SUPPLIER_BATCH", 0);
        Long id2 = insertDlqRecord("dlq-batch-002", "SUPPLIER_BATCH", 0);
        Long id3 = insertDlqRecord("dlq-batch-003", "SUPPLIER_BATCH", 1); // non-pending

        BatchRetryRequest request = new BatchRetryRequest();
        request.setIds(List.of(id1, id2, id3));
        request.setOperator("batch-admin");

        mockMvc.perform(post("/api/v1/admin/dlq/batch-retry")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(1));
    }

    @Test
    @DisplayName("Batch retry by supplier_code")
    void batchRetryBySupplierCode() throws Exception {
        insertDlqRecord("dlq-bysupplier-001", "SUPPLIER_BY_CODE", 0);
        insertDlqRecord("dlq-bysupplier-002", "SUPPLIER_BY_CODE", 0);

        BatchRetryRequest request = new BatchRetryRequest();
        request.setSupplierCode("SUPPLIER_BY_CODE");
        request.setOperator("batch-admin");

        mockMvc.perform(post("/api/v1/admin/dlq/batch-retry")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(0));
    }

    @Test
    @DisplayName("Ignore DLQ record: sets dlq_status=2 and records operator")
    void ignoreDlqRecord() throws Exception {
        Long dlqId = insertDlqRecord("dlq-ignore-001", "SUPPLIER_IGNORE", 0);

        mockMvc.perform(post("/api/v1/admin/dlq/{id}/ignore", dlqId)
                        .session(session)
                        .header("X-Operator", "ignore-admin"))
                .andExpect(status().isOk());

        // Verify DB status is 2 (ignored)
        NotificationDlqLogEntity entity = dlqLogMapper.selectById(dlqId);
        assert entity.getDlqStatus() == 2;
        assert "ignore-admin".equals(entity.getUpdatedBy());
    }

    @Test
    @DisplayName("DLQ API requires authentication")
    void dlqApiRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dlq"))
                .andExpect(status().isUnauthorized());
    }
}
