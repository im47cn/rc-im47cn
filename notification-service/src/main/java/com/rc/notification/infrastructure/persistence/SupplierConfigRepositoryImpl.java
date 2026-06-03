package com.rc.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rc.notification.domain.config.SupplierConfig;
import com.rc.notification.domain.config.SupplierConfigRepository;
import com.rc.notification.infrastructure.persistence.entity.SupplierConfigEntity;
import com.rc.notification.infrastructure.persistence.mapper.SupplierConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 供应商配置 Repository 实现
 * <p>
 * 基于 MyBatis-Plus，完成 Entity ↔ SupplierConfig 转换
 */
@Repository
public class SupplierConfigRepositoryImpl implements SupplierConfigRepository {

    private final SupplierConfigMapper supplierConfigMapper;

    public SupplierConfigRepositoryImpl(SupplierConfigMapper supplierConfigMapper) {
        this.supplierConfigMapper = supplierConfigMapper;
    }

    @Override
    public SupplierConfig findById(Long id) {
        SupplierConfigEntity entity = supplierConfigMapper.selectById(id);
        return SupplierConfig.fromEntity(entity);
    }

    @Override
    public SupplierConfig findBySupplierCode(String supplierCode) {
        LambdaQueryWrapper<SupplierConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierConfigEntity::getSupplierCode, supplierCode);
        SupplierConfigEntity entity = supplierConfigMapper.selectOne(wrapper);
        return SupplierConfig.fromEntity(entity);
    }

    @Override
    public List<SupplierConfig> findByFilters(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<SupplierConfigEntity> wrapper = buildFilterWrapper(keyword, status);
        wrapper.orderByDesc(SupplierConfigEntity::getUpdateTime);

        IPage<SupplierConfigEntity> pageResult = supplierConfigMapper.selectPage(
                new Page<>(page, size), wrapper);

        return pageResult.getRecords().stream()
                .map(SupplierConfig::fromEntity)
                .toList();
    }

    @Override
    public long countByFilters(String keyword, Integer status) {
        LambdaQueryWrapper<SupplierConfigEntity> wrapper = buildFilterWrapper(keyword, status);
        return supplierConfigMapper.selectCount(wrapper);
    }

    @Override
    public SupplierConfig save(SupplierConfig config) {
        SupplierConfigEntity entity = config.toEntity();
        supplierConfigMapper.insert(entity);
        config.setId(entity.getId());
        return config;
    }

    @Override
    public SupplierConfig update(SupplierConfig config) {
        SupplierConfigEntity entity = config.toEntity();
        supplierConfigMapper.updateById(entity);
        return config;
    }

    @Override
    public boolean existsBySupplierCode(String supplierCode) {
        LambdaQueryWrapper<SupplierConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierConfigEntity::getSupplierCode, supplierCode);
        return supplierConfigMapper.selectCount(wrapper) > 0;
    }

    private LambdaQueryWrapper<SupplierConfigEntity> buildFilterWrapper(String keyword, Integer status) {
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
        return wrapper;
    }
}
