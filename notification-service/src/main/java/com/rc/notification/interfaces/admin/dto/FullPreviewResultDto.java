package com.rc.notification.interfaces.admin.dto;

import java.util.Map;

/**
 * 完整请求预览结果
 */
public class FullPreviewResultDto {

    private boolean success;
    private String resolvedUrl;
    private String httpMethod;
    private Map<String, String> headers;
    private String body;
    private String error;
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
