package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 完整请求预览仿真请求
 */
@Schema(description = "完整请求预览仿真请求")
public class FullPreviewRequest {

    @Schema(description = "基础URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "基础URL不能为空")
    private String baseUrl;

    @Schema(description = "HTTP 方法", defaultValue = "POST")
    private String httpMethod = "POST";
    @Schema(description = "Content-Type 行为", defaultValue = "APPLICATION_JSON")
    private String contentTypeBehavior = "APPLICATION_JSON";
    @Schema(description = "路径模板 (JSONata)")
    private String pathTemplate;
    @Schema(description = "查询参数模板 (JSONata)")
    private String queryTemplate;
    @Schema(description = "请求头模板 (JSONata)")
    private String headerTemplate;

    @Schema(description = "请求体模板 (JSONata)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Body模板不能为空")
    private String bodyTemplate;

    @Schema(description = "模拟输入上下文", requiredMode = Schema.RequiredMode.REQUIRED)
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
