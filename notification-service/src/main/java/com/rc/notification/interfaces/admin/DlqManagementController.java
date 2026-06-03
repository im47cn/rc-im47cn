package com.rc.notification.interfaces.admin;

import com.rc.notification.application.dlq.DlqManagementService;
import com.rc.notification.interfaces.admin.dto.BatchResult;
import com.rc.notification.interfaces.admin.dto.BatchRetryRequest;
import com.rc.notification.interfaces.admin.dto.DlqLogDto;
import com.rc.notification.interfaces.admin.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 死信队列管理端点
 * <p>
 * 为运营/技术人员提供死信队列的查询、重试、忽略等管理操作 API。
 */
@Tag(name = "死信队列管理", description = "死信队列的查询、重试、忽略等管理操作")
@RestController
@RequestMapping("/api/v1/admin/dlq")
public class DlqManagementController {

    private final DlqManagementService dlqManagementService;

    public DlqManagementController(DlqManagementService dlqManagementService) {
        this.dlqManagementService = dlqManagementService;
    }

    /**
     * 分页查询死信记录，支持按供应商和状态筛选
     */
    @Operation(summary = "分页查询死信记录", description = "支持按供应商和状态筛选")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public PageResult<DlqLogDto> listDlqLogs(
            @Parameter(description = "供应商编码") @RequestParam(required = false) String supplierCode,
            @Parameter(description = "死信状态") @RequestParam(required = false) Integer dlqStatus,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return dlqManagementService.listDlqLogs(supplierCode, dlqStatus, page, size);
    }

    /**
     * 单条死信重试
     * <p>
     * 将 Redis 状态重置为 PROCESSING，重新压入 Redisson 队列，更新 updated_by
     */
    @Operation(summary = "单条死信重试", description = "将状态重置为 PROCESSING，重新压入队列")
    @ApiResponse(responseCode = "200", description = "重试成功")
    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retryDlqRecord(
            @Parameter(description = "死信记录ID") @PathVariable Long id,
            @Parameter(description = "操作人") @RequestHeader("X-Operator") String operator) {
        dlqManagementService.retryDlqRecord(id, operator);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量重试
     * <p>
     * 按供应商或 ID 列表批量执行重试，返回成功/失败计数
     */
    @Operation(summary = "批量重试", description = "按供应商或 ID 列表批量执行重试")
    @ApiResponse(responseCode = "200", description = "批量重试结果")
    @PostMapping("/batch-retry")
    public ResponseEntity<BatchResult> batchRetryDlqRecords(@RequestBody BatchRetryRequest request) {
        BatchResult result = dlqManagementService.batchRetryDlqRecords(
                request.getIds(), request.getSupplierCode(), request.getOperator());
        return ResponseEntity.ok(result);
    }

    /**
     * 标记忽略
     * <p>
     * 将 dlq_status 置为 2(已主动忽略)，记录操作人
     */
    @Operation(summary = "标记忽略", description = "将 dlq_status 置为已主动忽略")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @PostMapping("/{id}/ignore")
    public ResponseEntity<Void> ignoreDlqRecord(
            @Parameter(description = "死信记录ID") @PathVariable Long id,
            @Parameter(description = "操作人") @RequestHeader("X-Operator") String operator) {
        dlqManagementService.ignoreDlqRecord(id, operator);
        return ResponseEntity.ok().build();
    }
}
