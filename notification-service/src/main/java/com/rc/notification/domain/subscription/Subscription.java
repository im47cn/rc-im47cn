package com.rc.notification.domain.subscription;

import com.rc.notification.infrastructure.persistence.entity.SubscriptionEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订阅关系领域模型
 * <p>
 * 纯 POJO，不依赖任何持久化框架注解
 */
public class Subscription {

    private Long id;
    private String subscriberCode;
    private String eventTypeCode;
    private String status;
    private String managedBy;
    private String pathTemplate;
    private String queryTemplate;
    private String headerTemplate;
    private String bodyTemplate;
    private Integer connectTimeoutMs;
    private Integer readTimeoutMs;
    private Integer maxRetryCount;
    private Integer retryBackoffInitialMs;
    private BigDecimal retryBackoffMultiplier;
    private Integer retryBackoffMaxMs;
    private String successHttpCodes;
    private String successBodyPattern;
    private String successBodyMatchMode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从持久化实体转换为领域模型
     */
    public static Subscription fromEntity(SubscriptionEntity entity) {
        if (entity == null) {
            return null;
        }
        Subscription subscription = new Subscription();
        subscription.setId(entity.getId());
        subscription.setSubscriberCode(entity.getSubscriberCode());
        subscription.setEventTypeCode(entity.getEventTypeCode());
        subscription.setStatus(entity.getStatus());
        subscription.setManagedBy(entity.getManagedBy());
        subscription.setPathTemplate(entity.getPathTemplate());
        subscription.setQueryTemplate(entity.getQueryTemplate());
        subscription.setHeaderTemplate(entity.getHeaderTemplate());
        subscription.setBodyTemplate(entity.getBodyTemplate());
        subscription.setConnectTimeoutMs(entity.getConnectTimeoutMs());
        subscription.setReadTimeoutMs(entity.getReadTimeoutMs());
        subscription.setMaxRetryCount(entity.getMaxRetryCount());
        subscription.setRetryBackoffInitialMs(entity.getRetryBackoffInitialMs());
        subscription.setRetryBackoffMultiplier(entity.getRetryBackoffMultiplier());
        subscription.setRetryBackoffMaxMs(entity.getRetryBackoffMaxMs());
        subscription.setSuccessHttpCodes(entity.getSuccessHttpCodes());
        subscription.setSuccessBodyPattern(entity.getSuccessBodyPattern());
        subscription.setSuccessBodyMatchMode(entity.getSuccessBodyMatchMode());
        subscription.setCreateTime(entity.getCreateTime());
        subscription.setUpdateTime(entity.getUpdateTime());
        return subscription;
    }

    /**
     * 转换为持久化实体
     */
    public SubscriptionEntity toEntity() {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setId(this.id);
        entity.setSubscriberCode(this.subscriberCode);
        entity.setEventTypeCode(this.eventTypeCode);
        entity.setStatus(this.status);
        entity.setManagedBy(this.managedBy);
        entity.setPathTemplate(this.pathTemplate);
        entity.setQueryTemplate(this.queryTemplate);
        entity.setHeaderTemplate(this.headerTemplate);
        entity.setBodyTemplate(this.bodyTemplate);
        entity.setConnectTimeoutMs(this.connectTimeoutMs);
        entity.setReadTimeoutMs(this.readTimeoutMs);
        entity.setMaxRetryCount(this.maxRetryCount);
        entity.setRetryBackoffInitialMs(this.retryBackoffInitialMs);
        entity.setRetryBackoffMultiplier(this.retryBackoffMultiplier);
        entity.setRetryBackoffMaxMs(this.retryBackoffMaxMs);
        entity.setSuccessHttpCodes(this.successHttpCodes);
        entity.setSuccessBodyPattern(this.successBodyPattern);
        entity.setSuccessBodyMatchMode(this.successBodyMatchMode);
        entity.setCreateTime(this.createTime);
        entity.setUpdateTime(this.updateTime);
        return entity;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubscriberCode() {
        return subscriberCode;
    }

    public void setSubscriberCode(String subscriberCode) {
        this.subscriberCode = subscriberCode;
    }

    public String getEventTypeCode() {
        return eventTypeCode;
    }

    public void setEventTypeCode(String eventTypeCode) {
        this.eventTypeCode = eventTypeCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getManagedBy() {
        return managedBy;
    }

    public void setManagedBy(String managedBy) {
        this.managedBy = managedBy;
    }

    public String getPathTemplate() {
        return pathTemplate;
    }

    public void setPathTemplate(String pathTemplate) {
        this.pathTemplate = pathTemplate;
    }

    public String getQueryTemplate() {
        return queryTemplate;
    }

    public void setQueryTemplate(String queryTemplate) {
        this.queryTemplate = queryTemplate;
    }

    public String getHeaderTemplate() {
        return headerTemplate;
    }

    public void setHeaderTemplate(String headerTemplate) {
        this.headerTemplate = headerTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(Integer connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(Integer readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public Integer getRetryBackoffInitialMs() {
        return retryBackoffInitialMs;
    }

    public void setRetryBackoffInitialMs(Integer retryBackoffInitialMs) {
        this.retryBackoffInitialMs = retryBackoffInitialMs;
    }

    public BigDecimal getRetryBackoffMultiplier() {
        return retryBackoffMultiplier;
    }

    public void setRetryBackoffMultiplier(BigDecimal retryBackoffMultiplier) {
        this.retryBackoffMultiplier = retryBackoffMultiplier;
    }

    public Integer getRetryBackoffMaxMs() {
        return retryBackoffMaxMs;
    }

    public void setRetryBackoffMaxMs(Integer retryBackoffMaxMs) {
        this.retryBackoffMaxMs = retryBackoffMaxMs;
    }

    public String getSuccessHttpCodes() {
        return successHttpCodes;
    }

    public void setSuccessHttpCodes(String successHttpCodes) {
        this.successHttpCodes = successHttpCodes;
    }

    public String getSuccessBodyPattern() {
        return successBodyPattern;
    }

    public void setSuccessBodyPattern(String successBodyPattern) {
        this.successBodyPattern = successBodyPattern;
    }

    public String getSuccessBodyMatchMode() {
        return successBodyMatchMode;
    }

    public void setSuccessBodyMatchMode(String successBodyMatchMode) {
        this.successBodyMatchMode = successBodyMatchMode;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
