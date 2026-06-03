package com.rc.notification.interfaces.api;

import com.rc.notification.application.service.IngestionService;
import com.rc.notification.interfaces.api.dto.IngestResponse;
import com.rc.notification.interfaces.api.dto.NotificationEventDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 高并发事件接收端点
 * <p>
 * 毫秒级完成参数校验、分布式锁拦截、压入 Redisson 队列并回应上游
 */
@Tag(name = "事件摄取", description = "高并发事件接收端点")
@RestController
public class EventIngestionController {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionController.class);

    private final IngestionService ingestionService;

    public EventIngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Operation(summary = "接收通知事件", description = "校验参数、幂等拦截、压入 Redisson 队列并回应上游")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "事件已接收"),
            @ApiResponse(responseCode = "202", description = "幂等命中，事件已存在"),
            @ApiResponse(responseCode = "400", description = "请求参数校验失败"),
            @ApiResponse(responseCode = "409", description = "事件已进入死信队列"),
            @ApiResponse(responseCode = "503", description = "Redis 不可用，请稍后重试")
    })
    @PostMapping("/api/v1/notifications/ingest")
    public ResponseEntity<IngestResponse> ingestEvent(@Valid @RequestBody NotificationEventDto eventDto) {
        try {
            IngestResponse response = ingestionService.ingest(eventDto);

            // 根据响应状态返回不同 HTTP 状态码
            return switch (response.getStatus()) {
                case "ACCEPTED" -> ResponseEntity.ok(response);
                case "IDEMPOTENT_HIT" -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
                case "DEAD_LETTERED" -> ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                case "REJECTED" -> ResponseEntity.badRequest().body(response);
                default -> ResponseEntity.ok(response);
            };
        } catch (IngestionService.RedisUnavailableException e) {
            log.error("Redis 不可用，返回 503", e);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Retry-After", "5");
            IngestResponse errorResponse = IngestResponse.rejected(
                    eventDto.getEventId(), "服务暂时不可用，请稍后重试");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .headers(headers)
                    .body(errorResponse);
        }
    }
}
