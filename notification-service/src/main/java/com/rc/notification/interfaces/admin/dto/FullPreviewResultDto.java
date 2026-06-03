package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 完整请求预览结果
 */
@Schema(description = "完整请求预览结果")
public class FullPreviewResultDto {

    @Schema(description = "是否成功")
    private boolean success;
    @Schema(description = "解析后的完整URL")
    private String resolvedUrl;
    @Schema(description = "HTTP 方法")
    private String httpMethod;
    @Schema(description = "请求头")
    private Map<String, String> headers;
    @Schema(description = "请求体")
    private String body;
    @Schema(description = "错误信息")
    private String error;
    @Schema(description = "错误偏移量，-1 表示无错误")
    private int errorOffset = -1;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getResolvedUrl() { return resolvedUrl; }
    public void setResolvedUrl(String resolvedUrl) { this.resolvedUrl = resolvedUrl; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public int getErrorOffset() { return errorOffset; }
    public void setErrorOffset(int errorOffset) { this.errorOffset = errorOffset; }
}
