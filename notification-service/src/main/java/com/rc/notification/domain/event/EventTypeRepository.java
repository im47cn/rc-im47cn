package com.rc.notification.domain.event;

import java.util.List;

public interface EventTypeRepository {
    EventType findById(Long id);
    EventType findByEventTypeCode(String eventTypeCode);
    List<EventType> findByPublisherCode(String publisherCode);
    List<EventType> findByFilters(String keyword, String publisherCode, String status, int page, int size);
    long countByFilters(String keyword, String publisherCode, String status);
    EventType save(EventType eventType);
    EventType update(EventType eventType);
    boolean existsByEventTypeCode(String eventTypeCode);
}
