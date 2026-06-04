package com.rc.notification.interfaces.admin;

import com.rc.notification.application.admin.EventTypeAdminService;
import com.rc.notification.interfaces.admin.dto.EventTypeCreateRequest;
import com.rc.notification.interfaces.admin.dto.EventTypeDto;
import com.rc.notification.interfaces.admin.dto.EventTypeUpdateRequest;
import com.rc.notification.interfaces.admin.dto.PageResult;
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
 * 事件类型管理端点
 */
@Tag(name = "事件类型管理", description = "事件类型 CRUD API")
@RestController
@RequestMapping("/api/v1/admin/event-types")
public class EventTypeController {

    private final EventTypeAdminService adminService;

    public EventTypeController(EventTypeAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "分页查询事件类型列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public PageResult<EventTypeDto> listEventTypes(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "发布方编码") @RequestParam(required = false) String publisherCode,
            @Parameter(description = "状态筛选: DRAFT / ACTIVE / DEPRECATED") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return adminService.listEventTypes(keyword, publisherCode, status, page, size);
    }

    @Operation(summary = "查询单个事件类型")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public EventTypeDto getEventType(@Parameter(description = "事件类型ID") @PathVariable Long id) {
        return adminService.getEventType(id);
    }

    @Operation(summary = "新增事件类型")
    @ApiResponse(responseCode = "200", description = "新增成功")
    @PostMapping
    public EventTypeDto createEventType(@Valid @RequestBody EventTypeCreateRequest request) {
        return adminService.createEventType(request);
    }

    @Operation(summary = "更新事件类型")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    public EventTypeDto updateEventType(@Parameter(description = "事件类型ID") @PathVariable Long id,
                                        @RequestBody EventTypeUpdateRequest request) {
        return adminService.updateEventType(id, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("code", 400, "message", ex.getMessage()));
    }
}
