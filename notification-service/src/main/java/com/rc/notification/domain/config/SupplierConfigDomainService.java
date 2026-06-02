package com.rc.notification.domain.config;

import com.rc.notification.infrastructure.persistence.entity.SupplierConfigEntity;

import java.util.List;

/**
 * 供应商配置领域服务接口
 * <p>
 * 提供配置的缓存读取、失效、全量加载等能力
 */
public interface SupplierConfigDomainService {

    /**
     * 通过供应商编码获取配置（缓存优先）
     *
     * @param supplierCode 供应商唯一标识
     * @return 供应商配置实体，不存在或已禁用返回 null
     */
    SupplierConfigEntity getBySupplierCode(String supplierCode);

    /**
     * 获取所有启用状态的供应商配置（缓存优先）
     *
     * @return 启用状态的供应商配置列表
     */
    List<SupplierConfigEntity> getAllActive();

    /**
     * 精准失效指定供应商的缓存
     *
     * @param supplierCode 供应商唯一标识
     */
    void evictCache(String supplierCode);

    /**
     * 失效全量缓存（看门狗兜底场景）
     */
    void evictAllCache();
}
