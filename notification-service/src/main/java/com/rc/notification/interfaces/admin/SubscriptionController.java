package com.rc.notification.interfaces.admin;

import com.rc.notification.application.admin.SubscriptionAdminService;
import com.rc.notification.interfaces.admin.dto.PageResult;
import com.rc.notification.interfaces.admin.dto.SubscriptionCreateRequest;
import com.rc.notification.interfaces.admin.dto.SubscriptionDto;
import com.rc.notification.interfaces.admin.dto.SubscriptionUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 订阅关系管理端点
 */
@Tag(name = "订阅关系管理", description = "订阅关系 CRUD API")
@RestController
@RequestMapping("/api/v1/admin/subscriptions")
public class SubscriptionController {

    private final SubscriptionAdminService adminService;

    public SubscriptionController(SubscriptionAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "分页查询订阅列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public PageResult<SubscriptionDto> listSubscriptions(
            @Parameter(description = "订阅方编码") @RequestParam(required = false) String subscriberCode,
            @Parameter(description = "事件类型编码") @RequestParam(required = false) String eventTypeCode,
            @Parameter(description = "状态筛选: ACTIVE / SUSPENDED") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return adminService.listSubscriptions(subscriberCode, eventTypeCode, status, page, size);
    }

    @Operation(summary = "查询单个订阅")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public SubscriptionDto getSubscription(@Parameter(description = "订阅ID") @PathVariable Long id) {
        return adminService.getSubscription(id);
    }

    @Operation(summary = "新增订阅")
    @ApiResponse(responseCode = "200", description = "新增成功")
    @PostMapping
    public SubscriptionDto createSubscription(@Valid @RequestBody SubscriptionCreateRequest request) {
        return adminService.createSubscription(request);
    }

    @Operation(summary = "更新订阅")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    public SubscriptionDto updateSubscription(@Parameter(description = "订阅ID") @PathVariable Long id,
                                              @RequestBody SubscriptionUpdateRequest request) {
        return adminService.updateSubscription(id, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("code", 400, "message", ex.getMessage()));
    }
}
