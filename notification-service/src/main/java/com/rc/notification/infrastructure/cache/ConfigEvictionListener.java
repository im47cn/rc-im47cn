package com.rc.notification.infrastructure.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.domain.config.SupplierConfigDomainService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 缓存驱逐监听器
 * <p>
 * 订阅 Topic: Notification:Config:Evict
 * 接收管理后台广播的配置变更事件，精准驱逐对应供应商的本地缓存
 */
@Component
public class ConfigEvictionListener {

    private static final Logger log = LoggerFactory.getLogger(ConfigEvictionListener.class);
    private static final String EVICT_TOPIC = "Notification:Config:Evict";

    private final RedissonClient redissonClient;
    private final SupplierConfigDomainService configDomainService;
    private final ObjectMapper objectMapper;

    private int listenerId;

    public ConfigEvictionListener(RedissonClient redissonClient,
                                  SupplierConfigDomainService configDomainService,
                                  ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.configDomainService = configDomainService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void subscribe() {
        RTopic topic = redissonClient.getTopic(EVICT_TOPIC);
        listenerId = topic.addListener(String.class, (channel, message) -> {
            try {
                JsonNode node = objectMapper.readTree(message);
                String supplierCode = node.path("supplierCode").asText(null);
                String action = node.path("action").asText("UNKNOWN");
                log.info("收到缓存驱逐通知: supplierCode={}, action={}", supplierCode, action);

                if (supplierCode != null && !supplierCode.isEmpty()) {
                    configDomainService.evictCache(supplierCode);
                }
            } catch (Exception e) {
                log.error("解析缓存驱逐消息失败: {}", message, e);
            }
        });
        log.info("已订阅 Redis Pub/Sub Topic: {}", EVICT_TOPIC);
    }

    @PreDestroy
    public void unsubscribe() {
        try {
            RTopic topic = redissonClient.getTopic(EVICT_TOPIC);
            topic.removeListener(listenerId);
            log.info("已取消订阅 Redis Pub/Sub Topic: {}", EVICT_TOPIC);
        } catch (Exception e) {
            log.warn("取消订阅 Pub/Sub 失败", e);
        }
    }

    /**
     * 广播配置变更驱逐事件
     *
     * @param supplierCode 供应商编码
     * @param action       操作类型: CREATE, UPDATE, DELETE
     */
    public void publishEviction(String supplierCode, String action) {
        try {
            String message = objectMapper.writeValueAsString(
                    java.util.Map.of("supplierCode", supplierCode, "action", action)
            );
            RTopic topic = redissonClient.getTopic(EVICT_TOPIC);
            topic.publish(message);
            log.info("广播缓存驱逐事件: supplierCode={}, action={}", supplierCode, action);
        } catch (Exception e) {
            log.error("广播缓存驱逐事件失败: supplierCode={}", supplierCode, e);
        }
    }
}
