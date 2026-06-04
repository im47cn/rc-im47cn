package com.rc.notification.interfaces.admin;

import com.rc.notification.application.admin.PublisherAdminService;
import com.rc.notification.interfaces.admin.dto.PageResult;
import com.rc.notification.interfaces.admin.dto.PublisherCreateRequest;
import com.rc.notification.interfaces.admin.dto.PublisherDto;
import com.rc.notification.interfaces.admin.dto.PublisherUpdateRequest;
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
 * 发布方管理端点
 */
@Tag(name = "发布方管理", description = "发布方 CRUD API")
@RestController
@RequestMapping("/api/v1/admin/publishers")
public class PublisherController {

    private final PublisherAdminService adminService;

    public PublisherController(PublisherAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "分页查询发布方列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public PageResult<PublisherDto> listPublishers(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选: 0-禁用, 1-启用") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return adminService.listPublishers(keyword, status, page, size);
    }

    @Operation(summary = "查询单个发布方")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public PublisherDto getPublisher(@Parameter(description = "发布方ID") @PathVariable Long id) {
        return adminService.getPublisher(id);
    }

    @Operation(summary = "新增发布方")
    @ApiResponse(responseCode = "200", description = "新增成功")
    @PostMapping
    public PublisherDto createPublisher(@Valid @RequestBody PublisherCreateRequest request) {
        return adminService.createPublisher(request);
    }

    @Operation(summary = "更新发布方")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    public PublisherDto updatePublisher(@Parameter(description = "发布方ID") @PathVariable Long id,
                                        @RequestBody PublisherUpdateRequest request) {
        return adminService.updatePublisher(id, request);
    }

    @Operation(summary = "轮换 API Key")
    @ApiResponse(responseCode = "200", description = "轮换成功")
    @PostMapping("/{id}/rotate-key")
    public PublisherDto rotateApiKey(@Parameter(description = "发布方ID") @PathVariable Long id) {
        return adminService.rotateApiKey(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("code", 400, "message", ex.getMessage()));
    }
}
