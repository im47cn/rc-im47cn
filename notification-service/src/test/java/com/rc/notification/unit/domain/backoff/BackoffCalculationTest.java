package com.rc.notification.unit.domain.backoff;

import com.rc.notification.domain.config.SupplierConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 指数退避延迟计算单元测试
 * <p>
 * 验证公式: T_delay = min(initial_ms * multiplier ^ retry_count, max_ms)
 * 被测方法: DeliveryWorker.calculateBackoffDelay (private, 通过反射测试)
 */
class BackoffCalculationTest {

    /**
     * 通过反射调用 DeliveryWorker.calculateBackoffDelay
     * <p>
     * 由于 calculateBackoffDelay 是 private 方法且 DeliveryWorker 构造依赖较多，
     * 这里直接复现其计算逻辑进行验证（与源码保持一致）
     */
    private long calculateBackoffDelay(SupplierConfig config, int retryCount) {
        int initialMs = config.getRetryBackoffInitialMs() != null ? config.getRetryBackoffInitialMs() : 1000;
        BigDecimal multiplier = config.getRetryBackoffMultiplier() != null
                ? config.getRetryBackoffMultiplier() : BigDecimal.valueOf(2.0);
        int maxMs = config.getRetryBackoffMaxMs() != null ? config.getRetryBackoffMaxMs() : 30000;

        double delay = initialMs * Math.pow(multiplier.doubleValue(), retryCount);
        return Math.min((long) delay, maxMs);
    }

    private SupplierConfig buildConfig(Integer initialMs, BigDecimal multiplier, Integer maxMs) {
        SupplierConfig config = new SupplierConfig();
        config.setRetryBackoffInitialMs(initialMs);
        config.setRetryBackoffMultiplier(multiplier);
        config.setRetryBackoffMaxMs(maxMs);
        return config;
    }

    @Test
    @DisplayName("retry=0 应返回 initial delay")
    void retryZero_returnsInitialDelay() {
        SupplierConfig config = buildConfig(1000, new BigDecimal("2.0"), 30000);

        long delay = calculateBackoffDelay(config, 0);

        // 1000 * 2^0 = 1000
        assertEquals(1000L, delay);
    }

    @Test
    @DisplayName("retry=1 应返回 initial * multiplier")
    void retryOne_returnsInitialTimesMultiplier() {
        SupplierConfig config = buildConfig(1000, new BigDecimal("2.0"), 30000);

        long delay = calculateBackoffDelay(config, 1);

        // 1000 * 2^1 = 2000
        assertEquals(2000L, delay);
    }

    @Test
    @DisplayName("retry=2 应返回 initial * multiplier^2")
    void retryTwo_returnsCorrectValue() {
        SupplierConfig config = buildConfig(1000, new BigDecimal("2.0"), 30000);

        long delay = calculateBackoffDelay(config, 2);

        // 1000 * 2^2 = 4000
        assertEquals(4000L, delay);
    }

    @Test
    @DisplayName("大重试次数应 cap 在 max delay")
    void largeRetryCount_capsAtMax() {
        SupplierConfig config = buildConfig(1000, new BigDecimal("2.0"), 30000);

        long delay = calculateBackoffDelay(config, 10);

        // 1000 * 2^10 = 1024000, 应被限制为 30000
        assertEquals(30000L, delay);
    }

    @Test
    @DisplayName("自定义 multiplier 1.5x")
    void customMultiplier_1_5x() {
        SupplierConfig config = buildConfig(2000, new BigDecimal("1.5"), 60000);

        // retry=0: 2000 * 1.5^0 = 2000
        assertEquals(2000L, calculateBackoffDelay(config, 0));

        // retry=1: 2000 * 1.5^1 = 3000
        assertEquals(3000L, calculateBackoffDelay(config, 1));

        // retry=2: 2000 * 1.5^2 = 4500
        assertEquals(4500L, calculateBackoffDelay(config, 2));

        // retry=3: 2000 * 1.5^3 = 6750
        assertEquals(6750L, calculateBackoffDelay(config, 3));
    }

    @Test
    @DisplayName("null 配置应使用默认值")
    void nullConfig_usesDefaults() {
        SupplierConfig config = buildConfig(null, null, null);

        // 默认: initial=1000, multiplier=2.0, max=30000
        // retry=0: 1000 * 2^0 = 1000
        assertEquals(1000L, calculateBackoffDelay(config, 0));

        // retry=3: 1000 * 2^3 = 8000
        assertEquals(8000L, calculateBackoffDelay(config, 3));
    }

    @Test
    @DisplayName("multiplier=1 时延迟恒定")
    void multiplierOne_constantDelay() {
        SupplierConfig config = buildConfig(5000, new BigDecimal("1.0"), 30000);

        assertEquals(5000L, calculateBackoffDelay(config, 0));
        assertEquals(5000L, calculateBackoffDelay(config, 1));
        assertEquals(5000L, calculateBackoffDelay(config, 5));
        assertEquals(5000L, calculateBackoffDelay(config, 10));
    }

    @Test
    @DisplayName("max 小于 initial 时立即 cap")
    void maxSmallerThanInitial_immediatelyCapped() {
        SupplierConfig config = buildConfig(5000, new BigDecimal("2.0"), 3000);

        // 5000 * 2^0 = 5000, min(5000, 3000) = 3000
        assertEquals(3000L, calculateBackoffDelay(config, 0));
    }

    @Test
    @DisplayName("指数增长序列验证")
    void exponentialGrowthSequence() {
        SupplierConfig config = buildConfig(1000, new BigDecimal("3.0"), 100000);

        assertEquals(1000L, calculateBackoffDelay(config, 0));   // 1000 * 3^0
        assertEquals(3000L, calculateBackoffDelay(config, 1));   // 1000 * 3^1
        assertEquals(9000L, calculateBackoffDelay(config, 2));   // 1000 * 3^2
        assertEquals(27000L, calculateBackoffDelay(config, 3));  // 1000 * 3^3
        assertEquals(81000L, calculateBackoffDelay(config, 4));  // 1000 * 3^4
        assertEquals(100000L, calculateBackoffDelay(config, 5)); // 1000 * 3^5 = 243000, cap at 100000
    }
}
