package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 事件类型响应 DTO
 */
@Schema(description = "事件类型响应")
public class EventTypeDto {

    @Schema(description = "事件类型ID")
    private Long id;
    @Schema(description = "事件类型编码")
    private String eventTypeCode;
    @Schema(description = "发布方编码")
    private String publisherCode;
    @Schema(description = "显示名称")
    private String displayName;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "Payload Schema")
    private String payloadSchema;
    @Schema(description = "状态: DRAFT / ACTIVE / DEPRECATED")
    private String status;
    @Schema(description = "版本号")
    private Integer version;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventTypeCode() { return eventTypeCode; }
    public void setEventTypeCode(String eventTypeCode) { this.eventTypeCode = eventTypeCode; }

    public String getPublisherCode() { return publisherCode; }
    public void setPublisherCode(String publisherCode) { this.publisherCode = publisherCode; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPayloadSchema() { return payloadSchema; }
    public void setPayloadSchema(String payloadSchema) { this.payloadSchema = payloadSchema; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
