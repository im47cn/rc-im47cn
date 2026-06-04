package com.rc.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rc.notification.domain.detection.FieldFingerprint;
import com.rc.notification.domain.detection.FieldFingerprintRepository;
import com.rc.notification.infrastructure.persistence.entity.FieldFingerprintEntity;
import com.rc.notification.infrastructure.persistence.mapper.FieldFingerprintMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字段指纹 Repository 实现
 * <p>
 * 基于 MyBatis-Plus，完成 Entity ↔ FieldFingerprint 转换
 */
@Repository
public class FieldFingerprintRepositoryImpl implements FieldFingerprintRepository {

    private final FieldFingerprintMapper fieldFingerprintMapper;

    public FieldFingerprintRepositoryImpl(FieldFingerprintMapper fieldFingerprintMapper) {
        this.fieldFingerprintMapper = fieldFingerprintMapper;
    }

    @Override
    public FieldFingerprint findByEventTypeCodeAndFieldPath(String eventTypeCode, String fieldPath) {
        LambdaQueryWrapper<FieldFingerprintEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldFingerprintEntity::getEventTypeCode, eventTypeCode)
               .eq(FieldFingerprintEntity::getFieldPath, fieldPath);
        FieldFingerprintEntity entity = fieldFingerprintMapper.selectOne(wrapper);
        return FieldFingerprint.fromEntity(entity);
    }

    @Override
    public List<FieldFingerprint> findByEventTypeCode(String eventTypeCode) {
        LambdaQueryWrapper<FieldFingerprintEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldFingerprintEntity::getEventTypeCode, eventTypeCode);
        return fieldFingerprintMapper.selectList(wrapper).stream()
                .map(FieldFingerprint::fromEntity)
                .toList();
    }

    @Override
    public FieldFingerprint save(FieldFingerprint fingerprint) {
        FieldFingerprintEntity entity = fingerprint.toEntity();
        fieldFingerprintMapper.insert(entity);
        fingerprint.setId(entity.getId());
        return fingerprint;
    }

    @Override
    public FieldFingerprint update(FieldFingerprint fingerprint) {
        FieldFingerprintEntity entity = fingerprint.toEntity();
        fieldFingerprintMapper.updateById(entity);
        return fingerprint;
    }
}
