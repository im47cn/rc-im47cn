package com.rc.notification.infrastructure.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.domain.translation.JsonataTranslationEngine;
import com.rc.notification.infrastructure.persistence.entity.SupplierConfigEntity;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 全位置参数动态组装器
 * <p>
 * 接收计算引擎输出的 Path、Query、Header、Body 四路标准 JSON 数据，
 * 利用 OkHttp 机制像素级还原第三方网关所需的网络请求实体，
 * 并基于该渠道的特性动态裁剪出具备专属超时的轻量客户端。
 */
@Component
public class FullStackHttpRequestBuilder {

    private static final Logger log = LoggerFactory.getLogger(FullStackHttpRequestBuilder.class);

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String CONTENT_TYPE_FORM = "APPLICATION_FORM_URLENCODED";

    private final JsonataTranslationEngine translationEngine;
    private final ObjectMapper objectMapper;
    private final OkHttpClient globalClient;

    public FullStackHttpRequestBuilder(JsonataTranslationEngine translationEngine,
                                        ObjectMapper objectMapper,
                                        OkHttpClient globalClient) {
        this.translationEngine = translationEngine;
        this.objectMapper = objectMapper;
        this.globalClient = globalClient;
    }

    /**
     * 构建完整 HTTP 请求
     *
     * @param config       供应商配置
     * @param inputContext  统一只读上下文（UnifiedInputContext）
     * @return OkHttp Request 对象
     */
    public Request buildRequest(SupplierConfigEntity config, Object inputContext) throws Exception {
        // 1. 计算动态 Path
        String resolvedPath = evaluateTemplate(config.getPathTemplate(), inputContext);

        // 2. 构建 URL（base_url + path + query）
        String baseUrl = config.getBaseUrl();
        String fullUrl = buildFullUrl(baseUrl, resolvedPath);

        // 3. 计算 Query 参数并追加到 URL
        String queryJson = evaluateTemplate(config.getQueryTemplate(), inputContext);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(fullUrl).newBuilder();
        if (queryJson != null && !queryJson.isBlank()) {
            Map<String, Object> queryParams = objectMapper.readValue(queryJson, new TypeReference<>() {});
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(),
                        entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
            }
        }
        HttpUrl finalUrl = urlBuilder.build();

        // 4. 计算 Header
        String headerJson = evaluateTemplate(config.getHeaderTemplate(), inputContext);
        Request.Builder requestBuilder = new Request.Builder().url(finalUrl);

        if (headerJson != null && !headerJson.isBlank()) {
            Map<String, Object> headers = objectMapper.readValue(headerJson, new TypeReference<>() {});
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                if (entry.getValue() != null) {
                    requestBuilder.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        }

        // 5. 计算 Body 并根据 HTTP 方法与 Content-Type 行为组装请求体
        String method = config.getHttpMethod() != null ? config.getHttpMethod().toUpperCase() : "POST";
        RequestBody requestBody = buildRequestBody(config, inputContext);

        switch (method) {
            case "GET" -> requestBuilder.get();
            case "POST" -> requestBuilder.post(requestBody != null ? requestBody : emptyBody());
            case "PUT" -> requestBuilder.put(requestBody != null ? requestBody : emptyBody());
            case "PATCH" -> requestBuilder.patch(requestBody != null ? requestBody : emptyBody());
            case "DELETE" -> requestBuilder.delete(requestBody);
            default -> requestBuilder.method(method, requestBody);
        }

        return requestBuilder.build();
    }

    /**
     * 基于全局连接池动态衍生出包含特定 connect/read 超时的客户端
     *
     * @param config 供应商配置
     * @return 衍生的 OkHttpClient（共享全局连接池）
     */
    public OkHttpClient deriveClient(SupplierConfigEntity config) {
        int connectTimeout = config.getConnectTimeoutMs() != null ? config.getConnectTimeoutMs() : 3000;
        int readTimeout = config.getReadTimeoutMs() != null ? config.getReadTimeoutMs() : 5000;

        return globalClient.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 评估 JSONata 模板，空模板返回空字符串
     */
    private String evaluateTemplate(String template, Object inputContext) {
        if (template == null || template.isBlank()) {
            return "";
        }
        return translationEngine.transform(template, inputContext);
    }

    /**
     * 拼接 base_url 和动态 path
     */
    private String buildFullUrl(String baseUrl, String resolvedPath) {
        if (resolvedPath == null || resolvedPath.isBlank()) {
            return baseUrl;
        }
        // 去除 baseUrl 末尾斜杠和 path 开头斜杠，防止双斜杠
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = resolvedPath.startsWith("/") ? resolvedPath : "/" + resolvedPath;
        return base + path;
    }

    /**
     * 根据 Content-Type 行为构建请求体
     */
    private RequestBody buildRequestBody(SupplierConfigEntity config, Object inputContext) throws Exception {
        String bodyResult = evaluateTemplate(config.getBodyTemplate(), inputContext);
        if (bodyResult == null || bodyResult.isBlank()) {
            return null;
        }

        if (CONTENT_TYPE_FORM.equalsIgnoreCase(config.getContentTypeBehavior())) {
            // Form URL Encoded
            Map<String, Object> formParams = objectMapper.readValue(bodyResult, new TypeReference<>() {});
            FormBody.Builder formBuilder = new FormBody.Builder();
            for (Map.Entry<String, Object> entry : formParams.entrySet()) {
                formBuilder.add(entry.getKey(),
                        entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
            }
            return formBuilder.build();
        }

        // 默认 APPLICATION_JSON
        return RequestBody.create(bodyResult, JSON_MEDIA_TYPE);
    }

    /**
     * 创建空请求体（用于 POST/PUT/PATCH 无 body 场景）
     */
    private RequestBody emptyBody() {
        return RequestBody.create("", JSON_MEDIA_TYPE);
    }
}
