package com.rc.notification.infrastructure;

import com.rc.notification.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Redis queue integration tests with real Redis container.
 * <p>
 * Tests RBlockingQueue enqueue/dequeue and RDelayedQueue delayed delivery.
 */
@Tag("docker")
class RedisQueueIntegrationTest extends IntegrationTestBase {

    private static final String QUEUE_PREFIX = "queue:notification:";

    @Autowired
    private RedissonClient redissonClient;

    @Test
    @DisplayName("Enqueue to RQueue and dequeue in FIFO order")
    void enqueueDequeueFifoOrder() {
        String queueName = QUEUE_PREFIX + "TC_FIFO_TEST";
        RQueue<String> queue = redissonClient.getQueue(queueName);
        queue.delete(); // clean slate

        queue.add("{\"eventId\":\"evt-1\"}");
        queue.add("{\"eventId\":\"evt-2\"}");
        queue.add("{\"eventId\":\"evt-3\"}");

        assertEquals(3, queue.size());
        assertEquals("{\"eventId\":\"evt-1\"}", queue.poll());
        assertEquals("{\"eventId\":\"evt-2\"}", queue.poll());
        assertEquals("{\"eventId\":\"evt-3\"}", queue.poll());
        assertNull(queue.poll());
    }

    @Test
    @DisplayName("RBlockingQueue enqueue and dequeue")
    void blockingQueueEnqueueDequeue() throws InterruptedException {
        String queueName = QUEUE_PREFIX + "TC_BLOCKING_TEST";
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(queueName);
        blockingQueue.delete();

        blockingQueue.add("{\"eventId\":\"blocking-evt-1\"}");

        String item = blockingQueue.poll(1, TimeUnit.SECONDS);
        assertNotNull(item);
        assertEquals("{\"eventId\":\"blocking-evt-1\"}", item);

        // Empty queue poll returns null after timeout
        String empty = blockingQueue.poll(500, TimeUnit.MILLISECONDS);
        assertNull(empty);
    }

    @Test
    @DisplayName("RDelayedQueue delivers after delay")
    void delayedQueueDeliversAfterDelay() throws InterruptedException {
        String queueName = QUEUE_PREFIX + "TC_DELAYED_TEST";
        RBlockingQueue<String> destination = redissonClient.getBlockingQueue(queueName);
        destination.delete();

        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(destination);

        // Offer with 1 second delay
        delayedQueue.offer("{\"eventId\":\"delayed-evt-1\"}", 1, TimeUnit.SECONDS);

        // Should not be available immediately
        String immediate = destination.poll(200, TimeUnit.MILLISECONDS);
        assertNull(immediate);

        // Should be available after delay
        String delayed = destination.poll(2, TimeUnit.SECONDS);
        assertNotNull(delayed);
        assertEquals("{\"eventId\":\"delayed-evt-1\"}", delayed);

        delayedQueue.destroy();
    }

    @Test
    @DisplayName("Queue isolation: different suppliers have separate queues")
    void queueIsolationPerSupplier() {
        RQueue<String> queueA = redissonClient.getQueue(QUEUE_PREFIX + "TC_SUPPLIER_A");
        RQueue<String> queueB = redissonClient.getQueue(QUEUE_PREFIX + "TC_SUPPLIER_B");
        queueA.delete();
        queueB.delete();

        queueA.add("{\"supplier\":\"A\",\"id\":1}");
        queueA.add("{\"supplier\":\"A\",\"id\":2}");
        queueB.add("{\"supplier\":\"B\",\"id\":1}");

        assertEquals(2, queueA.size());
        assertEquals(1, queueB.size());

        assertEquals("{\"supplier\":\"A\",\"id\":1}", queueA.poll());
        assertEquals("{\"supplier\":\"B\",\"id\":1}", queueB.poll());
    }
}
