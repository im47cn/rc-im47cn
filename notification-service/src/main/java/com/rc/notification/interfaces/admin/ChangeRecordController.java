package com.rc.notification.interfaces.admin;

import com.rc.notification.domain.detection.ChangeRecord;
import com.rc.notification.domain.detection.ChangeRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 变更记录管理端点
 */
@Tag(name = "变更记录", description = "变更记录查询与审核 API")
@RestController
@RequestMapping("/api/v1/admin/change-records")
public class ChangeRecordController {

    private final ChangeRecordRepository changeRecordRepo;

    public ChangeRecordController(ChangeRecordRepository changeRecordRepo) {
        this.changeRecordRepo = changeRecordRepo;
    }

    @Operation(summary = "查询变更记录")
    @GetMapping
    public List<ChangeRecord> listChangeRecords(
            @Parameter(description = "事件类型编码") @RequestParam(required = false) String eventTypeCode,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {
        if (eventTypeCode != null && !eventTypeCode.isBlank()) {
            return changeRecordRepo.findByEventTypeCode(eventTypeCode);
        }
        if (status != null && !status.isBlank()) {
            return changeRecordRepo.findByStatus(status);
        }
        // 默认返回待审核记录
        return changeRecordRepo.findByStatus("PENDING_REVIEW");
    }

    @Operation(summary = "确认变更记录")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirmChangeRecord(
            @Parameter(description = "变更记录ID") @PathVariable Long id) {
        ChangeRecord record = changeRecordRepo.findById(id);
        if (record == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "变更记录不存在"));
        }
        record.setStatus("CONFIRMED");
        changeRecordRepo.update(record);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已确认"));
    }

    @Operation(summary = "驳回变更记录")
    @PutMapping("/{id}/dismiss")
    public ResponseEntity<Map<String, Object>> dismissChangeRecord(
            @Parameter(description = "变更记录ID") @PathVariable Long id) {
        ChangeRecord record = changeRecordRepo.findById(id);
        if (record == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "变更记录不存在"));
        }
        record.setStatus("DISMISSED");
        changeRecordRepo.update(record);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已驳回"));
    }
}
