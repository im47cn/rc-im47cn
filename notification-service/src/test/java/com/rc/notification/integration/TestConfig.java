package com.rc.notification.integration;

import org.redisson.api.RBucket;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test configuration providing mock RedissonClient.
 * <p>
 * All mock objects are pre-created eagerly in the @Bean method to avoid
 * Mockito inline mocking issues on newer JDK versions (Java 26+).
 * Uses keyed routing to dispatch operations to correct in-memory stores.
 */
@TestConfiguration
public class TestConfig {

    static {
        // Enable Byte Buddy experimental mode for Java 26+ compatibility
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    /** In-memory store simulating Redis key-value buckets */
    private final ConcurrentHashMap<String, Object> redisStore = new ConcurrentHashMap<>();

    /** In-memory store simulating Redis queues */
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> queueStore = new ConcurrentHashMap<>();

    /** Current bucket key for mock routing (thread-local for safety) */
    private final ThreadLocal<String> currentBucketKey = new ThreadLocal<>();

    /** Current queue name for mock routing */
    private final ThreadLocal<String> currentQueueName = new ThreadLocal<>();

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedissonClient redissonClient() {
        RedissonClient client = mock(RedissonClient.class);

        // ============ Pre-create ONE reusable bucket mock ============
        RBucket<Object> bucketMock = mock(RBucket.class);

        when(bucketMock.get()).thenAnswer(inv -> {
            String key = currentBucketKey.get();
            return key != null ? redisStore.get(key) : null;
        });

        doAnswer(inv -> {
            String key = currentBucketKey.get();
            Object value = inv.getArgument(0);
            if (key != null) redisStore.put(key, value);
            return null;
        }).when(bucketMock).set(any());

        doAnswer(inv -> {
            String key = currentBucketKey.get();
            Object value = inv.getArgument(0);
            if (key != null) redisStore.put(key, value);
            return null;
        }).when(bucketMock).set(any(), any());

        when(bucketMock.remainTimeToLive()).thenReturn(86400000L);
        lenient().when(bucketMock.expire(any(java.time.Duration.class))).thenReturn(true);
        lenient().when(bucketMock.expire(any(java.time.Instant.class))).thenReturn(true);

        when(client.getBucket(anyString())).thenAnswer(inv -> {
            currentBucketKey.set(inv.getArgument(0));
            return bucketMock;
        });

        // ============ Pre-create ONE reusable queue mock ============
        RQueue<String> queueMock = mock(RQueue.class);

        when(queueMock.add(anyString())).thenAnswer(inv -> {
            String name = currentQueueName.get();
            String msg = inv.getArgument(0);
            if (name != null) {
                queueStore.computeIfAbsent(name, k -> new ConcurrentLinkedQueue<>()).add(msg);
            }
            return true;
        });
        when(queueMock.poll()).thenAnswer(inv -> {
            String name = currentQueueName.get();
            ConcurrentLinkedQueue<String> q = name != null ? queueStore.get(name) : null;
            return q != null ? q.poll() : null;
        });
        when(queueMock.size()).thenAnswer(inv -> {
            String name = currentQueueName.get();
            ConcurrentLinkedQueue<String> q = name != null ? queueStore.get(name) : null;
            return q != null ? q.size() : 0;
        });

        when(client.getQueue(anyString())).thenAnswer(inv -> {
            currentQueueName.set(inv.getArgument(0));
            return queueMock;
        });

        // ============ Pre-create lock mock ============
        RLock lockMock = mock(RLock.class);
        try {
            when(lockMock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            // won't happen in mock setup
        }
        when(lockMock.isHeldByCurrentThread()).thenReturn(true);
        when(client.getLock(anyString())).thenReturn(lockMock);

        // ============ Pre-create blocking queue & delayed queue mocks ============
        RBlockingQueue<String> blockingQueueMock = mock(RBlockingQueue.class);
        when(client.getBlockingQueue(anyString())).thenAnswer(inv -> blockingQueueMock);

        RDelayedQueue<String> delayedQueueMock = mock(RDelayedQueue.class);
        when(client.getDelayedQueue(any(RBlockingQueue.class))).thenAnswer(inv -> delayedQueueMock);

        // ============ Pre-create topic mock ============
        RTopic topicMock = mock(RTopic.class);
        when(topicMock.addListener(any(Class.class), any())).thenReturn(1);
        when(topicMock.publish(any())).thenReturn(1L);
        when(client.getTopic(anyString())).thenReturn(topicMock);

        return client;
    }

    public ConcurrentHashMap<String, Object> getRedisStore() {
        return redisStore;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> getQueueStore() {
        return queueStore;
    }

    /** Clear all in-memory state between tests */
    public void clearAll() {
        redisStore.clear();
        queueStore.clear();
    }
}
