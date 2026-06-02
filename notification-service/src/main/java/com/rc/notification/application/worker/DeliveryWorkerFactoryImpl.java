package com.rc.notification.application.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.domain.config.SupplierConfigDomainService;
import com.rc.notification.infrastructure.http.FullStackHttpRequestBuilder;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * DeliveryWorker 工厂实现
 * <p>
 * 负责创建 DeliveryWorker 实例并注入所有依赖
 */
@Component
public class DeliveryWorkerFactoryImpl implements SupplierWorkerManager.DeliveryWorkerFactory {

    private final RedissonClient redissonClient;
    private final SupplierConfigDomainService configDomainService;
    private final FullStackHttpRequestBuilder requestBuilder;
    private final UnifiedInputContextBuilder contextBuilder;
    private final ObjectMapper objectMapper;

    /** 可选依赖：审计日志（T10 实现后注入） */
    private DeliveryWorker.AuditLogger auditLogger;

    /** 可选依赖：死信处理（T11 实现后注入） */
    private DeliveryWorker.DeadLetterHandler deadLetterHandler;

    public DeliveryWorkerFactoryImpl(RedissonClient redissonClient,
                                     SupplierConfigDomainService configDomainService,
                                     FullStackHttpRequestBuilder requestBuilder,
                                     UnifiedInputContextBuilder contextBuilder,
                                     ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.configDomainService = configDomainService;
        this.requestBuilder = requestBuilder;
        this.contextBuilder = contextBuilder;
        this.objectMapper = objectMapper;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setAuditLogger(DeliveryWorker.AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setDeadLetterHandler(DeliveryWorker.DeadLetterHandler deadLetterHandler) {
        this.deadLetterHandler = deadLetterHandler;
    }

    @Override
    public Runnable create(String supplierCode, String queueName, SupplierWorkerManager manager) {
        DeliveryWorker worker = new DeliveryWorker(
                supplierCode, queueName, manager,
                redissonClient, configDomainService,
                requestBuilder, contextBuilder, objectMapper);
        worker.setAuditLogger(auditLogger);
        worker.setDeadLetterHandler(deadLetterHandler);
        return worker;
    }
}
