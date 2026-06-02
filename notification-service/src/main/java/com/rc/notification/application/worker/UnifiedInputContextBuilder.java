package com.rc.notification.application.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.domain.credential.CredentialVault;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一只读上下文构建器
 * <p>
 * 将事件荷载、供应商凭证、系统环境与全局追踪号聚合为一棵只读树（Immutable Tree），
 * 供 4 路 JSONata 引擎并发安全地调阅提取
 */
@Component
public class UnifiedInputContextBuilder {

    private final CredentialVault credentialVault;
    private final ObjectMapper objectMapper;

    public UnifiedInputContextBuilder(CredentialVault credentialVault, ObjectMapper objectMapper) {
        this.credentialVault = credentialVault;
        this.objectMapper = objectMapper;
    }

    /**
     * 从队列消息 JSON 和供应商凭证密文构建 UnifiedInputContext
     *
     * @param eventJson            队列中的事件 JSON 字符串
     * @param credentialsEncrypted 供应商凭证密文
     * @return 不可变的统一上下文 Map
     */
    public Map<String, Object> build(String eventJson, String credentialsEncrypted) {
        try {
            Map<String, Object> event = objectMapper.readValue(eventJson, new TypeReference<>() {});
            Map<String, Object> context = new HashMap<>(event);

            // 解密并注入 auth 子树
            Map<String, Object> auth = credentialVault.decrypt(credentialsEncrypted);
            context.put("auth", auth != null ? auth : Map.of());

            return Collections.unmodifiableMap(context);
        } catch (Exception e) {
            throw new RuntimeException("构建 UnifiedInputContext 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 UnifiedInputContext 序列化为 JSON 字符串（用于 DLQ 存储）
     */
    public String serialize(Map<String, Object> context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            throw new RuntimeException("序列化 UnifiedInputContext 失败: " + e.getMessage(), e);
        }
    }
}
