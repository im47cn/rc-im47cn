package com.rc.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rc.notification.domain.detection.ChangeRecord;
import com.rc.notification.domain.detection.ChangeRecordRepository;
import com.rc.notification.infrastructure.persistence.entity.ChangeRecordEntity;
import com.rc.notification.infrastructure.persistence.mapper.ChangeRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 变更记录 Repository 实现
 * <p>
 * 基于 MyBatis-Plus，完成 Entity ↔ ChangeRecord 转换
 */
@Repository
public class ChangeRecordRepositoryImpl implements ChangeRecordRepository {

    private final ChangeRecordMapper changeRecordMapper;

    public ChangeRecordRepositoryImpl(ChangeRecordMapper changeRecordMapper) {
        this.changeRecordMapper = changeRecordMapper;
    }

    @Override
    public ChangeRecord save(ChangeRecord record) {
        ChangeRecordEntity entity = record.toEntity();
        changeRecordMapper.insert(entity);
        record.setId(entity.getId());
        return record;
    }

    @Override
    public List<ChangeRecord> findByEventTypeCode(String eventTypeCode) {
        LambdaQueryWrapper<ChangeRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChangeRecordEntity::getEventTypeCode, eventTypeCode);
        return changeRecordMapper.selectList(wrapper).stream()
                .map(ChangeRecord::fromEntity)
                .toList();
    }

    @Override
    public List<ChangeRecord> findByStatus(String status) {
        LambdaQueryWrapper<ChangeRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChangeRecordEntity::getStatus, status);
        return changeRecordMapper.selectList(wrapper).stream()
                .map(ChangeRecord::fromEntity)
                .toList();
    }

    @Override
    public ChangeRecord findById(Long id) {
        ChangeRecordEntity entity = changeRecordMapper.selectById(id);
        return ChangeRecord.fromEntity(entity);
    }

    @Override
    public ChangeRecord update(ChangeRecord record) {
        ChangeRecordEntity entity = record.toEntity();
        changeRecordMapper.updateById(entity);
        return record;
    }
}
