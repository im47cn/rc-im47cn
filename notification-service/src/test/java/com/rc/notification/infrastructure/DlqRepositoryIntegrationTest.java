package com.rc.notification.infrastructure;

import com.rc.notification.IntegrationTestBase;
import com.rc.notification.infrastructure.persistence.entity.NotificationDlqLogEntity;
import org.junit.jupiter.api.Tag;
import com.rc.notification.infrastructure.persistence.mapper.NotificationDlqLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DLQ repository integration tests with real MySQL container.
 * <p>
 * Tests insert, unique constraint on biz_sign, and status update.
 */
@Tag("docker")
class DlqRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private NotificationDlqLogMapper dlqLogMapper;

    @Test
    @DisplayName("Insert DLQ record and retrieve by id")
    void insertAndRetrieve() {
        NotificationDlqLogEntity entity = buildEntity("tc-dlq-insert-001", "TC_SUP");

        dlqLogMapper.insert(entity);

        assertNotNull(entity.getId());

        NotificationDlqLogEntity retrieved = dlqLogMapper.selectById(entity.getId());
        assertNotNull(retrieved);
        assertEquals("tc-dlq-insert-001", retrieved.getBizSign());
        assertEquals("TC_SUP", retrieved.getSupplierCode());
        assertEquals(0, retrieved.getDlqStatus());
    }

    @Test
    @DisplayName("Unique constraint on biz_sign prevents duplicate insert")
    void uniqueConstraintOnBizSign() {
        NotificationDlqLogEntity entity1 = buildEntity("tc-dlq-unique-001", "TC_SUP");
        dlqLogMapper.insert(entity1);

        NotificationDlqLogEntity entity2 = buildEntity("tc-dlq-unique-001", "TC_SUP");

        assertThrows(DuplicateKeyException.class, () -> dlqLogMapper.insert(entity2));
    }

    @Test
    @DisplayName("Update DLQ status works correctly")
    void updateStatus() {
        NotificationDlqLogEntity entity = buildEntity("tc-dlq-update-001", "TC_SUP");
        dlqLogMapper.insert(entity);

        entity.setDlqStatus(1);
        entity.setUpdatedBy("test-operator");
        dlqLogMapper.updateById(entity);

        NotificationDlqLogEntity updated = dlqLogMapper.selectById(entity.getId());
        assertEquals(1, updated.getDlqStatus());
        assertEquals("test-operator", updated.getUpdatedBy());
    }

    private NotificationDlqLogEntity buildEntity(String bizSign, String supplierCode) {
        NotificationDlqLogEntity entity = new NotificationDlqLogEntity();
        entity.setBizSign(bizSign);
        entity.setTraceId("T-" + bizSign);
        entity.setSupplierCode(supplierCode);
        entity.setUnifiedContext("{\"eventId\":\"" + bizSign + "\"}");
        entity.setErrorMsg("Test error: max retry exhausted");
        entity.setRetryCount(3);
        entity.setDlqStatus(0);
        return entity;
    }
}
