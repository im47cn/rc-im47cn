package com.rc.notification.interfaces.admin;

import com.rc.notification.domain.detection.FieldFingerprint;
import com.rc.notification.domain.detection.FieldFingerprintRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字段指纹管理端点
 */
@Tag(name = "字段指纹", description = "字段指纹查询 API")
@RestController
@RequestMapping("/api/v1/admin/field-fingerprints")
public class FieldFingerprintController {

    private final FieldFingerprintRepository fingerprintRepo;

    public FieldFingerprintController(FieldFingerprintRepository fingerprintRepo) {
        this.fingerprintRepo = fingerprintRepo;
    }

    @Operation(summary = "按事件类型查询字段指纹")
    @GetMapping
    public List<FieldFingerprint> listFingerprints(
            @Parameter(description = "事件类型编码") @RequestParam String eventTypeCode) {
        return fingerprintRepo.findByEventTypeCode(eventTypeCode);
    }
}
