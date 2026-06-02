package com.rc.notification.infrastructure.health;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Redis 连接健康检查
 * <p>
 * 用于 Readiness Probe 组合检查，Redis 不可达时返回 DOWN
 */
@Component("redisHealthCheck")
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(RedisHealthIndicator.class);

    private final RedissonClient redissonClient;

    public RedisHealthIndicator(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Health health() {
        try {
            // 尝试 ping Redis
            redissonClient.getBucket("health:ping").get();
            return Health.up()
                    .withDetail("redis", "connected")
                    .build();
        } catch (Exception e) {
            log.warn("Redis 健康检查失败: {}", e.getMessage());
            return Health.down()
                    .withDetail("redis", "disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
