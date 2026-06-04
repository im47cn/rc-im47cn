package com.rc.notification.domain.subscription;

import java.util.List;

public interface SubscriptionRepository {
    Subscription findById(Long id);
    Subscription findBySubscriberAndEventType(String subscriberCode, String eventTypeCode);
    List<Subscription> findActiveByEventTypeCode(String eventTypeCode);
    List<Subscription> findBySubscriberCode(String subscriberCode);
    List<Subscription> findByFilters(String subscriberCode, String eventTypeCode, String status, int page, int size);
    long countByFilters(String subscriberCode, String eventTypeCode, String status);
    Subscription save(Subscription subscription);
    Subscription update(Subscription subscription);
    boolean existsBySubscriberAndEventType(String subscriberCode, String eventTypeCode);
}
