package com.rc.notification.application.admin;

import com.rc.notification.domain.config.SupplierConfigRepository;
import com.rc.notification.domain.event.EventType;
import com.rc.notification.domain.event.EventTypeRepository;
import com.rc.notification.domain.subscription.Subscription;
import com.rc.notification.domain.subscription.SubscriptionRepository;
import com.rc.notification.interfaces.admin.dto.PageResult;
import com.rc.notification.interfaces.admin.dto.SubscriptionCreateRequest;
import com.rc.notification.interfaces.admin.dto.SubscriptionDto;
import com.rc.notification.interfaces.admin.dto.SubscriptionUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订阅关系管理服务
 */
@Service
public class SubscriptionAdminService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAdminService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SupplierConfigRepository supplierConfigRepository;
    private final EventTypeRepository eventTypeRepository;

    public SubscriptionAdminService(SubscriptionRepository subscriptionRepository,
                                    SupplierConfigRepository supplierConfigRepository,
                                    EventTypeRepository eventTypeRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.supplierConfigRepository = supplierConfigRepository;
        this.eventTypeRepository = eventTypeRepository;
    }

    /**
     * 分页查询订阅列表
     */
    public PageResult<SubscriptionDto> listSubscriptions(String subscriberCode, String eventTypeCode,
                                                         String status, int page, int size) {
        List<Subscription> subscriptions = subscriptionRepository.findByFilters(subscriberCode, eventTypeCode, status, page, size);
        long total = subscriptionRepository.countByFilters(subscriberCode, eventTypeCode, status);

        List<SubscriptionDto> dtoList = subscriptions.stream()
                .map(this::toDto)
                .toList();

        return new PageResult<>(dtoList, total, page, size);
    }

    /**
     * 查询单个订阅
     */
    public SubscriptionDto getSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id);
        if (subscription == null) {
            throw new IllegalArgumentException("订阅不存在: id=" + id);
        }
        return toDto(subscription);
    }

    /**
     * 新增订阅
     */
    public SubscriptionDto createSubscription(SubscriptionCreateRequest request) {
        if (!supplierConfigRepository.existsBySupplierCode(request.getSubscriberCode())) {
            throw new IllegalArgumentException("订阅方不存在: subscriberCode=" + request.getSubscriberCode());
        }

        EventType eventType = eventTypeRepository.findByEventTypeCode(request.getEventTypeCode());
        if (eventType == null) {
            throw new IllegalArgumentException("事件类型不存在: eventTypeCode=" + request.getEventTypeCode());
        }
        if (!"ACTIVE".equals(eventType.getStatus())) {
            throw new IllegalArgumentException("事件类型未激活: eventTypeCode=" + request.getEventTypeCode());
        }

        if (subscriptionRepository.existsBySubscriberAndEventType(request.getSubscriberCode(), request.getEventTypeCode())) {
            throw new IllegalArgumentException("订阅关系已存在: subscriberCode=" + request.getSubscriberCode()
                    + ", eventTypeCode=" + request.getEventTypeCode());
        }

        Subscription subscription = new Subscription();
        subscription.setSubscriberCode(request.getSubscriberCode());
        subscription.setEventTypeCode(request.getEventTypeCode());
        subscription.setStatus("ACTIVE");
        subscription.setManagedBy(request.getManagedBy() != null ? request.getManagedBy() : "SUBSCRIBER");
        subscription.setPathTemplate(request.getPathTemplate());
        subscription.setQueryTemplate(request.getQueryTemplate());
        subscription.setHeaderTemplate(request.getHeaderTemplate());
        subscription.setBodyTemplate(request.getBodyTemplate());
        subscription.setConnectTimeoutMs(request.getConnectTimeoutMs());
        subscription.setReadTimeoutMs(request.getReadTimeoutMs());
        subscription.setMaxRetryCount(request.getMaxRetryCount());
        subscription.setRetryBackoffInitialMs(request.getRetryBackoffInitialMs());
        subscription.setRetryBackoffMultiplier(request.getRetryBackoffMultiplier());
        subscription.setRetryBackoffMaxMs(request.getRetryBackoffMaxMs());
        subscription.setSuccessHttpCodes(request.getSuccessHttpCodes());
        subscription.setSuccessBodyPattern(request.getSuccessBodyPattern());
        subscription.setSuccessBodyMatchMode(request.getSuccessBodyMatchMode());

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("新增订阅: subscriberCode={}, eventTypeCode={}", request.getSubscriberCode(), request.getEventTypeCode());

        return toDto(saved);
    }

    /**
     * 更新订阅（部分更新）
     */
    public SubscriptionDto updateSubscription(Long id, SubscriptionUpdateRequest request) {
        Subscription subscription = subscriptionRepository.findById(id);
        if (subscription == null) {
            throw new IllegalArgumentException("订阅不存在: id=" + id);
        }

        if (request.getStatus() != null) {
            subscription.setStatus(request.getStatus());
        }
        if (request.getManagedBy() != null) {
            subscription.setManagedBy(request.getManagedBy());
        }
        if (request.getPathTemplate() != null) {
            subscription.setPathTemplate(request.getPathTemplate());
        }
        if (request.getQueryTemplate() != null) {
            subscription.setQueryTemplate(request.getQueryTemplate());
        }
        if (request.getHeaderTemplate() != null) {
            subscription.setHeaderTemplate(request.getHeaderTemplate());
        }
        if (request.getBodyTemplate() != null) {
            subscription.setBodyTemplate(request.getBodyTemplate());
        }
        if (request.getConnectTimeoutMs() != null) {
            subscription.setConnectTimeoutMs(request.getConnectTimeoutMs());
        }
        if (request.getReadTimeoutMs() != null) {
            subscription.setReadTimeoutMs(request.getReadTimeoutMs());
        }
        if (request.getMaxRetryCount() != null) {
            subscription.setMaxRetryCount(request.getMaxRetryCount());
        }
        if (request.getRetryBackoffInitialMs() != null) {
            subscription.setRetryBackoffInitialMs(request.getRetryBackoffInitialMs());
        }
        if (request.getRetryBackoffMultiplier() != null) {
            subscription.setRetryBackoffMultiplier(request.getRetryBackoffMultiplier());
        }
        if (request.getRetryBackoffMaxMs() != null) {
            subscription.setRetryBackoffMaxMs(request.getRetryBackoffMaxMs());
        }
        if (request.getSuccessHttpCodes() != null) {
            subscription.setSuccessHttpCodes(request.getSuccessHttpCodes());
        }
        if (request.getSuccessBodyPattern() != null) {
            subscription.setSuccessBodyPattern(request.getSuccessBodyPattern());
        }
        if (request.getSuccessBodyMatchMode() != null) {
            subscription.setSuccessBodyMatchMode(request.getSuccessBodyMatchMode());
        }

        Subscription updated = subscriptionRepository.update(subscription);
        log.info("更新订阅: id={}", id);

        return toDto(updated);
    }

    private SubscriptionDto toDto(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(subscription.getId());
        dto.setSubscriberCode(subscription.getSubscriberCode());
        dto.setEventTypeCode(subscription.getEventTypeCode());
        dto.setStatus(subscription.getStatus());
        dto.setManagedBy(subscription.getManagedBy());
        dto.setPathTemplate(subscription.getPathTemplate());
        dto.setQueryTemplate(subscription.getQueryTemplate());
        dto.setHeaderTemplate(subscription.getHeaderTemplate());
        dto.setBodyTemplate(subscription.getBodyTemplate());
        dto.setConnectTimeoutMs(subscription.getConnectTimeoutMs());
        dto.setReadTimeoutMs(subscription.getReadTimeoutMs());
        dto.setMaxRetryCount(subscription.getMaxRetryCount());
        dto.setRetryBackoffInitialMs(subscription.getRetryBackoffInitialMs());
        dto.setRetryBackoffMultiplier(subscription.getRetryBackoffMultiplier());
        dto.setRetryBackoffMaxMs(subscription.getRetryBackoffMaxMs());
        dto.setSuccessHttpCodes(subscription.getSuccessHttpCodes());
        dto.setSuccessBodyPattern(subscription.getSuccessBodyPattern());
        dto.setSuccessBodyMatchMode(subscription.getSuccessBodyMatchMode());
        dto.setCreateTime(subscription.getCreateTime());
        dto.setUpdateTime(subscription.getUpdateTime());
        return dto;
    }
}
