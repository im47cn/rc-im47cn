package com.rc.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rc.notification.domain.event.EventType;
import com.rc.notification.domain.event.EventTypeRepository;
import com.rc.notification.infrastructure.persistence.entity.EventTypeEntity;
import com.rc.notification.infrastructure.persistence.mapper.EventTypeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 事件类型 Repository 实现
 * <p>
 * 基于 MyBatis-Plus，完成 Entity ↔ EventType 转换
 */
@Repository
public class EventTypeRepositoryImpl implements EventTypeRepository {

    private final EventTypeMapper eventTypeMapper;

    public EventTypeRepositoryImpl(EventTypeMapper eventTypeMapper) {
        this.eventTypeMapper = eventTypeMapper;
    }

    @Override
    public EventType findById(Long id) {
        EventTypeEntity entity = eventTypeMapper.selectById(id);
        return EventType.fromEntity(entity);
    }

    @Override
    public EventType findByEventTypeCode(String eventTypeCode) {
        LambdaQueryWrapper<EventTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventTypeEntity::getEventTypeCode, eventTypeCode);
        EventTypeEntity entity = eventTypeMapper.selectOne(wrapper);
        return EventType.fromEntity(entity);
    }

    @Override
    public List<EventType> findByPublisherCode(String publisherCode) {
        LambdaQueryWrapper<EventTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventTypeEntity::getPublisherCode, publisherCode);
        wrapper.orderByDesc(EventTypeEntity::getUpdateTime);
        return eventTypeMapper.selectList(wrapper).stream()
                .map(EventType::fromEntity)
                .toList();
    }

    @Override
    public List<EventType> findByFilters(String keyword, String publisherCode, String status, int page, int size) {
        LambdaQueryWrapper<EventTypeEntity> wrapper = buildFilterWrapper(keyword, publisherCode, status);
        wrapper.orderByDesc(EventTypeEntity::getUpdateTime);

        IPage<EventTypeEntity> pageResult = eventTypeMapper.selectPage(
                new Page<>(page, size), wrapper);

        return pageResult.getRecords().stream()
                .map(EventType::fromEntity)
                .toList();
    }

    @Override
    public long countByFilters(String keyword, String publisherCode, String status) {
        LambdaQueryWrapper<EventTypeEntity> wrapper = buildFilterWrapper(keyword, publisherCode, status);
        return eventTypeMapper.selectCount(wrapper);
    }

    @Override
    public EventType save(EventType eventType) {
        EventTypeEntity entity = eventType.toEntity();
        eventTypeMapper.insert(entity);
        eventType.setId(entity.getId());
        return eventType;
    }

    @Override
    public EventType update(EventType eventType) {
        EventTypeEntity entity = eventType.toEntity();
        eventTypeMapper.updateById(entity);
        return eventType;
    }

    @Override
    public boolean existsByEventTypeCode(String eventTypeCode) {
        LambdaQueryWrapper<EventTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventTypeEntity::getEventTypeCode, eventTypeCode);
        return eventTypeMapper.selectCount(wrapper) > 0;
    }

    private LambdaQueryWrapper<EventTypeEntity> buildFilterWrapper(String keyword, String publisherCode, String status) {
        LambdaQueryWrapper<EventTypeEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(EventTypeEntity::getEventTypeCode, keyword)
                    .or()
                    .like(EventTypeEntity::getDisplayName, keyword));
        }
        if (publisherCode != null && !publisherCode.isBlank()) {
            wrapper.eq(EventTypeEntity::getPublisherCode, publisherCode);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(EventTypeEntity::getStatus, status);
        }
        return wrapper;
    }
}
