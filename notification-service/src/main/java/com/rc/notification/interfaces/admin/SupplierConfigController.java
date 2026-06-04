package com.rc.notification.interfaces.admin;

import com.rc.notification.application.admin.SupplierConfigAdminService;
import com.rc.notification.interfaces.admin.dto.PageResult;
import com.rc.notification.interfaces.admin.dto.SupplierConfigCreateRequest;
import com.rc.notification.interfaces.admin.dto.SupplierConfigDto;
import com.rc.notification.interfaces.admin.dto.SupplierConfigUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供应商配置管理端点
 * <p>
 * 为管理后台提供供应商配置的完整 CRUD API。
 * 新增/修改操作完成后自动广播 Redis Pub/Sub 驱逐事件，
 * 触发缓存刷新与 Worker 热加载。
 */
@Tag(name = "供应商配置", description = "供应商配置管理 CRUD API")
@RestController
@RequestMapping("/api/v1/admin/suppliers")
public class SupplierConfigController {

    private final SupplierConfigAdminService adminService;

    public SupplierConfigController(SupplierConfigAdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 分页查询供应商列表，支持按名称/编码模糊搜索和状态筛选
     */
    @Operation(summary = "分页查询供应商列表", description = "支持按名称/编码模糊搜索和状态筛选")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public PageResult<SupplierConfigDto> listSuppliers(
            @Parameter(description = "搜索关键词（名称/编码）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选: 0-禁用, 1-启用") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        return adminService.listSuppliers(keyword, status, page, size);
    }

    /**
     * 查询单个供应商完整配置（credentials_encrypted 字段脱敏返回）
     */
    @Operation(summary = "查询单个供应商配置", description = "credentials_encrypted 字段脱敏返回")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/{id}")
    public SupplierConfigDto getSupplier(@Parameter(description = "供应商ID") @PathVariable Long id) {
        return adminService.getSupplier(id);
    }

    /**
     * 按供应商编码查询配置
     */
    @Operation(summary = "按编码查询供应商配置")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/by-code/{code}")
    public SupplierConfigDto getSupplierByCode(@Parameter(description = "供应商编码") @PathVariable String code) {
        return adminService.getSupplierByCode(code);
    }

    /**
     * 新增供应商
     */
    @Operation(summary = "新增供应商")
    @ApiResponse(responseCode = "200", description = "新增成功")
    @PostMapping
    public SupplierConfigDto createSupplier(@Valid @RequestBody SupplierConfigCreateRequest request) {
        return adminService.createSupplier(request);
    }

    /**
     * 修改供应商配置
     */
    @Operation(summary = "修改供应商配置")
    @ApiResponse(responseCode = "200", description = "修改成功")
    @PutMapping("/{id}")
    public SupplierConfigDto updateSupplier(@Parameter(description = "供应商ID") @PathVariable Long id,
                                             @Valid @RequestBody SupplierConfigUpdateRequest request) {
        return adminService.updateSupplier(id, request);
    }

    /**
     * 启用/禁用供应商
     */
    @Operation(summary = "启用/禁用供应商")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleSupplierStatus(
            @Parameter(description = "供应商ID") @PathVariable Long id,
            @Parameter(description = "状态: 0-禁用, 1-启用") @RequestParam int status) {
        adminService.toggleSupplierStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
