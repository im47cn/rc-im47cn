package com.rc.notification.infrastructure.http;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 全局 OkHttpClient 连接池配置
 * <p>
 * 提供全局共享的连接池实例，各供应商通过 newBuilder() 衍生专属超时客户端，
 * 零开销复用底层连接池与线程资源。
 */
@Configuration
public class OkHttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
    }
}
