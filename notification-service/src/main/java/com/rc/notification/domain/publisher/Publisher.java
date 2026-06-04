package com.rc.notification.domain.publisher;

import com.rc.notification.infrastructure.persistence.entity.PublisherEntity;

import java.time.LocalDateTime;

/**
 * 发布方领域模型
 * <p>
 * 纯 POJO，不依赖任何持久化框架注解
 */
public class Publisher {

    private Long id;
    private String publisherCode;
    private String publisherName;
    private String apiKey;
    private Integer status;
    private String contactInfo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从持久化实体转换为领域模型
     */
    public static Publisher fromEntity(PublisherEntity entity) {
        if (entity == null) {
            return null;
        }
        Publisher publisher = new Publisher();
        publisher.setId(entity.getId());
        publisher.setPublisherCode(entity.getPublisherCode());
        publisher.setPublisherName(entity.getPublisherName());
        publisher.setApiKey(entity.getApiKey());
        publisher.setStatus(entity.getStatus());
        publisher.setContactInfo(entity.getContactInfo());
        publisher.setCreateTime(entity.getCreateTime());
        publisher.setUpdateTime(entity.getUpdateTime());
        return publisher;
    }

    /**
     * 转换为持久化实体
     */
    public PublisherEntity toEntity() {
        PublisherEntity entity = new PublisherEntity();
        entity.setId(this.id);
        entity.setPublisherCode(this.publisherCode);
        entity.setPublisherName(this.publisherName);
        entity.setApiKey(this.apiKey);
        entity.setStatus(this.status);
        entity.setContactInfo(this.contactInfo);
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

    public String getPublisherCode() {
        return publisherCode;
    }

    public void setPublisherCode(String publisherCode) {
        this.publisherCode = publisherCode;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
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
