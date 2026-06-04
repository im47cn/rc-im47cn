package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 发布方响应 DTO
 */
@Schema(description = "发布方响应")
public class PublisherDto {

    @Schema(description = "发布方ID")
    private Long id;
    @Schema(description = "发布方编码")
    private String publisherCode;
    @Schema(description = "发布方名称")
    private String publisherName;
    @Schema(description = "API Key")
    private String apiKey;
    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;
    @Schema(description = "联系信息")
    private String contactInfo;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPublisherCode() { return publisherCode; }
    public void setPublisherCode(String publisherCode) { this.publisherCode = publisherCode; }

    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
