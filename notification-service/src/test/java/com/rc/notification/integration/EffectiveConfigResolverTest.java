package com.rc.notification.integration;

import com.rc.notification.domain.config.SupplierConfig;
import com.rc.notification.domain.subscription.EffectiveConfig;
import com.rc.notification.domain.subscription.EffectiveConfigResolver;
import com.rc.notification.domain.subscription.Subscription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EffectiveConfigResolverTest {

    @Test
    @DisplayName("No subscription: all fields from SubscriberConfig")
    void noSubscription_usesBaseConfig() {
        SupplierConfig base = buildBase();
        EffectiveConfig result = EffectiveConfigResolver.resolve(base, null);
        assertEquals("https://api.test.com", result.getBaseUrl());
        assertEquals("encrypted-cred", result.getCredentialsEncrypted());
        assertEquals("/default/path", result.getPathTemplate());
        assertEquals(3000, result.getConnectTimeoutMs());
        assertEquals(3, result.getMaxRetryCount());
    }

    @Test
    @DisplayName("Subscription overrides specific fields, inherits rest")
    void subscriptionOverrides_mergedCorrectly() {
        SupplierConfig base = buildBase();
        Subscription sub = new Subscription();
        sub.setPathTemplate("/override/path");
        sub.setReadTimeoutMs(10000);

        EffectiveConfig result = EffectiveConfigResolver.resolve(base, sub);
        assertEquals("/override/path", result.getPathTemplate());
        assertEquals(10000, result.getReadTimeoutMs());
        assertEquals("https://api.test.com", result.getBaseUrl());
        assertEquals("encrypted-cred", result.getCredentialsEncrypted());
        assertEquals(3000, result.getConnectTimeoutMs());
        assertEquals(3, result.getMaxRetryCount());
        assertEquals("'{\"test\": true}'", result.getBodyTemplate());
    }

    @Test
    @DisplayName("Subscription overrides all overridable fields")
    void subscriptionOverridesAll() {
        SupplierConfig base = buildBase();
        Subscription sub = new Subscription();
        sub.setPathTemplate("/sub/path");
        sub.setQueryTemplate("sub-query");
        sub.setHeaderTemplate("sub-header");
        sub.setBodyTemplate("sub-body");
        sub.setConnectTimeoutMs(1000);
        sub.setReadTimeoutMs(2000);
        sub.setMaxRetryCount(10);
        sub.setRetryBackoffInitialMs(500);
        sub.setRetryBackoffMultiplier(new BigDecimal("3.00"));
        sub.setRetryBackoffMaxMs(60000);
        sub.setSuccessHttpCodes("200,201");
        sub.setSuccessBodyPattern("ok");
        sub.setSuccessBodyMatchMode("CONTAINS");

        EffectiveConfig result = EffectiveConfigResolver.resolve(base, sub);
        // Channel-level always from base
        assertEquals("https://api.test.com", result.getBaseUrl());
        assertEquals("POST", result.getHttpMethod());
        assertEquals("APPLICATION_JSON", result.getContentTypeBehavior());
        assertEquals("encrypted-cred", result.getCredentialsEncrypted());
        // All overridable from subscription
        assertEquals("/sub/path", result.getPathTemplate());
        assertEquals("sub-query", result.getQueryTemplate());
        assertEquals("sub-header", result.getHeaderTemplate());
        assertEquals("sub-body", result.getBodyTemplate());
        assertEquals(1000, result.getConnectTimeoutMs());
        assertEquals(2000, result.getReadTimeoutMs());
        assertEquals(10, result.getMaxRetryCount());
        assertEquals(500, result.getRetryBackoffInitialMs());
        assertEquals(new BigDecimal("3.00"), result.getRetryBackoffMultiplier());
        assertEquals(60000, result.getRetryBackoffMaxMs());
        assertEquals("200,201", result.getSuccessHttpCodes());
        assertEquals("ok", result.getSuccessBodyPattern());
        assertEquals("CONTAINS", result.getSuccessBodyMatchMode());
    }

    @Test
    @DisplayName("toSupplierConfigCompat creates compatible SupplierConfig")
    void toSupplierConfigCompat_works() {
        SupplierConfig base = buildBase();
        EffectiveConfig result = EffectiveConfigResolver.resolve(base, null);
        SupplierConfig compat = result.toSupplierConfigCompat();
        assertEquals("TEST_SUB", compat.getSupplierCode());
        assertEquals("https://api.test.com", compat.getBaseUrl());
        assertEquals(1, compat.getStatus());
    }

    private SupplierConfig buildBase() {
        SupplierConfig c = new SupplierConfig();
        c.setSupplierCode("TEST_SUB");
        c.setBaseUrl("https://api.test.com");
        c.setHttpMethod("POST");
        c.setContentTypeBehavior("APPLICATION_JSON");
        c.setCredentialsEncrypted("encrypted-cred");
        c.setPathTemplate("/default/path");
        c.setQueryTemplate("default-query");
        c.setHeaderTemplate("default-header");
        c.setBodyTemplate("'{\"test\": true}'");
        c.setConnectTimeoutMs(3000);
        c.setReadTimeoutMs(5000);
        c.setSuccessHttpCodes("200");
        c.setSuccessBodyPattern(null);
        c.setSuccessBodyMatchMode("EQUALS");
        c.setSuccessCaseSensitive(1);
        c.setMaxRetryCount(3);
        c.setRetryBackoffInitialMs(1000);
        c.setRetryBackoffMultiplier(new BigDecimal("2.00"));
        c.setRetryBackoffMaxMs(30000);
        c.setWorkerConcurrency(2);
        c.setStatus(1);
        return c;
    }
}
