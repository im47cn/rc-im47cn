package com.rc.notification.domain.subscription;

import com.rc.notification.domain.config.SupplierConfig;

/**
 * 合并 SubscriberConfig 与 Subscription 的配置解析器
 * <p>
 * 优先级：Subscription 覆盖字段 > SupplierConfig 基础字段（coalesce 语义）
 */
public final class EffectiveConfigResolver {

    private EffectiveConfigResolver() {}

    public static EffectiveConfig resolve(SupplierConfig base, Subscription sub) {
        if (sub == null) {
            return fromBase(base);
        }
        return EffectiveConfig.builder()
                // Channel-level: always from base
                .subscriberCode(base.getSupplierCode())
                .baseUrl(base.getBaseUrl())
                .httpMethod(base.getHttpMethod())
                .contentTypeBehavior(base.getContentTypeBehavior())
                .credentialsEncrypted(base.getCredentialsEncrypted())
                .workerConcurrency(base.getWorkerConcurrency())
                // Overridable: subscription > base
                .pathTemplate(coalesce(sub.getPathTemplate(), base.getPathTemplate()))
                .queryTemplate(coalesce(sub.getQueryTemplate(), base.getQueryTemplate()))
                .headerTemplate(coalesce(sub.getHeaderTemplate(), base.getHeaderTemplate()))
                .bodyTemplate(coalesce(sub.getBodyTemplate(), base.getBodyTemplate()))
                .connectTimeoutMs(coalesce(sub.getConnectTimeoutMs(), base.getConnectTimeoutMs()))
                .readTimeoutMs(coalesce(sub.getReadTimeoutMs(), base.getReadTimeoutMs()))
                .successHttpCodes(coalesce(sub.getSuccessHttpCodes(), base.getSuccessHttpCodes()))
                .successBodyPattern(coalesce(sub.getSuccessBodyPattern(), base.getSuccessBodyPattern()))
                .successBodyMatchMode(coalesce(sub.getSuccessBodyMatchMode(), base.getSuccessBodyMatchMode()))
                .successCaseSensitive(base.getSuccessCaseSensitive())
                .maxRetryCount(coalesce(sub.getMaxRetryCount(), base.getMaxRetryCount()))
                .retryBackoffInitialMs(coalesce(sub.getRetryBackoffInitialMs(), base.getRetryBackoffInitialMs()))
                .retryBackoffMultiplier(coalesce(sub.getRetryBackoffMultiplier(), base.getRetryBackoffMultiplier()))
                .retryBackoffMaxMs(coalesce(sub.getRetryBackoffMaxMs(), base.getRetryBackoffMaxMs()))
                .build();
    }

    private static EffectiveConfig fromBase(SupplierConfig base) {
        return EffectiveConfig.builder()
                .subscriberCode(base.getSupplierCode())
                .baseUrl(base.getBaseUrl())
                .httpMethod(base.getHttpMethod())
                .contentTypeBehavior(base.getContentTypeBehavior())
                .credentialsEncrypted(base.getCredentialsEncrypted())
                .workerConcurrency(base.getWorkerConcurrency())
                .pathTemplate(base.getPathTemplate())
                .queryTemplate(base.getQueryTemplate())
                .headerTemplate(base.getHeaderTemplate())
                .bodyTemplate(base.getBodyTemplate())
                .connectTimeoutMs(base.getConnectTimeoutMs())
                .readTimeoutMs(base.getReadTimeoutMs())
                .successHttpCodes(base.getSuccessHttpCodes())
                .successBodyPattern(base.getSuccessBodyPattern())
                .successBodyMatchMode(base.getSuccessBodyMatchMode())
                .successCaseSensitive(base.getSuccessCaseSensitive())
                .maxRetryCount(base.getMaxRetryCount())
                .retryBackoffInitialMs(base.getRetryBackoffInitialMs())
                .retryBackoffMultiplier(base.getRetryBackoffMultiplier())
                .retryBackoffMaxMs(base.getRetryBackoffMaxMs())
                .build();
    }

    private static <T> T coalesce(T override, T fallback) {
        return override != null ? override : fallback;
    }
}
