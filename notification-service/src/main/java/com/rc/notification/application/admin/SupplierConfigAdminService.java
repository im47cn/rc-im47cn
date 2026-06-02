package com.rc.notification.application.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rc.notification.domain.credential.CredentialVault;
import com.rc.notification.infrastructure.cache.ConfigEvictionListener;
import com.rc.notification.infrastructure.persistence.entity.SupplierConfigEntity;
import com.rc.notification.infrastructure.persistence.mapper.SupplierConfigMapper;
import com.rc.notification.interfaces.admin.dto.PageResult;
import com.rc.notification.interfaces.admin.dto.SupplierConfigCreateRequest;
import com.rc.notification.interfaces.admin.dto.SupplierConfigDto;
import com.rc.notification.interfaces.admin.dto.SupplierConfigUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 供应商配置管理服务
 * <p>
 * 提供 CRUD 全流程，新增/修改后广播 Pub/Sub 驱逐事件，
 * 触发缓存刷新与 Worker 热加载。
 */
@Service
public class SupplierConfigAdminService {

    private static final Logger log = LoggerFactory.getLogger(SupplierConfigAdminService.class);

    private final SupplierConfigMapper supplierConfigMapper;
    private final CredentialVault credentialVault;
    private final ConfigEvictionListener configEvictionListener;

    public SupplierConfigAdminService(SupplierConfigMapper supplierConfigMapper,
                                       CredentialVault credentialVault,
                                       ConfigEvictionListener configEvictionListener) {
        this.supplierConfigMapper = supplierConfigMapper;
        this.credentialVault = credentialVault;
        this.configEvictionListener = configEvictionListener;
    }

