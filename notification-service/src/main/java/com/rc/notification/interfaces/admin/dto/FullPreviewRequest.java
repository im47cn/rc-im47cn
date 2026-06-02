package com.rc.notification.interfaces.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 完整请求预览仿真请求
 */
public class FullPreviewRequest {

    @NotBlank(message = "基础URL不能为空")
    private String baseUrl;

    private String httpMethod = "POST";
    private String contentTypeBehavior = "APPLICATION_JSON";
    private String pathTemplate;
    private String queryTemplate;
    private String headerTemplate;

    @NotBlank(message = "Body模板不能为空")
    private String bodyTemplate;

    @NotNull(message = "模拟输入上下文不能为空")
    private Map<String, Object> mockInputContext;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getContentTypeBehavior() { return contentTypeBehavior; }
    public void setContentTypeBehavior(String contentTypeBehavior) { this.contentTypeBehavior = contentTypeBehavior; }

    public String getPathTemplate() { return pathTemplate; }
    public void setPathTemplate(String pathTemplate) { this.pathTemplate = pathTemplate; }

    public String getQueryTemplate() { return queryTemplate; }
    public void setQueryTemplate(String queryTemplate) { this.queryTemplate = queryTemplate; }

    public String getHeaderTemplate() { return headerTemplate; }
    public void setHeaderTemplate(String headerTemplate) { this.headerTemplate = headerTemplate; }

    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }

    public Map<String, Object> getMockInputContext() { return mockInputContext; }
    public void setMockInputContext(Map<String, Object> mockInputContext) { this.mockInputContext = mockInputContext; }
}
