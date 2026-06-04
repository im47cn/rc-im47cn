package com.rc.notification.integration;

import com.rc.notification.application.detection.FieldSamplingService;
import com.rc.notification.domain.detection.ChangeRecord;
import com.rc.notification.domain.detection.ChangeRecordRepository;
import com.rc.notification.domain.detection.FieldFingerprint;
import com.rc.notification.domain.detection.FieldFingerprintRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FieldSamplingIntegrationTest {

    @Autowired
    private FieldSamplingService fieldSamplingService;

    @Autowired
    private FieldFingerprintRepository fingerprintRepo;

    @Autowired
    private ChangeRecordRepository changeRecordRepo;

    @Test
    @DisplayName("New field creates fingerprint and FIELD_ADDED change record")
    void newFieldCreatesFingerprint() {
        Map<String, Object> payload = Map.of(
                "orderId", "ORD-001",
                "amount", 100.5
        );

        // sampleAsync is @Async but in test context runs synchronously
        fieldSamplingService.sampleAsync("ORDER_CREATED", payload);

        // Verify fingerprints created
        List<FieldFingerprint> fps = fingerprintRepo.findByEventTypeCode("ORDER_CREATED");
        assertEquals(2, fps.size());

        // Verify change records
        List<ChangeRecord> crs = changeRecordRepo.findByEventTypeCode("ORDER_CREATED");
        assertEquals(2, crs.size());
        assertTrue(crs.stream().allMatch(cr -> "FIELD_ADDED".equals(cr.getChangeType())));
        assertTrue(crs.stream().allMatch(cr -> "RUNTIME_INFERRED".equals(cr.getDetectionSource())));
        assertTrue(crs.stream().allMatch(cr -> "PENDING_REVIEW".equals(cr.getStatus())));
    }

    @Test
    @DisplayName("Nested object fields are extracted recursively")
    void nestedFieldsExtracted() {
        Map<String, Object> payload = Map.of(
                "address", Map.of(
                        "city", "Shanghai",
                        "zip", "200000"
                )
        );

        fieldSamplingService.sampleAsync("ORDER_CREATED", payload);

        List<FieldFingerprint> fps = fingerprintRepo.findByEventTypeCode("ORDER_CREATED");
        // payload.address (OBJECT) + payload.address.city (STRING) + payload.address.zip (STRING) = 3
        assertEquals(3, fps.size());
    }

    @Test
    @DisplayName("Repeated sampling updates lastSeenAt and sampleCount, no duplicate change records")
    void repeatedSamplingUpdatesExisting() {
        Map<String, Object> payload = Map.of("orderId", "ORD-001");

        fieldSamplingService.sampleAsync("ORDER_CREATED", payload);
        fieldSamplingService.sampleAsync("ORDER_CREATED", payload);

        List<FieldFingerprint> fps = fingerprintRepo.findByEventTypeCode("ORDER_CREATED");
        assertEquals(1, fps.size());
        assertEquals(2, fps.get(0).getSampleCount());

        // Only 1 change record (from first sampling)
        List<ChangeRecord> crs = changeRecordRepo.findByEventTypeCode("ORDER_CREATED");
        assertEquals(1, crs.size());
    }

    @Test
    @DisplayName("Type change creates FIELD_TYPE_CHANGED change record")
    void typeChangeDetected() {
        // First: orderId is STRING
        fieldSamplingService.sampleAsync("ORDER_CREATED", Map.of("orderId", "ORD-001"));

        // Second: orderId becomes NUMBER
        fieldSamplingService.sampleAsync("ORDER_CREATED", Map.of("orderId", 12345));

        List<ChangeRecord> crs = changeRecordRepo.findByEventTypeCode("ORDER_CREATED");
        assertEquals(2, crs.size()); // FIELD_ADDED + FIELD_TYPE_CHANGED

        ChangeRecord typeChange = crs.stream()
                .filter(cr -> "FIELD_TYPE_CHANGED".equals(cr.getChangeType()))
                .findFirst()
                .orElseThrow();
        assertEquals("STRING", typeChange.getOldValue());
        assertEquals("NUMBER", typeChange.getNewValue());
        assertEquals("LOW", typeChange.getConfidence());
    }

    @Test
    @DisplayName("extractFieldPaths handles various types correctly")
    void extractFieldPathsTypes() {
        Map<String, String> paths = fieldSamplingService.extractFieldPaths("payload", Map.of(
                "str", "hello",
                "num", 42,
                "bool", true,
                "arr", List.of(1, 2, 3)
        ));

        assertEquals("STRING", paths.get("payload.str"));
        assertEquals("NUMBER", paths.get("payload.num"));
        assertEquals("BOOLEAN", paths.get("payload.bool"));
        assertEquals("ARRAY", paths.get("payload.arr"));
    }
}
