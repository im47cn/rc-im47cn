package com.rc.notification.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rc.notification.infrastructure.persistence.entity.NotificationDlqLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 死信保险库 Mapper 接口
 */
@Mapper
public interface NotificationDlqLogMapper extends BaseMapper<NotificationDlqLogEntity> {
}
