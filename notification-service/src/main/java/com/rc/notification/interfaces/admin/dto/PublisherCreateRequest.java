package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 新增发布方请求
 */
@Schema(description = "新增发布方请求")
public class PublisherCreateRequest {

    @Schema(description = "发布方编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "publisherCode 不能为空")
    private String publisherCode;

    @Schema(description = "发布方名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "publisherName 不能为空")
    private String publisherName;

    @Schema(description = "联系信息")
    private String contactInfo;

    // --- Getters & Setters ---

    public String getPublisherCode() { return publisherCode; }
    public void setPublisherCode(String publisherCode) { this.publisherCode = publisherCode; }

    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
}
