package com.rc.notification.application.admin;

import com.rc.notification.application.detection.ChangeDetectionService;
import com.rc.notification.domain.event.EventType;
import com.rc.notification.domain.event.EventTypeRepository;
import com.rc.notification.domain.publisher.PublisherRepository;
import com.rc.notification.interfaces.admin.dto.EventTypeCreateRequest;
import com.rc.notification.interfaces.admin.dto.EventTypeDto;
import com.rc.notification.interfaces.admin.dto.EventTypeUpdateRequest;
import com.rc.notification.interfaces.admin.dto.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 事件类型管理服务
 */
@Service
public class EventTypeAdminService {

    private static final Logger log = LoggerFactory.getLogger(EventTypeAdminService.class);

    private final EventTypeRepository eventTypeRepository;
    private final PublisherRepository publisherRepository;
    private ChangeDetectionService changeDetectionService;

    public EventTypeAdminService(EventTypeRepository eventTypeRepository, PublisherRepository publisherRepository) {
        this.eventTypeRepository = eventTypeRepository;
        this.publisherRepository = publisherRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setChangeDetectionService(ChangeDetectionService changeDetectionService) {
        this.changeDetectionService = changeDetectionService;
    }

    /**
     * 分页查询事件类型列表
     */
    public PageResult<EventTypeDto> listEventTypes(String keyword, String publisherCode, String status, int page, int size) {
        List<EventType> eventTypes = eventTypeRepository.findByFilters(keyword, publisherCode, status, page, size);
        long total = eventTypeRepository.countByFilters(keyword, publisherCode, status);

        List<EventTypeDto> dtoList = eventTypes.stream()
                .map(this::toDto)
                .toList();

        return new PageResult<>(dtoList, total, page, size);
    }

    /**
     * 查询单个事件类型
     */
    public EventTypeDto getEventType(Long id) {
        EventType eventType = eventTypeRepository.findById(id);
        if (eventType == null) {
            throw new IllegalArgumentException("事件类型不存在: id=" + id);
        }
        return toDto(eventType);
    }

    /**
     * 新增事件类型
     */
    public EventTypeDto createEventType(EventTypeCreateRequest request) {
        if (!publisherRepository.existsByPublisherCode(request.getPublisherCode())) {
            throw new IllegalArgumentException("发布方不存在: publisherCode=" + request.getPublisherCode());
        }
        if (eventTypeRepository.existsByEventTypeCode(request.getEventTypeCode())) {
            throw new IllegalArgumentException("事件类型编码已存在: " + request.getEventTypeCode());
        }

        EventType eventType = new EventType();
        eventType.setEventTypeCode(request.getEventTypeCode());
        eventType.setPublisherCode(request.getPublisherCode());
        eventType.setDisplayName(request.getDisplayName());
        eventType.setDescription(request.getDescription());
        eventType.setPayloadSchema(request.getPayloadSchema());
        eventType.setStatus("DRAFT");
        eventType.setVersion(1);

        EventType saved = eventTypeRepository.save(eventType);
        log.info("新增事件类型: eventTypeCode={}", request.getEventTypeCode());

        return toDto(saved);
    }

    /**
     * 更新事件类型
     */
    public EventTypeDto updateEventType(Long id, EventTypeUpdateRequest request) {
        EventType eventType = eventTypeRepository.findById(id);
        if (eventType == null) {
            throw new IllegalArgumentException("事件类型不存在: id=" + id);
        }

        if (request.getDisplayName() != null) {
            eventType.setDisplayName(request.getDisplayName());
        }
        if (request.getDescription() != null) {
            eventType.setDescription(request.getDescription());
        }
        if (request.getPayloadSchema() != null) {
            String oldSchema = eventType.getPayloadSchema();
            String newSchema = request.getPayloadSchema();
            if (!newSchema.equals(oldSchema)) {
                eventType.setVersion(eventType.getVersion() + 1);
                // 触发 Schema 变更检测
                if (changeDetectionService != null) {
                    changeDetectionService.detectSchemaChange(eventType.getEventTypeCode(), oldSchema, newSchema);
                }
            }
            eventType.setPayloadSchema(newSchema);
        }
        if (request.getStatus() != null) {
            eventType.setStatus(request.getStatus());
        }

        EventType updated = eventTypeRepository.update(eventType);
        log.info("更新事件类型: id={}, eventTypeCode={}", id, eventType.getEventTypeCode());

        return toDto(updated);
    }

    private EventTypeDto toDto(EventType eventType) {
        EventTypeDto dto = new EventTypeDto();
        dto.setId(eventType.getId());
        dto.setEventTypeCode(eventType.getEventTypeCode());
        dto.setPublisherCode(eventType.getPublisherCode());
        dto.setDisplayName(eventType.getDisplayName());
        dto.setDescription(eventType.getDescription());
        dto.setPayloadSchema(eventType.getPayloadSchema());
        dto.setStatus(eventType.getStatus());
        dto.setVersion(eventType.getVersion());
        dto.setCreateTime(eventType.getCreateTime());
        dto.setUpdateTime(eventType.getUpdateTime());
        return dto;
    }
}
