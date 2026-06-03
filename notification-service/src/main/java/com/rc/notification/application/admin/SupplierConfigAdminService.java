package com.rc.notification.application.admin;

import com.rc.notification.domain.config.SupplierConfig;
import com.rc.notification.domain.config.SupplierConfigRepository;
import com.rc.notification.domain.credential.CredentialVault;
import com.rc.notification.infrastructure.cache.ConfigEvictionListener;
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

    private final SupplierConfigRepository supplierConfigRepository;
    private final CredentialVault credentialVault;
    private final ConfigEvictionListener configEvictionListener;

    public SupplierConfigAdminService(SupplierConfigRepository supplierConfigRepository,
                                       CredentialVault credentialVault,
                                       ConfigEvictionListener configEvictionListener) {
        this.supplierConfigRepository = supplierConfigRepository;
        this.credentialVault = credentialVault;
        this.configEvictionListener = configEvictionListener;
    }

    /**
     * 分页查询供应商列表
     */
    public PageResult<SupplierConfigDto> listSuppliers(String keyword, Integer status, int page, int size) {
        List<SupplierConfig> configs = supplierConfigRepository.findByFilters(keyword, status, page, size);
        long total = supplierConfigRepository.countByFilters(keyword, status);

        List<SupplierConfigDto> dtoList = configs.stream()
                .map(this::toDto)
                .toList();

        return new PageResult<>(dtoList, total, page, size);
    }

    /**
     * 查询单个供应商完整配置（凭证脱敏）
     */
    public SupplierConfigDto getSupplier(Long id) {
        SupplierConfig config = supplierConfigRepository.findById(id);
        if (config == null) {
            throw new IllegalArgumentException("供应商配置不存在: id=" + id);
        }
        return toDto(config);
    }

    /**
     * 新增供应商
     */
    public SupplierConfigDto createSupplier(SupplierConfigCreateRequest request) {
        // 校验 supplier_code 唯一性
        if (supplierConfigRepository.existsBySupplierCode(request.getSupplierCode())) {
            throw new IllegalArgumentException("供应商编码已存在: " + request.getSupplierCode());
        }

        SupplierConfig config = new SupplierConfig();
        copyFromCreateRequest(config, request);

        // 加密凭证
        if (request.getCredentials() != null && !request.getCredentials().isEmpty()) {
            config.setCredentialsEncrypted(credentialVault.encrypt(request.getCredentials()));
        }

        supplierConfigRepository.save(config);
        log.info("新增供应商配置: supplierCode={}", request.getSupplierCode());

        // 广播 Pub/Sub CREATE 事件
        configEvictionListener.publishEviction(request.getSupplierCode(), "CREATE");

        return toDto(config);
    }

    /**
     * 修改供应商配置
     */
    public SupplierConfigDto updateSupplier(Long id, SupplierConfigUpdateRequest request) {
        SupplierConfig config = supplierConfigRepository.findById(id);
        if (config == null) {
            throw new IllegalArgumentException("供应商配置不存在: id=" + id);
        }

        // 保存更新前的配置快照用于变更检测
        String snapshotBefore = configSnapshot(config);

        copyFromUpdateRequest(config, request);

        // 凭证处理：null 表示保留原值，非 null 表示重新加密
        boolean credentialChanged = false;
        if (request.getCredentials() != null) {
            config.setCredentialsEncrypted(credentialVault.encrypt(request.getCredentials()));
            credentialChanged = true;
        }

        // 检测是否有实际变更
        String snapshotAfter = configSnapshot(config);
        if (!credentialChanged && snapshotBefore.equals(snapshotAfter)) {
            log.info("供应商配置无变更，跳过更新: id={}, supplierCode={}", id, config.getSupplierCode());
            return toDto(config);
        }

        supplierConfigRepository.update(config);
        log.info("修改供应商配置: id={}, supplierCode={}", id, config.getSupplierCode());

        // 广播 Pub/Sub UPDATE 事件
        configEvictionListener.publishEviction(config.getSupplierCode(), "UPDATE");

        return toDto(config);
    }

    /**
     * 启用/禁用供应商
     */
    public void toggleSupplierStatus(Long id, int status) {
        SupplierConfig config = supplierConfigRepository.findById(id);
        if (config == null) {
            throw new IllegalArgumentException("供应商配置不存在: id=" + id);
        }

        config.setStatus(status);
        supplierConfigRepository.update(config);
        log.info("切换供应商状态: id={}, supplierCode={}, status={}", id, config.getSupplierCode(), status);

        // 广播状态变更事件
        String action = status == 1 ? "ENABLE" : "DISABLE";
        configEvictionListener.publishEviction(config.getSupplierCode(), action);
    }

    /**
     * SupplierConfig 转 DTO（凭证脱敏）
     */
    private SupplierConfigDto toDto(SupplierConfig config) {
        SupplierConfigDto dto = new SupplierConfigDto();
        dto.setId(config.getId());
        dto.setSupplierCode(config.getSupplierCode());
        dto.setSupplierName(config.getSupplierName());
        dto.setDescription(config.getDescription());
        dto.setBaseUrl(config.getBaseUrl());
        dto.setHttpMethod(config.getHttpMethod());
        dto.setContentTypeBehavior(config.getContentTypeBehavior());
        dto.setPathTemplate(config.getPathTemplate());
        dto.setQueryTemplate(config.getQueryTemplate());
        dto.setHeaderTemplate(config.getHeaderTemplate());
        dto.setBodyTemplate(config.getBodyTemplate());
        dto.setConnectTimeoutMs(config.getConnectTimeoutMs());
        dto.setReadTimeoutMs(config.getReadTimeoutMs());
        dto.setSuccessHttpCodes(config.getSuccessHttpCodes());
        dto.setSuccessBodyPattern(config.getSuccessBodyPattern());
        dto.setSuccessBodyMatchMode(config.getSuccessBodyMatchMode());
        dto.setSuccessCaseSensitive(config.getSuccessCaseSensitive());
        dto.setMaxRetryCount(config.getMaxRetryCount());
        dto.setRetryBackoffInitialMs(config.getRetryBackoffInitialMs());
        dto.setRetryBackoffMultiplier(config.getRetryBackoffMultiplier());
        dto.setRetryBackoffMaxMs(config.getRetryBackoffMaxMs());
        dto.setWorkerConcurrency(config.getWorkerConcurrency());
        dto.setStatus(config.getStatus());
        dto.setCreateTime(config.getCreateTime());
        dto.setUpdateTime(config.getUpdateTime());

        // 凭证脱敏：仅返回 key 列表
        dto.setCredentialKeys(extractCredentialKeys(config.getCredentialsEncrypted()));

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
     * 从创建请求复制字段到领域模型
     */
    private void copyFromCreateRequest(SupplierConfig config, SupplierConfigCreateRequest request) {
        config.setSupplierCode(request.getSupplierCode());
        config.setSupplierName(request.getSupplierName());
        config.setDescription(request.getDescription());
        config.setBaseUrl(request.getBaseUrl());
        config.setHttpMethod(request.getHttpMethod());
        config.setContentTypeBehavior(request.getContentTypeBehavior());
        config.setPathTemplate(request.getPathTemplate());
        config.setQueryTemplate(request.getQueryTemplate());
        config.setHeaderTemplate(request.getHeaderTemplate());
        config.setBodyTemplate(request.getBodyTemplate());
        config.setConnectTimeoutMs(request.getConnectTimeoutMs());
        config.setReadTimeoutMs(request.getReadTimeoutMs());
        config.setSuccessHttpCodes(request.getSuccessHttpCodes());
        config.setSuccessBodyPattern(request.getSuccessBodyPattern());
        config.setSuccessBodyMatchMode(request.getSuccessBodyMatchMode());
        config.setSuccessCaseSensitive(request.getSuccessCaseSensitive());
        config.setMaxRetryCount(request.getMaxRetryCount());
        config.setRetryBackoffInitialMs(request.getRetryBackoffInitialMs());
        config.setRetryBackoffMultiplier(request.getRetryBackoffMultiplier());
        config.setRetryBackoffMaxMs(request.getRetryBackoffMaxMs());
        config.setWorkerConcurrency(request.getWorkerConcurrency());
        config.setStatus(request.getStatus());
    }

    /**
     * 从更新请求复制字段到领域模型
     */
    private void copyFromUpdateRequest(SupplierConfig config, SupplierConfigUpdateRequest request) {
        config.setSupplierName(request.getSupplierName());
        config.setDescription(request.getDescription());
        config.setBaseUrl(request.getBaseUrl());
        if (request.getHttpMethod() != null) config.setHttpMethod(request.getHttpMethod());
        if (request.getContentTypeBehavior() != null) config.setContentTypeBehavior(request.getContentTypeBehavior());
        config.setPathTemplate(request.getPathTemplate());
        config.setQueryTemplate(request.getQueryTemplate());
        config.setHeaderTemplate(request.getHeaderTemplate());
        config.setBodyTemplate(request.getBodyTemplate());
        if (request.getConnectTimeoutMs() != null) config.setConnectTimeoutMs(request.getConnectTimeoutMs());
        if (request.getReadTimeoutMs() != null) config.setReadTimeoutMs(request.getReadTimeoutMs());
        if (request.getSuccessHttpCodes() != null) config.setSuccessHttpCodes(request.getSuccessHttpCodes());
        config.setSuccessBodyPattern(request.getSuccessBodyPattern());
        if (request.getSuccessBodyMatchMode() != null) config.setSuccessBodyMatchMode(request.getSuccessBodyMatchMode());
        if (request.getSuccessCaseSensitive() != null) config.setSuccessCaseSensitive(request.getSuccessCaseSensitive());
        if (request.getMaxRetryCount() != null) config.setMaxRetryCount(request.getMaxRetryCount());
        if (request.getRetryBackoffInitialMs() != null) config.setRetryBackoffInitialMs(request.getRetryBackoffInitialMs());
        if (request.getRetryBackoffMultiplier() != null) config.setRetryBackoffMultiplier(request.getRetryBackoffMultiplier());
        if (request.getRetryBackoffMaxMs() != null) config.setRetryBackoffMaxMs(request.getRetryBackoffMaxMs());
        if (request.getWorkerConcurrency() != null) config.setWorkerConcurrency(request.getWorkerConcurrency());
    }

    /**
     * 生成配置快照字符串，用于变更检测（排除 id、createTime、updateTime）
     */
    private String configSnapshot(SupplierConfig c) {
        return String.join("|",
                str(c.getSupplierName()), str(c.getDescription()), str(c.getBaseUrl()),
                str(c.getHttpMethod()), str(c.getContentTypeBehavior()),
                str(c.getPathTemplate()), str(c.getQueryTemplate()),
                str(c.getHeaderTemplate()), str(c.getBodyTemplate()),
                str(c.getConnectTimeoutMs()), str(c.getReadTimeoutMs()),
                str(c.getSuccessHttpCodes()), str(c.getSuccessBodyPattern()),
                str(c.getSuccessBodyMatchMode()), str(c.getSuccessCaseSensitive()),
                str(c.getMaxRetryCount()), str(c.getRetryBackoffInitialMs()),
                str(c.getRetryBackoffMultiplier()), str(c.getRetryBackoffMaxMs()),
                str(c.getWorkerConcurrency()), str(c.getStatus()));
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
