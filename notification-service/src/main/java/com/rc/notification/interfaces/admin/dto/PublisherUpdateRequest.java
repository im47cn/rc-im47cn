package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新发布方请求
 */
@Schema(description = "更新发布方请求")
public class PublisherUpdateRequest {

    @Schema(description = "发布方名称")
    private String publisherName;

    @Schema(description = "联系信息")
    private String contactInfo;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    // --- Getters & Setters ---

    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
