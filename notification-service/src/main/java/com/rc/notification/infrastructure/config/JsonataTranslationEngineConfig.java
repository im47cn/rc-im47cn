package com.rc.notification.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.domain.translation.JsonataTranslationEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JsonataTranslationEngine Bean 配置
 * <p>
 * 将领域层的纯计算引擎通过 infrastructure 配置类注册为 Spring Bean，
 * 避免领域层直接依赖 Spring 注解
 */
@Configuration
public class JsonataTranslationEngineConfig {

    @Bean
    public JsonataTranslationEngine jsonataTranslationEngine(ObjectMapper objectMapper) {
        return new JsonataTranslationEngine(objectMapper);
    }
}
