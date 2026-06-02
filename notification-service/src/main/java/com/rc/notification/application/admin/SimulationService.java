package com.rc.notification.application.admin;

import com.rc.notification.domain.translation.JsonataTranslationEngine;
import com.rc.notification.domain.translation.TranslationEngineException;
import com.rc.notification.interfaces.admin.dto.FullPreviewRequest;
import com.rc.notification.interfaces.admin.dto.FullPreviewResultDto;
import com.rc.notification.interfaces.admin.dto.SimulationResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSONata 在线仿真服务
 * <p>
 * 提供单表达式转换和完整请求预览两个核心能力，
 * 阻断错误配置上线。
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final JsonataTranslationEngine translationEngine;

    public SimulationService(JsonataTranslationEngine translationEngine) {
        this.translationEngine = translationEngine;
    }

    /**
     * 单表达式仿真转换
     */
    public SimulationResultDto simulate(String jsonataExpression, Map<String, Object> mockInputContext) {
        try {
            String result = translationEngine.transform(jsonataExpression, mockInputContext);
            return SimulationResultDto.ok(result);
        } catch (TranslationEngineException e) {
            return SimulationResultDto.fail(e.getMessage(), e.getErrorOffset());
        } catch (Exception e) {
            return SimulationResultDto.fail("仿真执行失败: " + e.getMessage(), -1);
        }
    }

    /**
     * 完整请求预览
     * <p>
     * 评估四路 JSONata 模板，组装完整 HTTP 请求预览（不实际发送网络请求）
     */
    public FullPreviewResultDto fullPreview(FullPreviewRequest request) {
        FullPreviewResultDto result = new FullPreviewResultDto();
        Map<String, Object> context = request.getMockInputContext();

        try {
            // 1. 解析 Path
            String resolvedPath = evaluateOrEmpty(request.getPathTemplate(), context);
            String baseUrl = request.getBaseUrl();
            String fullUrl = buildFullUrl(baseUrl, resolvedPath);

            // 2. 解析 Query 参数并追加到 URL
            String queryJson = evaluateOrEmpty(request.getQueryTemplate(), context);
            if (queryJson != null && !queryJson.isBlank()) {
                StringBuilder sb = new StringBuilder(fullUrl);
                sb.append(fullUrl.contains("?") ? "&" : "?");
                sb.append("(query params from: ").append(queryJson).append(")");
                fullUrl = sb.toString();
                // 实际解析 query JSON 为 URL 参数
                fullUrl = appendQueryParams(baseUrl, resolvedPath, queryJson);
            }

            result.setResolvedUrl(fullUrl);
            result.setHttpMethod(request.getHttpMethod() != null ? request.getHttpMethod().toUpperCase() : "POST");

            // 3. 解析 Header
            Map<String, String> headers = new LinkedHashMap<>();
            String contentType = "APPLICATION_FORM_URLENCODED".equalsIgnoreCase(request.getContentTypeBehavior())
                    ? "application/x-www-form-urlencoded" : "application/json";
            headers.put("Content-Type", contentType);

            String headerJson = evaluateOrEmpty(request.getHeaderTemplate(), context);
            if (headerJson != null && !headerJson.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> headerMap = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(headerJson, Map.class);
                    headerMap.forEach((k, v) -> {
                        if (v != null) headers.put(k, String.valueOf(v));
                    });
                } catch (Exception e) {
                    headers.put("_header_parse_error", e.getMessage());
                }
            }
            result.setHeaders(headers);

            // 4. 解析 Body
            String body = evaluateOrEmpty(request.getBodyTemplate(), context);
            result.setBody(body);

            result.setSuccess(true);
            return result;

        } catch (TranslationEngineException e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
            result.setErrorOffset(e.getErrorOffset());
            return result;
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("预览执行失败: " + e.getMessage());
            return result;
        }
    }

    private String evaluateOrEmpty(String template, Map<String, Object> context) {
        if (template == null || template.isBlank()) {
            return "";
        }
        return translationEngine.transform(template, context);
    }

    private String buildFullUrl(String baseUrl, String resolvedPath) {
        if (resolvedPath == null || resolvedPath.isBlank()) {
            return baseUrl;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = resolvedPath.startsWith("/") ? resolvedPath : "/" + resolvedPath;
        return base + path;
    }

    @SuppressWarnings("unchecked")
    private String appendQueryParams(String baseUrl, String resolvedPath, String queryJson) {
        String fullUrl = buildFullUrl(baseUrl, resolvedPath);
        try {
            Map<String, Object> queryParams = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(queryJson, Map.class);
            StringBuilder sb = new StringBuilder(fullUrl);
            boolean first = !fullUrl.contains("?");
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                sb.append(first ? "?" : "&");
                sb.append(entry.getKey()).append("=")
                  .append(entry.getValue() != null ? entry.getValue() : "");
                first = false;
            }
            return sb.toString();
        } catch (Exception e) {
            return fullUrl + "?_query_parse_error=" + e.getMessage();
        }
    }
}
