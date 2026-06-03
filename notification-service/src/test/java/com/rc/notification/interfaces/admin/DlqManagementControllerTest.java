package com.rc.notification.interfaces.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.integration.TestConfig;
import com.rc.notification.infrastructure.persistence.entity.NotificationDlqLogEntity;
import com.rc.notification.infrastructure.persistence.mapper.NotificationDlqLogMapper;
import com.rc.notification.interfaces.admin.dto.BatchRetryRequest;
import com.rc.notification.interfaces.admin.dto.LoginRequest;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DlqManagementController MockMvc integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class DlqManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationDlqLogMapper dlqLogMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        session = loginAndGetSession();
    }

    @Test
    @DisplayName("List DLQ records with supplier filter")
    void listWithSupplierFilter() throws Exception {
        insertDlqRecord("tc-dlq-f1", "TC_SUP_A", 0);
        insertDlqRecord("tc-dlq-f2", "TC_SUP_A", 0);
        insertDlqRecord("tc-dlq-f3", "TC_SUP_B", 0);

        mockMvc.perform(get("/api/v1/admin/dlq")
                        .session(session)
                        .param("supplierCode", "TC_SUP_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(2)));
    }

    @Test
    @DisplayName("List DLQ records with status filter")
    void listWithStatusFilter() throws Exception {
        insertDlqRecord("tc-dlq-s1", "TC_SUP_SF", 0);
        insertDlqRecord("tc-dlq-s2", "TC_SUP_SF", 1);

        mockMvc.perform(get("/api/v1/admin/dlq")
                        .session(session)
                        .param("supplierCode", "TC_SUP_SF")
                        .param("dlqStatus", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(1)));
    }

    @Test
    @DisplayName("Single retry resets status and updates operator")
    void singleRetry() throws Exception {
        Long id = insertDlqRecord("tc-dlq-retry-1", "TC_SUP_RETRY", 0);

        mockMvc.perform(post("/api/v1/admin/dlq/{id}/retry", id)
                        .session(session)
                        .header("X-Operator", "tc-admin"))
                .andExpect(status().isOk());

        // Verify DB updated
        NotificationDlqLogEntity entity = dlqLogMapper.selectById(id);
        assert entity.getDlqStatus() == 1;
        assert "tc-admin".equals(entity.getUpdatedBy());
    }

    @Test
    @DisplayName("Batch retry returns success/failure counts")
    void batchRetry() throws Exception {
        Long id1 = insertDlqRecord("tc-dlq-batch-1", "TC_SUP_BATCH", 0);
        Long id2 = insertDlqRecord("tc-dlq-batch-2", "TC_SUP_BATCH", 0);
        Long id3 = insertDlqRecord("tc-dlq-batch-3", "TC_SUP_BATCH", 1); // non-pending

        BatchRetryRequest request = new BatchRetryRequest();
        request.setIds(List.of(id1, id2, id3));
        request.setOperator("batch-tc-admin");

        mockMvc.perform(post("/api/v1/admin/dlq/batch-retry")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(1));
    }

    @Test
    @DisplayName("Ignore sets dlq_status=2 and records operator")
    void ignoreDlqRecord() throws Exception {
        Long id = insertDlqRecord("tc-dlq-ignore-1", "TC_SUP_IGN", 0);

        mockMvc.perform(post("/api/v1/admin/dlq/{id}/ignore", id)
                        .session(session)
                        .header("X-Operator", "ignore-tc-admin"))
                .andExpect(status().isOk());

        NotificationDlqLogEntity entity = dlqLogMapper.selectById(id);
        assert entity.getDlqStatus() == 2;
        assert "ignore-tc-admin".equals(entity.getUpdatedBy());
    }

    private Long insertDlqRecord(String bizSign, String supplierCode, int dlqStatus) {
        NotificationDlqLogEntity entity = new NotificationDlqLogEntity();
        entity.setBizSign(bizSign);
        entity.setTraceId("T-" + bizSign);
        entity.setSupplierCode(supplierCode);
        entity.setUnifiedContext("{\"eventId\":\"" + bizSign + "\",\"supplierCode\":\"" + supplierCode + "\"}");
        entity.setErrorMsg("Test error");
        entity.setRetryCount(3);
        entity.setDlqStatus(dlqStatus);
        dlqLogMapper.insert(entity);
        return entity.getId();
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
