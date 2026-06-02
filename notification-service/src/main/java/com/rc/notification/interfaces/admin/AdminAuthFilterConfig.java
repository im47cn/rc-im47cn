package com.rc.notification.interfaces.admin;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 管理后台鉴权过滤器注册配置
 * <p>
 * 将 AdminAuthFilter 精确注册到 /api/v1/admin/* 路径，
 * 避免影响其他 API 路径（如 /api/v1/notifications/ingest）。
 */
@Configuration
public class AdminAuthFilterConfig {

    @Bean
    public FilterRegistrationBean<AdminAuthFilter> adminAuthFilterRegistration(AdminAuthFilter filter) {
        FilterRegistrationBean<AdminAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/v1/admin/*");
        registration.setOrder(1);
        registration.setName("adminAuthFilter");
        return registration;
    }
}