    /**
     * 分页查询供应商列表
     */
    public PageResult<SupplierConfigDto> listSuppliers(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<SupplierConfigEntity> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(SupplierConfigEntity::getSupplierCode, keyword)
                    .or()
                    .like(SupplierConfigEntity::getSupplierName, keyword));
        }
        if (status != null) {
            wrapper.eq(SupplierConfigEntity::getStatus, status);
        }
        wrapper.orderByDesc(SupplierConfigEntity::getUpdateTime);

        IPage<SupplierConfigEntity> pageResult = supplierConfigMapper.selectPage(
                new Page<>(page, size), wrapper);

        List<SupplierConfigDto> dtoList = pageResult.getRecords().stream()
                .map(this::toDto)
                .toList();

        return new PageResult<>(dtoList, pageResult.getTotal(), page, size);
    }

    /**
     * 查询单个供应商完整配置（凭证脱敏）
     */
    public SupplierConfigDto getSupplier(Long id) {
        SupplierConfigEntity entity = supplierConfigMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("供应商配置不存在: id=" + id);
        }
        return toDto(entity);
    }

    /**
     * 新增供应商
     */
    public SupplierConfigDto createSupplier(SupplierConfigCreateRequest request) {
        // 校验 supplier_code 唯一性
        LambdaQueryWrapper<SupplierConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierConfigEntity::getSupplierCode, request.getSupplierCode());
        if (supplierConfigMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("供应商编码已存在: " + request.getSupplierCode());
        }

        SupplierConfigEntity entity = new SupplierConfigEntity();
        copyFromCreateRequest(entity, request);

        // 加密凭证
        if (request.getCredentials() != null && !request.getCredentials().isEmpty()) {
            entity.setCredentialsEncrypted(credentialVault.encrypt(request.getCredentials()));
        }

        supplierConfigMapper.insert(entity);
        log.info("新增供应商配置: supplierCode={}", request.getSupplierCode());

        // 广播 Pub/Sub CREATE 事件
        configEvictionListener.publishEviction(request.getSupplierCode(), "CREATE");

        return toDto(entity);
    }

    /**
     * 修改供应商配置
     */
    public SupplierConfigDto updateSupplier(Long id, SupplierConfigUpdateRequest request) {
        SupplierConfigEntity entity = supplierConfigMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("供应商配置不存在: id=" + id);
        }

        copyFromUpdateRequest(entity, request);

        // 凭证处理：null 表示保留原值，非 null 表示重新加密
        if (request.getCredentials() != null) {
            entity.setCredentialsEncrypted(credentialVault.encrypt(request.getCredentials()));
        }

        supplierConfigMapper.updateById(entity);
        log.info("修改供应商配置: id={}, supplierCode={}", id, entity.getSupplierCode());

        // 广播 Pub/Sub UPDATE 事件
        configEvictionListener.publishEviction(entity.getSupplierCode(), "UPDATE");

        return toDto(entity);
    }

    /**
     * 启用/禁用供应商
     */
    public void toggleSupplierStatus(Long id, int status) {
        SupplierConfigEntity entity = supplierConfigMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("供应商配置不存在: id=" + id);
        }

        entity.setStatus(status);
        supplierConfigMapper.updateById(entity);
        log.info("切换供应商状态: id={}, supplierCode={}, status={}", id, entity.getSupplierCode(), status);

        // 广播状态变更事件
        String action = status == 1 ? "ENABLE" : "DISABLE";
        configEvictionListener.publishEviction(entity.getSupplierCode(), action);
    }

    /**
     * Entity 转 DTO（凭证脱敏）
     */
    private SupplierConfigDto toDto(SupplierConfigEntity entity) {
        SupplierConfigDto dto = new SupplierConfigDto();
        dto.setId(entity.getId());
        dto.setSupplierCode(entity.getSupplierCode());
        dto.setSupplierName(entity.getSupplierName());
        dto.setDescription(entity.getDescription());
        dto.setBaseUrl(entity.getBaseUrl());
        dto.setHttpMethod(entity.getHttpMethod());
        dto.setContentTypeBehavior(entity.getContentTypeBehavior());
        dto.setPathTemplate(entity.getPathTemplate());
        dto.setQueryTemplate(entity.getQueryTemplate());
        dto.setHeaderTemplate(entity.getHeaderTemplate());
        dto.setBodyTemplate(entity.getBodyTemplate());
        dto.setConnectTimeoutMs(entity.getConnectTimeoutMs());
        dto.setReadTimeoutMs(entity.getReadTimeoutMs());
        dto.setSuccessHttpCodes(entity.getSuccessHttpCodes());
        dto.setSuccessBodyPattern(entity.getSuccessBodyPattern());
        dto.setSuccessBodyMatchMode(entity.getSuccessBodyMatchMode());
        dto.setSuccessCaseSensitive(entity.getSuccessCaseSensitive());
        dto.setMaxRetryCount(entity.getMaxRetryCount());
        dto.setRetryBackoffInitialMs(entity.getRetryBackoffInitialMs());
        dto.setRetryBackoffMultiplier(entity.getRetryBackoffMultiplier());
        dto.setRetryBackoffMaxMs(entity.getRetryBackoffMaxMs());
        dto.setWorkerConcurrency(entity.getWorkerConcurrency());
        dto.setStatus(entity.getStatus());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        // 凭证脱敏：仅返回 key 列表
        dto.setCredentialKeys(extractCredentialKeys(entity.getCredentialsEncrypted()));

        return dto;
    }

    /**
     * 从加密凭证中提取 key 列表（脱敏）
     */
    private List<String> extractCredentialKeys(String encryptedCredentials) {
        if (encryptedCredentials == null || encryptedCredentials.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> decrypted = credentialVault.decrypt(encryptedCredentials);
            return new ArrayList<>(decrypted.keySet());
        } catch (Exception e) {
            log.warn("解密凭证提取 key 列表失败", e);
            return List.of();
        }
    }

    /**
     * 从创建请求复制字段到实体
     */
    private void copyFromCreateRequest(SupplierConfigEntity entity, SupplierConfigCreateRequest request) {
        entity.setSupplierCode(request.getSupplierCode());
        entity.setSupplierName(request.getSupplierName());
        entity.setDescription(request.getDescription());
        entity.setBaseUrl(request.getBaseUrl());
        entity.setHttpMethod(request.getHttpMethod());
        entity.setContentTypeBehavior(request.getContentTypeBehavior());
        entity.setPathTemplate(request.getPathTemplate());
        entity.setQueryTemplate(request.getQueryTemplate());
        entity.setHeaderTemplate(request.getHeaderTemplate());
        entity.setBodyTemplate(request.getBodyTemplate());
        entity.setConnectTimeoutMs(request.getConnectTimeoutMs());
        entity.setReadTimeoutMs(request.getReadTimeoutMs());
        entity.setSuccessHttpCodes(request.getSuccessHttpCodes());
        entity.setSuccessBodyPattern(request.getSuccessBodyPattern());
        entity.setSuccessBodyMatchMode(request.getSuccessBodyMatchMode());
        entity.setSuccessCaseSensitive(request.getSuccessCaseSensitive());
        entity.setMaxRetryCount(request.getMaxRetryCount());
        entity.setRetryBackoffInitialMs(request.getRetryBackoffInitialMs());
        entity.setRetryBackoffMultiplier(request.getRetryBackoffMultiplier());
        entity.setRetryBackoffMaxMs(request.getRetryBackoffMaxMs());
        entity.setWorkerConcurrency(request.getWorkerConcurrency());
        entity.setStatus(request.getStatus());
    }

    /**
     * 从更新请求复制字段到实体
     */
    private void copyFromUpdateRequest(SupplierConfigEntity entity, SupplierConfigUpdateRequest request) {
        entity.setSupplierName(request.getSupplierName());
        entity.setDescription(request.getDescription());
        entity.setBaseUrl(request.getBaseUrl());
        if (request.getHttpMethod() != null) entity.setHttpMethod(request.getHttpMethod());
        if (request.getContentTypeBehavior() != null) entity.setContentTypeBehavior(request.getContentTypeBehavior());
        entity.setPathTemplate(request.getPathTemplate());
        entity.setQueryTemplate(request.getQueryTemplate());
        entity.setHeaderTemplate(request.getHeaderTemplate());
        entity.setBodyTemplate(request.getBodyTemplate());
        if (request.getConnectTimeoutMs() != null) entity.setConnectTimeoutMs(request.getConnectTimeoutMs());
        if (request.getReadTimeoutMs() != null) entity.setReadTimeoutMs(request.getReadTimeoutMs());
        if (request.getSuccessHttpCodes() != null) entity.setSuccessHttpCodes(request.getSuccessHttpCodes());
        entity.setSuccessBodyPattern(request.getSuccessBodyPattern());
        if (request.getSuccessBodyMatchMode() != null) entity.setSuccessBodyMatchMode(request.getSuccessBodyMatchMode());
        if (request.getSuccessCaseSensitive() != null) entity.setSuccessCaseSensitive(request.getSuccessCaseSensitive());
        if (request.getMaxRetryCount() != null) entity.setMaxRetryCount(request.getMaxRetryCount());
        if (request.getRetryBackoffInitialMs() != null) entity.setRetryBackoffInitialMs(request.getRetryBackoffInitialMs());
        if (request.getRetryBackoffMultiplier() != null) entity.setRetryBackoffMultiplier(request.getRetryBackoffMultiplier());
        if (request.getRetryBackoffMaxMs() != null) entity.setRetryBackoffMaxMs(request.getRetryBackoffMaxMs());
        if (request.getWorkerConcurrency() != null) entity.setWorkerConcurrency(request.getWorkerConcurrency());
    }
}
