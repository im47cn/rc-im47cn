package com.rc.notification.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rc.notification.infrastructure.persistence.entity.SubscriptionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubscriptionMapper extends BaseMapper<SubscriptionEntity> {
}
