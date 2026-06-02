package com.rc.notification.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rc.notification.infrastructure.persistence.entity.SupplierConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商配置 Mapper 接口
 */
@Mapper
public interface SupplierConfigMapper extends BaseMapper<SupplierConfigEntity> {
}
