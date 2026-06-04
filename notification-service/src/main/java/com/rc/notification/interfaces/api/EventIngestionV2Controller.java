package com.rc.notification.interfaces.api;

import com.rc.notification.application.detection.FieldSamplingService;
import com.rc.notification.application.service.IngestionService;
import com.rc.notification.domain.event.EventType;
import com.rc.notification.domain.event.EventTypeRepository;
import com.rc.notification.domain.subscription.Subscription;
import com.rc.notification.domain.subscription.SubscriptionRepository;
import com.rc.notification.interfaces.api.dto.DispatchDetail;
import com.rc.notification.interfaces.api.dto.IngestV2Request;
import com.rc.notification.interfaces.api.dto.IngestV2Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class EventIngestionV2Controller {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionV2Controller.class);

    private final IngestionService ingestionService;
    private final EventTypeRepository eventTypeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final FieldSamplingService fieldSamplingService;

    public EventIngestionV2Controller(IngestionService ingestionService,
                                       EventTypeRepository eventTypeRepository,
                                       SubscriptionRepository subscriptionRepository,
                                       FieldSamplingService fieldSamplingService) {
        this.ingestionService = ingestionService;
        this.eventTypeRepository = eventTypeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.fieldSamplingService = fieldSamplingService;
    }

    @PostMapping("/api/v2/notifications/ingest")
    public ResponseEntity<IngestV2Response> ingestV2(
            @Valid @RequestBody IngestV2Request request,
            HttpServletRequest httpRequest) {
        try {
            String publisherCode = (String) httpRequest.getAttribute(PublisherAuthFilter.PUBLISHER_CODE_ATTR);

            // Validate event type
            EventType eventType = eventTypeRepository.findByEventTypeCode(request.getEventType());
            if (eventType == null) {
                // 自动注册为 DRAFT 事件类型，等待人工审核激活
                eventType = new EventType();
                eventType.setEventTypeCode(request.getEventType());
                eventType.setPublisherCode(publisherCode);
                eventType.setDisplayName(request.getEventType());
                eventType.setStatus("DRAFT");
                eventType.setVersion(1);
                eventTypeRepository.save(eventType);
                log.info("自动注册新事件类型(DRAFT): eventType={}, publisher={}",
                        request.getEventType(), publisherCode);

                // 异步采样字段结构
                fieldSamplingService.sampleAsync(request.getEventType(), request.getPayload());

                // DRAFT 状态无活跃订阅，返回空分发列表
                return ResponseEntity.ok(IngestV2Response.accepted(request.getEventId(), List.of()));
            }
            if (!"ACTIVE".equals(eventType.getStatus())) {
                return ResponseEntity.badRequest().body(
                        IngestV2Response.rejected(request.getEventId(),
                                "事件类型未激活(当前状态: " + eventType.getStatus() + "): " + request.getEventType()));
            }
            // Validate event type belongs to this publisher
            if (!eventType.getPublisherCode().equals(publisherCode)) {
                return ResponseEntity.badRequest().body(
                        IngestV2Response.rejected(request.getEventId(), "事件类型不属于当前发布方"));
            }

            // 异步字段采样（不阻塞入队主路径）
            fieldSamplingService.sampleAsync(request.getEventType(), request.getPayload());

            // Generate traceId if not provided
            String traceId = request.getTraceId();
            if (traceId == null || traceId.isEmpty()) {
                traceId = "T-" + UUID.randomUUID();
            }

            // Determine targets
            List<Subscription> targets;
            if (request.getSubscriberCode() != null && !request.getSubscriberCode().isBlank()) {
                // Targeted delivery
                Subscription sub = subscriptionRepository.findBySubscriberAndEventType(
                        request.getSubscriberCode(), request.getEventType());
                if (sub == null || !"ACTIVE".equals(sub.getStatus())) {
                    return ResponseEntity.badRequest().body(
                            IngestV2Response.rejected(request.getEventId(),
                                    "订阅关系不存在或已暂停: " + request.getSubscriberCode()));
                }
                targets = List.of(sub);
            } else {
                // Fan-out: all active subscriptions
                targets = subscriptionRepository.findActiveByEventTypeCode(request.getEventType());
            }

            // Enqueue for each target
            List<DispatchDetail> dispatches = new ArrayList<>();
            for (Subscription sub : targets) {
                String status = ingestionService.enqueueForSubscriber(
                        request.getEventId(), sub.getSubscriberCode(),
                        request.getEventType(), request.getPayload(), traceId);
                dispatches.add(new DispatchDetail(sub.getSubscriberCode(), status));
            }

            return ResponseEntity.ok(IngestV2Response.accepted(request.getEventId(), dispatches));

        } catch (IngestionService.RedisUnavailableException e) {
            log.error("Redis 不可用(v2)，返回 503", e);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Retry-After", "5");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .headers(headers)
                    .body(IngestV2Response.rejected(request.getEventId(), "服务暂时不可用"));
        }
    }
}
