package com.rc.notification.infrastructure.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rc.notification.application.event.SupplierConfigActivatedEvent;
import com.rc.notification.application.event.SupplierConfigDeactivatedEvent;
import com.rc.notification.domain.config.SupplierConfig;
import com.rc.notification.domain.config.SupplierConfigDomainService;
import com.rc.notification.infrastructure.persistence.entity.SupplierConfigEntity;
import com.rc.notification.infrastructure.persistence.mapper.SupplierConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 供应商配置三层缓存管理器
 * <p>
 * 1. Redis Pub/Sub 广播驱逐（由 ConfigEvictionListener 触发）
 * 2. 60秒看门狗定时扫描 update_time 兜底
 * 3. Caffeine 10分钟 expireAfterWrite 底线失效
 */
@Service
public class SupplierConfigCacheManager implements SupplierConfigDomainService {

    private static final Logger log = LoggerFactory.getLogger(SupplierConfigCacheManager.class);

    private final SupplierConfigMapper supplierConfigMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 单供应商缓存：supplierCode -> SupplierConfig
     */
    private final Cache<String, SupplierConfig> configCache;

    /**
     * 全量活跃配置列表缓存
     */
    private final Cache<String, List<SupplierConfig>> activeListCache;

    private static final String ACTIVE_LIST_KEY = "ALL_ACTIVE";

    /**
     * 看门狗上次扫描时间戳
     */
    private final AtomicReference<LocalDateTime> lastWatchdogScanTime = new AtomicReference<>(LocalDateTime.now());

    public SupplierConfigCacheManager(SupplierConfigMapper supplierConfigMapper,
                                      ApplicationEventPublisher eventPublisher) {
        this.supplierConfigMapper = supplierConfigMapper;
        this.eventPublisher = eventPublisher;

        this.configCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();

        this.activeListCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1)
                .build();
    }

    @Override
    public SupplierConfig getBySupplierCode(String supplierCode) {
        return configCache.get(supplierCode, this::loadFromDb);
    }

    @Override
    public List<SupplierConfig> getAllActive() {
        return activeListCache.get(ACTIVE_LIST_KEY, key -> loadAllActiveFromDb());
    }

    @Override
    public void evictCache(String supplierCode) {
        log.info("精准驱逐供应商缓存: supplierCode={}", supplierCode);
        configCache.invalidate(supplierCode);
        activeListCache.invalidate(ACTIVE_LIST_KEY);

        // Cache Miss 回源加载，根据状态发布启用/停用事件
        SupplierConfig config = getBySupplierCode(supplierCode);
        if (config != null) {
            publishConfigChangeEvent(config);
        }
    }

    @Override
    public void evictAllCache() {
        log.info("全量驱逐供应商缓存");
        configCache.invalidateAll();
        activeListCache.invalidate(ACTIVE_LIST_KEY);
    }

    /**
     * 60秒看门狗定时扫描 update_time 兜底
     * <p>
     * 扫描自上次检查以来有变更的配置，逐个驱逐缓存并触发事件
     */
    @Scheduled(fixedDelay = 60000)
    public void watchdogScan() {
        LocalDateTime lastScan = lastWatchdogScanTime.get();
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<SupplierConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(SupplierConfigEntity::getUpdateTime, lastScan);

        List<SupplierConfigEntity> changedEntities = supplierConfigMapper.selectList(wrapper);
        if (!changedEntities.isEmpty()) {
            log.info("看门狗扫描发现 {} 条配置变更", changedEntities.size());
            for (SupplierConfigEntity entity : changedEntities) {
                SupplierConfig config = SupplierConfig.fromEntity(entity);
                configCache.invalidate(config.getSupplierCode());
                // 重新加载并放入缓存
                configCache.put(config.getSupplierCode(), config);
                publishConfigChangeEvent(config);
            }
            activeListCache.invalidate(ACTIVE_LIST_KEY);
        }

        lastWatchdogScanTime.set(now);
    }

    /**
     * 根据配置状态发布对应的领域事件
     */
    private void publishConfigChangeEvent(SupplierConfig config) {
        if (config.getStatus() != null && config.getStatus() == 1) {
            eventPublisher.publishEvent(new SupplierConfigActivatedEvent(this, config));
        } else {
            eventPublisher.publishEvent(new SupplierConfigDeactivatedEvent(this, config.getSupplierCode()));
        }
    }

    /**
     * 从数据库加载单个供应商配置
     */
    private SupplierConfig loadFromDb(String supplierCode) {
        LambdaQueryWrapper<SupplierConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierConfigEntity::getSupplierCode, supplierCode);
        return SupplierConfig.fromEntity(supplierConfigMapper.selectOne(wrapper));
    }

    /**
     * 从数据库加载所有启用状态的供应商配置
     */
    private List<SupplierConfig> loadAllActiveFromDb() {
        LambdaQueryWrapper<SupplierConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplierConfigEntity::getStatus, 1);
        return supplierConfigMapper.selectList(wrapper).stream()
                .map(SupplierConfig::fromEntity)
                .toList();
    }
}
