package com.rc.notification.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rc.notification.infrastructure.persistence.entity.PublisherEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PublisherMapper extends BaseMapper<PublisherEntity> {
}
