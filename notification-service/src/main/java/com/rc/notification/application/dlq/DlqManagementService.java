package com.rc.notification.application.dlq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.application.service.IngestionService;
import com.rc.notification.infrastructure.persistence.entity.NotificationDlqLogEntity;
import com.rc.notification.infrastructure.persistence.mapper.NotificationDlqLogMapper;
import com.rc.notification.interfaces.admin.dto.BatchResult;
import com.rc.notification.interfaces.admin.dto.DlqLogDto;
import com.rc.notification.interfaces.admin.dto.PageResult;
import org.redisson.api.RBucket;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 死信队列管理服务
 * <p>
 * 提供死信记录的分页查询、单条/批量重试、标记忽略能力。
 */
@Service
public class DlqManagementService {

    private static final Logger log = LoggerFactory.getLogger(DlqManagementService.class);

    private static final String STATUS_PREFIX = "status:dispatch:";
    private static final String QUEUE_PREFIX = "queue:notification:";
    private static final long STATUS_TTL_HOURS = 24;

    private final NotificationDlqLogMapper dlqLogMapper;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public DlqManagementService(NotificationDlqLogMapper dlqLogMapper,
                                 RedissonClient redissonClient,
                                 ObjectMapper objectMapper) {
        this.dlqLogMapper = dlqLogMapper;
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询死信记录
     */
    public PageResult<DlqLogDto> listDlqLogs(String supplierCode, Integer dlqStatus, int page, int size) {
        LambdaQueryWrapper<NotificationDlqLogEntity> wrapper = new LambdaQueryWrapper<>();

        if (supplierCode != null && !supplierCode.isBlank()) {
            wrapper.eq(NotificationDlqLogEntity::getSupplierCode, supplierCode);
        }
        if (dlqStatus != null) {
            wrapper.eq(NotificationDlqLogEntity::getDlqStatus, dlqStatus);
        }
        wrapper.orderByDesc(NotificationDlqLogEntity::getCreateTime);

        IPage<NotificationDlqLogEntity> pageResult = dlqLogMapper.selectPage(
                new Page<>(page, size), wrapper);

        List<DlqLogDto> dtoList = pageResult.getRecords().stream()
                .map(this::toDto)
                .toList();

        return new PageResult<>(dtoList, pageResult.getTotal(), page, size);
    }

    /**
     * 单条死信重试
     * <p>
     * 1. 将 Redis 状态重置为 PROCESSING
     * 2. 重新压入 Redisson 队列
     * 3. 更新 updated_by
     */
    public void retryDlqRecord(Long id, String operator) {
        NotificationDlqLogEntity entity = dlqLogMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("死信记录不存在: id=" + id);
        }
        if (entity.getDlqStatus() != 0) {
            throw new IllegalStateException("仅待处理状态(0)的死信可重试，当前状态: " + entity.getDlqStatus());
        }

        doRetry(entity, operator);
    }

    /**
     * 批量重试
     */
    @SuppressWarnings("unchecked")
    public BatchResult batchRetryDlqRecords(List<Long> ids, String supplierCode, String operator) {
        List<NotificationDlqLogEntity> records;

        if (ids != null && !ids.isEmpty()) {
            records = dlqLogMapper.selectBatchIds(ids);
        } else if (supplierCode != null && !supplierCode.isBlank()) {
            LambdaQueryWrapper<NotificationDlqLogEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NotificationDlqLogEntity::getSupplierCode, supplierCode)
                   .eq(NotificationDlqLogEntity::getDlqStatus, 0);
            records = dlqLogMapper.selectList(wrapper);
        } else {
            throw new IllegalArgumentException("ids 或 supplierCode 至少提供一个");
        }

        int successCount = 0;
        int failureCount = 0;

        for (NotificationDlqLogEntity entity : records) {
            if (entity.getDlqStatus() != 0) {
                failureCount++;
                continue;
            }
            try {
                doRetry(entity, operator);
                successCount++;
            } catch (Exception e) {
                log.warn("批量重试失败: id={}, error={}", entity.getId(), e.getMessage());
                failureCount++;
            }
        }

        return new BatchResult(successCount, failureCount);
    }

    /**
     * 标记忽略
     */
    public void ignoreDlqRecord(Long id, String operator) {
        NotificationDlqLogEntity entity = dlqLogMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("死信记录不存在: id=" + id);
        }

        entity.setDlqStatus(2); // 已主动忽略
        entity.setUpdatedBy(operator);
        dlqLogMapper.updateById(entity);

        log.info("死信标记忽略: id={}, operator={}", id, operator);
    }

    /**
     * 执行重试：重置 Redis 状态 + 重新入队 + 更新 DB
     */
    @SuppressWarnings("unchecked")
    private void doRetry(NotificationDlqLogEntity entity, String operator) {
        String bizSign = entity.getBizSign();
        String supplierCode = entity.getSupplierCode();

        // 1. 重置 Redis 状态为 PROCESSING
        String statusKey = STATUS_PREFIX + bizSign;
        RBucket<String> bucket = redissonClient.getBucket(statusKey);
        bucket.set(IngestionService.STATUS_PROCESSING, Duration.ofHours(STATUS_TTL_HOURS));

        // 2. 重新压入 Redisson 队列
        try {
            String queueName = QUEUE_PREFIX + supplierCode;
            RQueue<String> queue = redissonClient.getQueue(queueName);

            // 从 unified_context 重建入队消息
            Map<String, Object> contextMap = objectMapper.readValue(entity.getUnifiedContext(), Map.class);
            contextMap.put("retryFromDlq", true);
            contextMap.put("dlqRecordId", entity.getId());
            String message = objectMapper.writeValueAsString(contextMap);
            queue.add(message);
        } catch (Exception e) {
            // 入队失败，回滚 Redis 状态（保留 TTL）
            bucket.set(IngestionService.STATUS_DEAD_LETTERED, Duration.ofHours(24));
            throw new RuntimeException("重新入队失败: " + e.getMessage(), e);
        }

        // 3. 更新 DB 状态
        entity.setDlqStatus(1); // 已人工重试成功
        entity.setUpdatedBy(operator);
        dlqLogMapper.updateById(entity);

        log.info("死信重试成功: id={}, bizSign={}, operator={}", entity.getId(), bizSign, operator);
    }

    /**
     * Entity 转 DTO
     */
    private DlqLogDto toDto(NotificationDlqLogEntity entity) {
        DlqLogDto dto = new DlqLogDto();
        dto.setId(entity.getId());
        dto.setBizSign(entity.getBizSign());
        dto.setTraceId(entity.getTraceId());
        dto.setSupplierCode(entity.getSupplierCode());
        dto.setErrorMsg(entity.getErrorMsg());
        dto.setRetryCount(entity.getRetryCount());
        dto.setDlqStatus(entity.getDlqStatus());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }
}
