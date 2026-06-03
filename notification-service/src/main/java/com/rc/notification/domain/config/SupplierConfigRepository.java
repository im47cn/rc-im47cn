package com.rc.notification.domain.config;

import java.util.List;

/**
 * 供应商配置 Repository 接口
 * <p>
 * 领域层定义的持久化抽象，由 infrastructure 层实现
 */
public interface SupplierConfigRepository {

    SupplierConfig findById(Long id);

    SupplierConfig findBySupplierCode(String supplierCode);

    List<SupplierConfig> findByFilters(String keyword, Integer status, int page, int size);

    long countByFilters(String keyword, Integer status);

    SupplierConfig save(SupplierConfig config);

    SupplierConfig update(SupplierConfig config);

    boolean existsBySupplierCode(String supplierCode);
}
