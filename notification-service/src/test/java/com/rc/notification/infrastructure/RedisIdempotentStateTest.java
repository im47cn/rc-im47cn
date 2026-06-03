package com.rc.notification.infrastructure;

import com.rc.notification.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redis idempotent state lifecycle integration tests with real Redis container.
 * <p>
 * Tests the actual Redis state transitions used by the ingestion pipeline:
 * PROCESSING -> SUCCESS (TTL shrink) and PROCESSING -> DEAD_LETTERED.
 */
@Tag("docker")
class RedisIdempotentStateTest extends IntegrationTestBase {

    private static final String STATUS_PREFIX = "status:dispatch:";

    @Autowired
    private RedissonClient redissonClient;

    @Test
    @DisplayName("Set PROCESSING state with TTL")
    void setProcessingStateWithTtl() {
        String key = STATUS_PREFIX + "redis-test-001";
        RBucket<String> bucket = redissonClient.getBucket(key);

        bucket.set("PROCESSING", Duration.ofHours(24));

        assertEquals("PROCESSING", bucket.get());
        assertTrue(bucket.remainTimeToLive() > 0);
    }

    @Test
    @DisplayName("Transition from PROCESSING to SUCCESS with TTL shrink")
    void transitionToSuccessWithTtlShrink() {
        String key = STATUS_PREFIX + "redis-test-002";
        RBucket<String> bucket = redissonClient.getBucket(key);

        // Set PROCESSING with 24h TTL
        bucket.set("PROCESSING", Duration.ofHours(24));
        long initialTtl = bucket.remainTimeToLive();
        assertTrue(initialTtl > Duration.ofHours(23).toMillis());

        // Transition to SUCCESS with 1h TTL (shrink)
        bucket.set("SUCCESS", Duration.ofHours(1));

        assertEquals("SUCCESS", bucket.get());
        long newTtl = bucket.remainTimeToLive();
        assertTrue(newTtl <= Duration.ofHours(1).toMillis());
        assertTrue(newTtl > 0);
    }

    @Test
    @DisplayName("Transition from PROCESSING to DEAD_LETTERED preserves TTL")
    void transitionToDeadLetteredPreservesTtl() {
        String key = STATUS_PREFIX + "redis-test-003";
        RBucket<String> bucket = redissonClient.getBucket(key);

        // Set PROCESSING with 24h TTL
        bucket.set("PROCESSING", Duration.ofHours(24));

        // Transition to DEAD_LETTERED, keep 24h TTL
        bucket.set("DEAD_LETTERED", Duration.ofHours(24));

        assertEquals("DEAD_LETTERED", bucket.get());
        long ttl = bucket.remainTimeToLive();
        assertTrue(ttl > Duration.ofHours(23).toMillis());
    }

    @Test
    @DisplayName("Non-existent key returns null")
    void nonExistentKeyReturnsNull() {
        String key = STATUS_PREFIX + "redis-test-nonexistent";
        RBucket<String> bucket = redissonClient.getBucket(key);

        assertNull(bucket.get());
    }

    @Test
    @DisplayName("Delete state key works correctly")
    void deleteStateKeyWorks() {
        String key = STATUS_PREFIX + "redis-test-004";
        RBucket<String> bucket = redissonClient.getBucket(key);

        bucket.set("PROCESSING", Duration.ofHours(24));
        assertEquals("PROCESSING", bucket.get());

        bucket.delete();
        assertNull(bucket.get());
    }
}
