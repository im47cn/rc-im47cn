package com.rc.notification.interfaces.admin;

import com.rc.notification.application.admin.SupplierConfigAdminService;
import com.rc.notification.interfaces.admin.dto.PageResult;
import com.rc.notification.interfaces.admin.dto.SupplierConfigCreateRequest;
import com.rc.notification.interfaces.admin.dto.SupplierConfigDto;
import com.rc.notification.interfaces.admin.dto.SupplierConfigUpdateRequest;
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
    @GetMapping
    public PageResult<SupplierConfigDto> listSuppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listSuppliers(keyword, status, page, size);
    }

    /**
     * 查询单个供应商完整配置（credentials_encrypted 字段脱敏返回）
     */
    @GetMapping("/{id}")
    public SupplierConfigDto getSupplier(@PathVariable Long id) {
        return adminService.getSupplier(id);
    }

    /**
     * 新增供应商
     */
    @PostMapping
    public SupplierConfigDto createSupplier(@Valid @RequestBody SupplierConfigCreateRequest request) {
        return adminService.createSupplier(request);
    }

    /**
     * 修改供应商配置
     */
    @PutMapping("/{id}")
    public SupplierConfigDto updateSupplier(@PathVariable Long id,
                                             @Valid @RequestBody SupplierConfigUpdateRequest request) {
        return adminService.updateSupplier(id, request);
    }

    /**
     * 启用/禁用供应商
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleSupplierStatus(@PathVariable Long id,
                                                      @RequestParam int status) {
        adminService.toggleSupplierStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
