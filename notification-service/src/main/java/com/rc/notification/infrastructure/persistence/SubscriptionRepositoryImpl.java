package com.rc.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rc.notification.domain.subscription.Subscription;
import com.rc.notification.domain.subscription.SubscriptionRepository;
import com.rc.notification.infrastructure.persistence.entity.SubscriptionEntity;
import com.rc.notification.infrastructure.persistence.mapper.SubscriptionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订阅关系 Repository 实现
 * <p>
 * 基于 MyBatis-Plus，完成 Entity ↔ Subscription 转换
 */
@Repository
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final SubscriptionMapper subscriptionMapper;

    public SubscriptionRepositoryImpl(SubscriptionMapper subscriptionMapper) {
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    public Subscription findById(Long id) {
        SubscriptionEntity entity = subscriptionMapper.selectById(id);
        return Subscription.fromEntity(entity);
    }

    @Override
    public Subscription findBySubscriberAndEventType(String subscriberCode, String eventTypeCode) {
        LambdaQueryWrapper<SubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionEntity::getSubscriberCode, subscriberCode);
        wrapper.eq(SubscriptionEntity::getEventTypeCode, eventTypeCode);
        SubscriptionEntity entity = subscriptionMapper.selectOne(wrapper);
        return Subscription.fromEntity(entity);
    }

    @Override
    public List<Subscription> findActiveByEventTypeCode(String eventTypeCode) {
        LambdaQueryWrapper<SubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionEntity::getEventTypeCode, eventTypeCode);
        wrapper.eq(SubscriptionEntity::getStatus, "ACTIVE");
        wrapper.orderByDesc(SubscriptionEntity::getUpdateTime);
        return subscriptionMapper.selectList(wrapper).stream()
                .map(Subscription::fromEntity)
                .toList();
    }

    @Override
    public List<Subscription> findBySubscriberCode(String subscriberCode) {
        LambdaQueryWrapper<SubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionEntity::getSubscriberCode, subscriberCode);
        wrapper.orderByDesc(SubscriptionEntity::getUpdateTime);
        return subscriptionMapper.selectList(wrapper).stream()
                .map(Subscription::fromEntity)
                .toList();
    }

    @Override
    public List<Subscription> findByFilters(String subscriberCode, String eventTypeCode, String status, int page, int size) {
        LambdaQueryWrapper<SubscriptionEntity> wrapper = buildFilterWrapper(subscriberCode, eventTypeCode, status);
        wrapper.orderByDesc(SubscriptionEntity::getUpdateTime);

        IPage<SubscriptionEntity> pageResult = subscriptionMapper.selectPage(
                new Page<>(page, size), wrapper);

        return pageResult.getRecords().stream()
                .map(Subscription::fromEntity)
                .toList();
    }

    @Override
    public long countByFilters(String subscriberCode, String eventTypeCode, String status) {
        LambdaQueryWrapper<SubscriptionEntity> wrapper = buildFilterWrapper(subscriberCode, eventTypeCode, status);
        return subscriptionMapper.selectCount(wrapper);
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionEntity entity = subscription.toEntity();
        subscriptionMapper.insert(entity);
        subscription.setId(entity.getId());
        return subscription;
    }

    @Override
    public Subscription update(Subscription subscription) {
        SubscriptionEntity entity = subscription.toEntity();
        subscriptionMapper.updateById(entity);
        return subscription;
    }

    @Override
    public boolean existsBySubscriberAndEventType(String subscriberCode, String eventTypeCode) {
        LambdaQueryWrapper<SubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionEntity::getSubscriberCode, subscriberCode);
        wrapper.eq(SubscriptionEntity::getEventTypeCode, eventTypeCode);
        return subscriptionMapper.selectCount(wrapper) > 0;
    }

    private LambdaQueryWrapper<SubscriptionEntity> buildFilterWrapper(String subscriberCode, String eventTypeCode, String status) {
        LambdaQueryWrapper<SubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
        if (subscriberCode != null && !subscriberCode.isBlank()) {
            wrapper.eq(SubscriptionEntity::getSubscriberCode, subscriberCode);
        }
        if (eventTypeCode != null && !eventTypeCode.isBlank()) {
            wrapper.eq(SubscriptionEntity::getEventTypeCode, eventTypeCode);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(SubscriptionEntity::getStatus, status);
        }
        return wrapper;
    }
}
