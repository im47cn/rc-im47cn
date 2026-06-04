# Event Registration & Subscription Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the notification platform from supplier-centric routing to a publish-subscribe model where business systems register event types and subscribers configure their own delivery mappings.

**Architecture:** Incremental evolution on existing DDD four-layer architecture. New entities (Publisher, EventType, Subscription) added alongside existing SupplierConfig (which becomes the subscriber's channel-level default). Ingest API v2 handles fan-out routing; v1 preserved via adapter. Worker config resolution gains a Subscription merge layer. Permission control is documented only, not implemented this phase.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus 3.5.5, Redisson, H2 (test), MySQL 8.0 (prod), Caffeine cache

---

## File Structure

### New Files

```
# Domain layer
src/main/java/com/rc/notification/domain/publisher/Publisher.java
src/main/java/com/rc/notification/domain/publisher/PublisherRepository.java
src/main/java/com/rc/notification/domain/event/EventType.java
src/main/java/com/rc/notification/domain/event/EventTypeRepository.java
src/main/java/com/rc/notification/domain/subscription/Subscription.java
src/main/java/com/rc/notification/domain/subscription/SubscriptionRepository.java
src/main/java/com/rc/notification/domain/subscription/EffectiveConfig.java
src/main/java/com/rc/notification/domain/subscription/EffectiveConfigResolver.java
src/main/java/com/rc/notification/domain/detection/FieldFingerprint.java
src/main/java/com/rc/notification/domain/detection/ChangeRecord.java
src/main/java/com/rc/notification/domain/detection/FieldFingerprintRepository.java
src/main/java/com/rc/notification/domain/detection/ChangeRecordRepository.java

# Infrastructure - persistence
src/main/java/com/rc/notification/infrastructure/persistence/entity/PublisherEntity.java
src/main/java/com/rc/notification/infrastructure/persistence/entity/EventTypeEntity.java
src/main/java/com/rc/notification/infrastructure/persistence/entity/SubscriptionEntity.java
src/main/java/com/rc/notification/infrastructure/persistence/entity/FieldFingerprintEntity.java
src/main/java/com/rc/notification/infrastructure/persistence/entity/ChangeRecordEntity.java
src/main/java/com/rc/notification/infrastructure/persistence/mapper/PublisherMapper.java
src/main/java/com/rc/notification/infrastructure/persistence/mapper/EventTypeMapper.java
src/main/java/com/rc/notification/infrastructure/persistence/mapper/SubscriptionMapper.java
src/main/java/com/rc/notification/infrastructure/persistence/mapper/FieldFingerprintMapper.java
src/main/java/com/rc/notification/infrastructure/persistence/mapper/ChangeRecordMapper.java
src/main/java/com/rc/notification/infrastructure/persistence/PublisherRepositoryImpl.java
src/main/java/com/rc/notification/infrastructure/persistence/EventTypeRepositoryImpl.java
src/main/java/com/rc/notification/infrastructure/persistence/SubscriptionRepositoryImpl.java
src/main/java/com/rc/notification/infrastructure/persistence/FieldFingerprintRepositoryImpl.java
src/main/java/com/rc/notification/infrastructure/persistence/ChangeRecordRepositoryImpl.java

# Application layer
src/main/java/com/rc/notification/application/admin/PublisherAdminService.java
src/main/java/com/rc/notification/application/admin/EventTypeAdminService.java
src/main/java/com/rc/notification/application/admin/SubscriptionAdminService.java
src/main/java/com/rc/notification/application/detection/FieldSamplingService.java
src/main/java/com/rc/notification/application/detection/ChangeDetectionService.java

# Interfaces - admin DTOs & controllers
src/main/java/com/rc/notification/interfaces/admin/dto/PublisherDto.java
src/main/java/com/rc/notification/interfaces/admin/dto/PublisherCreateRequest.java
src/main/java/com/rc/notification/interfaces/admin/dto/PublisherUpdateRequest.java
src/main/java/com/rc/notification/interfaces/admin/dto/EventTypeDto.java
src/main/java/com/rc/notification/interfaces/admin/dto/EventTypeCreateRequest.java
src/main/java/com/rc/notification/interfaces/admin/dto/EventTypeUpdateRequest.java
src/main/java/com/rc/notification/interfaces/admin/dto/SubscriptionDto.java
src/main/java/com/rc/notification/interfaces/admin/dto/SubscriptionCreateRequest.java
src/main/java/com/rc/notification/interfaces/admin/dto/SubscriptionUpdateRequest.java
src/main/java/com/rc/notification/interfaces/admin/PublisherController.java
src/main/java/com/rc/notification/interfaces/admin/EventTypeController.java
src/main/java/com/rc/notification/interfaces/admin/SubscriptionController.java

# Interfaces - v2 API
src/main/java/com/rc/notification/interfaces/api/dto/IngestV2Request.java
src/main/java/com/rc/notification/interfaces/api/dto/IngestV2Response.java
src/main/java/com/rc/notification/interfaces/api/dto/DispatchDetail.java
src/main/java/com/rc/notification/interfaces/api/EventIngestionV2Controller.java
src/main/java/com/rc/notification/interfaces/api/PublisherAuthFilter.java
src/main/java/com/rc/notification/interfaces/api/PublisherAuthFilterConfig.java

# Database migrations
src/main/resources/db/migration/V3__create_publisher.sql
src/main/resources/db/migration/V4__create_event_type.sql
src/main/resources/db/migration/V5__create_subscription.sql
src/main/resources/db/migration/V6__create_field_fingerprint_and_change_record.sql

# Tests
src/test/java/com/rc/notification/integration/PublisherCrudIntegrationTest.java
src/test/java/com/rc/notification/integration/EventTypeCrudIntegrationTest.java
src/test/java/com/rc/notification/integration/SubscriptionCrudIntegrationTest.java
src/test/java/com/rc/notification/integration/IngestV2IntegrationTest.java
src/test/java/com/rc/notification/integration/EffectiveConfigResolverTest.java
src/test/java/com/rc/notification/integration/FieldSamplingIntegrationTest.java
```

### Modified Files

```
# Schema & cleanup for tests
src/test/resources/schema.sql                    — add new tables
src/test/resources/cleanup.sql                   — add DELETE for new tables

# Worker config resolution
src/main/java/com/rc/notification/application/worker/DeliveryWorker.java
  — config loading uses EffectiveConfigResolver when eventTypeCode present

# Ingest service (fan-out)
src/main/java/com/rc/notification/application/service/IngestionService.java
  — extract single-target enqueue as reusable method for fan-out caller

# Queue message format
src/main/java/com/rc/notification/application/service/IngestionService.java:149-165
  — add eventTypeCode to serialized event map
```

---

## Phase 1: Foundation — New Entities & Persistence

### Task 1: Publisher Entity & Repository

**Files:**
- Create: `src/main/resources/db/migration/V3__create_publisher.sql`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/entity/PublisherEntity.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/mapper/PublisherMapper.java`
- Create: `src/main/java/com/rc/notification/domain/publisher/Publisher.java`
- Create: `src/main/java/com/rc/notification/domain/publisher/PublisherRepository.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/PublisherRepositoryImpl.java`
- Modify: `src/test/resources/schema.sql`
- Modify: `src/test/resources/cleanup.sql`

- [ ] **Step 1: Write MySQL migration**

```sql
-- V3__create_publisher.sql
CREATE TABLE `publisher` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `publisher_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '发布方唯一标识,如 order-service',
    `publisher_name` VARCHAR(128) NOT NULL COMMENT '发布方名称',
    `api_key` VARCHAR(256) NOT NULL UNIQUE COMMENT 'Ingest API 鉴权密钥',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `contact_info` VARCHAR(256) DEFAULT NULL COMMENT '联系方式',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件发布方注册表';
```

- [ ] **Step 2: Update H2 test schema**

Append to `src/test/resources/schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS publisher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_code VARCHAR(64) NOT NULL UNIQUE,
    publisher_name VARCHAR(128) NOT NULL,
    api_key VARCHAR(256) NOT NULL UNIQUE,
    status TINYINT NOT NULL DEFAULT 1,
    contact_info VARCHAR(256) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

Update `src/test/resources/cleanup.sql` — add at the top (before supplier_config deletion to avoid FK issues in future):

```sql
DELETE FROM publisher;
```

- [ ] **Step 3: Write PublisherEntity**

```java
package com.rc.notification.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 发布方实体，与 publisher 表一一映射
 */
@TableName("publisher")
public class PublisherEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("publisher_code")
    private String publisherCode;

    @TableField("publisher_name")
    private String publisherName;

    @TableField("api_key")
    private String apiKey;

    @TableField("status")
    private Integer status;

    @TableField("contact_info")
    private String contactInfo;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublisherCode() { return publisherCode; }
    public void setPublisherCode(String publisherCode) { this.publisherCode = publisherCode; }
    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 4: Write PublisherMapper**

```java
package com.rc.notification.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rc.notification.infrastructure.persistence.entity.PublisherEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PublisherMapper extends BaseMapper<PublisherEntity> {
}
```

- [ ] **Step 5: Write Publisher domain model**

```java
package com.rc.notification.domain.publisher;

import com.rc.notification.infrastructure.persistence.entity.PublisherEntity;

import java.time.LocalDateTime;

/**
 * 发布方领域模型
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

    public static Publisher fromEntity(PublisherEntity entity) {
        if (entity == null) return null;
        Publisher p = new Publisher();
        p.setId(entity.getId());
        p.setPublisherCode(entity.getPublisherCode());
        p.setPublisherName(entity.getPublisherName());
        p.setApiKey(entity.getApiKey());
        p.setStatus(entity.getStatus());
        p.setContactInfo(entity.getContactInfo());
        p.setCreateTime(entity.getCreateTime());
        p.setUpdateTime(entity.getUpdateTime());
        return p;
    }

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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublisherCode() { return publisherCode; }
    public void setPublisherCode(String publisherCode) { this.publisherCode = publisherCode; }
    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 6: Write PublisherRepository interface**

```java
package com.rc.notification.domain.publisher;

import java.util.List;

/**
 * 发布方 Repository 接口
 */
public interface PublisherRepository {
    Publisher findById(Long id);
    Publisher findByPublisherCode(String publisherCode);
    Publisher findByApiKey(String apiKey);
    List<Publisher> findByFilters(String keyword, Integer status, int page, int size);
    long countByFilters(String keyword, Integer status);
    Publisher save(Publisher publisher);
    Publisher update(Publisher publisher);
    boolean existsByPublisherCode(String publisherCode);
}
```

- [ ] **Step 7: Write PublisherRepositoryImpl**

```java
package com.rc.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rc.notification.domain.publisher.Publisher;
import com.rc.notification.domain.publisher.PublisherRepository;
import com.rc.notification.infrastructure.persistence.entity.PublisherEntity;
import com.rc.notification.infrastructure.persistence.mapper.PublisherMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PublisherRepositoryImpl implements PublisherRepository {

    private final PublisherMapper publisherMapper;

    public PublisherRepositoryImpl(PublisherMapper publisherMapper) {
        this.publisherMapper = publisherMapper;
    }

    @Override
    public Publisher findById(Long id) {
        return Publisher.fromEntity(publisherMapper.selectById(id));
    }

    @Override
    public Publisher findByPublisherCode(String publisherCode) {
        LambdaQueryWrapper<PublisherEntity> w = new LambdaQueryWrapper<>();
        w.eq(PublisherEntity::getPublisherCode, publisherCode);
        return Publisher.fromEntity(publisherMapper.selectOne(w));
    }

    @Override
    public Publisher findByApiKey(String apiKey) {
        LambdaQueryWrapper<PublisherEntity> w = new LambdaQueryWrapper<>();
        w.eq(PublisherEntity::getApiKey, apiKey);
        return Publisher.fromEntity(publisherMapper.selectOne(w));
    }

    @Override
    public List<Publisher> findByFilters(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<PublisherEntity> w = buildFilterWrapper(keyword, status);
        w.orderByDesc(PublisherEntity::getUpdateTime);
        return publisherMapper.selectPage(new Page<>(page, size), w)
                .getRecords().stream().map(Publisher::fromEntity).toList();
    }

    @Override
    public long countByFilters(String keyword, Integer status) {
        return publisherMapper.selectCount(buildFilterWrapper(keyword, status));
    }

    @Override
    public Publisher save(Publisher publisher) {
        PublisherEntity entity = publisher.toEntity();
        publisherMapper.insert(entity);
        publisher.setId(entity.getId());
        return publisher;
    }

    @Override
    public Publisher update(Publisher publisher) {
        publisherMapper.updateById(publisher.toEntity());
        return publisher;
    }

    @Override
    public boolean existsByPublisherCode(String publisherCode) {
        LambdaQueryWrapper<PublisherEntity> w = new LambdaQueryWrapper<>();
        w.eq(PublisherEntity::getPublisherCode, publisherCode);
        return publisherMapper.selectCount(w) > 0;
    }

    private LambdaQueryWrapper<PublisherEntity> buildFilterWrapper(String keyword, Integer status) {
        LambdaQueryWrapper<PublisherEntity> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(PublisherEntity::getPublisherCode, keyword)
                    .or().like(PublisherEntity::getPublisherName, keyword));
        }
        if (status != null) w.eq(PublisherEntity::getStatus, status);
        return w;
    }
}
```

- [ ] **Step 8: Verify compilation**

Run: `cd notification-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: Verify existing tests still pass**

Run: `cd notification-service && mvn test -q`
Expected: BUILD SUCCESS, all 28 tests pass

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: add Publisher entity, repository and database migration"
```

---

### Task 2: EventType Entity & Repository

**Files:**
- Create: `src/main/resources/db/migration/V4__create_event_type.sql`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/entity/EventTypeEntity.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/mapper/EventTypeMapper.java`
- Create: `src/main/java/com/rc/notification/domain/event/EventType.java`
- Create: `src/main/java/com/rc/notification/domain/event/EventTypeRepository.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/EventTypeRepositoryImpl.java`
- Modify: `src/test/resources/schema.sql`
- Modify: `src/test/resources/cleanup.sql`

- [ ] **Step 1: Write MySQL migration**

```sql
-- V4__create_event_type.sql
CREATE TABLE `event_type` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `event_type_code` VARCHAR(128) NOT NULL UNIQUE COMMENT '事件类型唯一标识,如 ORDER_CREATED',
    `publisher_code` VARCHAR(64) NOT NULL COMMENT '归属发布方编码',
    `display_name` VARCHAR(128) NOT NULL COMMENT '事件显示名称',
    `description` TEXT DEFAULT NULL COMMENT '事件描述',
    `payload_schema` TEXT DEFAULT NULL COMMENT '可选 JSON Schema,用于入口校验',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/ACTIVE/DEPRECATED',
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号,Schema变更时递增',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    INDEX `idx_publisher_code` (`publisher_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件类型注册表';
```

- [ ] **Step 2: Update H2 test schema**

Append to `src/test/resources/schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS event_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type_code VARCHAR(128) NOT NULL UNIQUE,
    publisher_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    description TEXT DEFAULT NULL,
    payload_schema TEXT DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    version INT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

Update `src/test/resources/cleanup.sql` — add before publisher deletion:

```sql
DELETE FROM event_type;
```

- [ ] **Step 3: Write EventTypeEntity**

```java
package com.rc.notification.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 事件类型实体，与 event_type 表一一映射
 */
@TableName("event_type")
public class EventTypeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_type_code")
    private String eventTypeCode;

    @TableField("publisher_code")
    private String publisherCode;

    @TableField("display_name")
    private String displayName;

    @TableField("description")
    private String description;

    @TableField("payload_schema")
    private String payloadSchema;

    @TableField("status")
    private String status;

    @TableField("version")
    private Integer version;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventTypeCode() { return eventTypeCode; }
    public void setEventTypeCode(String eventTypeCode) { this.eventTypeCode = eventTypeCode; }
    public String getPublisherCode() { return publisherCode; }
    public void setPublisherCode(String publisherCode) { this.publisherCode = publisherCode; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPayloadSchema() { return payloadSchema; }
    public void setPayloadSchema(String payloadSchema) { this.payloadSchema = payloadSchema; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 4: Write EventTypeMapper**

```java
package com.rc.notification.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rc.notification.infrastructure.persistence.entity.EventTypeEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventTypeMapper extends BaseMapper<EventTypeEntity> {
}
```

- [ ] **Step 5: Write EventType domain model**

```java
package com.rc.notification.domain.event;

import com.rc.notification.infrastructure.persistence.entity.EventTypeEntity;

import java.time.LocalDateTime;

/**
 * 事件类型领域模型
 */
public class EventType {

    private Long id;
    private String eventTypeCode;
    private String publisherCode;
    private String displayName;
    private String description;
    private String payloadSchema;
    private String status;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static EventType fromEntity(EventTypeEntity entity) {
        if (entity == null) return null;
        EventType e = new EventType();
        e.setId(entity.getId());
        e.setEventTypeCode(entity.getEventTypeCode());
        e.setPublisherCode(entity.getPublisherCode());
        e.setDisplayName(entity.getDisplayName());
        e.setDescription(entity.getDescription());
        e.setPayloadSchema(entity.getPayloadSchema());
        e.setStatus(entity.getStatus());
        e.setVersion(entity.getVersion());
        e.setCreateTime(entity.getCreateTime());
        e.setUpdateTime(entity.getUpdateTime());
        return e;
    }

    public EventTypeEntity toEntity() {
        EventTypeEntity entity = new EventTypeEntity();
        entity.setId(this.id);
        entity.setEventTypeCode(this.eventTypeCode);
        entity.setPublisherCode(this.publisherCode);
        entity.setDisplayName(this.displayName);
        entity.setDescription(this.description);
        entity.setPayloadSchema(this.payloadSchema);
        entity.setStatus(this.status);
        entity.setVersion(this.version);
        entity.setCreateTime(this.createTime);
        entity.setUpdateTime(this.updateTime);
        return entity;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventTypeCode() { return eventTypeCode; }
    public void setEventTypeCode(String eventTypeCode) { this.eventTypeCode = eventTypeCode; }
    public String getPublisherCode() { return publisherCode; }
    public void setPublisherCode(String publisherCode) { this.publisherCode = publisherCode; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPayloadSchema() { return payloadSchema; }
    public void setPayloadSchema(String payloadSchema) { this.payloadSchema = payloadSchema; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 6: Write EventTypeRepository interface**

```java
package com.rc.notification.domain.event;

import java.util.List;

/**
 * 事件类型 Repository 接口
 */
public interface EventTypeRepository {
    EventType findById(Long id);
    EventType findByEventTypeCode(String eventTypeCode);
    List<EventType> findByPublisherCode(String publisherCode);
    List<EventType> findByFilters(String keyword, String publisherCode, String status, int page, int size);
    long countByFilters(String keyword, String publisherCode, String status);
    EventType save(EventType eventType);
    EventType update(EventType eventType);
    boolean existsByEventTypeCode(String eventTypeCode);
}
```

- [ ] **Step 7: Write EventTypeRepositoryImpl**

```java
package com.rc.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rc.notification.domain.event.EventType;
import com.rc.notification.domain.event.EventTypeRepository;
import com.rc.notification.infrastructure.persistence.entity.EventTypeEntity;
import com.rc.notification.infrastructure.persistence.mapper.EventTypeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EventTypeRepositoryImpl implements EventTypeRepository {

    private final EventTypeMapper eventTypeMapper;

    public EventTypeRepositoryImpl(EventTypeMapper eventTypeMapper) {
        this.eventTypeMapper = eventTypeMapper;
    }

    @Override
    public EventType findById(Long id) {
        return EventType.fromEntity(eventTypeMapper.selectById(id));
    }

    @Override
    public EventType findByEventTypeCode(String eventTypeCode) {
        LambdaQueryWrapper<EventTypeEntity> w = new LambdaQueryWrapper<>();
        w.eq(EventTypeEntity::getEventTypeCode, eventTypeCode);
        return EventType.fromEntity(eventTypeMapper.selectOne(w));
    }

    @Override
    public List<EventType> findByPublisherCode(String publisherCode) {
        LambdaQueryWrapper<EventTypeEntity> w = new LambdaQueryWrapper<>();
        w.eq(EventTypeEntity::getPublisherCode, publisherCode);
        w.orderByDesc(EventTypeEntity::getUpdateTime);
        return eventTypeMapper.selectList(w).stream().map(EventType::fromEntity).toList();
    }

    @Override
    public List<EventType> findByFilters(String keyword, String publisherCode, String status, int page, int size) {
        LambdaQueryWrapper<EventTypeEntity> w = buildFilterWrapper(keyword, publisherCode, status);
        w.orderByDesc(EventTypeEntity::getUpdateTime);
        return eventTypeMapper.selectPage(new Page<>(page, size), w)
                .getRecords().stream().map(EventType::fromEntity).toList();
    }

    @Override
    public long countByFilters(String keyword, String publisherCode, String status) {
        return eventTypeMapper.selectCount(buildFilterWrapper(keyword, publisherCode, status));
    }

    @Override
    public EventType save(EventType eventType) {
        EventTypeEntity entity = eventType.toEntity();
        eventTypeMapper.insert(entity);
        eventType.setId(entity.getId());
        return eventType;
    }

    @Override
    public EventType update(EventType eventType) {
        eventTypeMapper.updateById(eventType.toEntity());
        return eventType;
    }

    @Override
    public boolean existsByEventTypeCode(String eventTypeCode) {
        LambdaQueryWrapper<EventTypeEntity> w = new LambdaQueryWrapper<>();
        w.eq(EventTypeEntity::getEventTypeCode, eventTypeCode);
        return eventTypeMapper.selectCount(w) > 0;
    }

    private LambdaQueryWrapper<EventTypeEntity> buildFilterWrapper(String keyword, String publisherCode, String status) {
        LambdaQueryWrapper<EventTypeEntity> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(EventTypeEntity::getEventTypeCode, keyword)
                    .or().like(EventTypeEntity::getDisplayName, keyword));
        }
        if (publisherCode != null && !publisherCode.isBlank()) {
            w.eq(EventTypeEntity::getPublisherCode, publisherCode);
        }
        if (status != null && !status.isBlank()) {
            w.eq(EventTypeEntity::getStatus, status);
        }
        return w;
    }
}
```

- [ ] **Step 8: Verify compilation and tests**

Run: `cd notification-service && mvn test -q`
Expected: BUILD SUCCESS, all 28 tests pass

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: add EventType entity, repository and database migration"
```

---

### Task 3: Subscription Entity & Repository

**Files:**
- Create: `src/main/resources/db/migration/V5__create_subscription.sql`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/entity/SubscriptionEntity.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/mapper/SubscriptionMapper.java`
- Create: `src/main/java/com/rc/notification/domain/subscription/Subscription.java`
- Create: `src/main/java/com/rc/notification/domain/subscription/SubscriptionRepository.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/SubscriptionRepositoryImpl.java`
- Modify: `src/test/resources/schema.sql`
- Modify: `src/test/resources/cleanup.sql`

- [ ] **Step 1: Write MySQL migration**

```sql
-- V5__create_subscription.sql
CREATE TABLE `subscription` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `subscriber_code` VARCHAR(64) NOT NULL COMMENT '订阅方编码(对应 supplier_config.supplier_code)',
    `event_type_code` VARCHAR(128) NOT NULL COMMENT '事件类型编码',
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED',
    `managed_by` VARCHAR(16) NOT NULL DEFAULT 'SUBSCRIBER' COMMENT '管理方: SUBSCRIBER/PUBLISHER/PLATFORM',
    `path_template` VARCHAR(255) DEFAULT NULL COMMENT '覆盖:JSONata Path模板',
    `query_template` TEXT DEFAULT NULL COMMENT '覆盖:JSONata Query模板',
    `header_template` TEXT DEFAULT NULL COMMENT '覆盖:JSONata Header模板',
    `body_template` TEXT DEFAULT NULL COMMENT '覆盖:JSONata Body模板',
    `connect_timeout_ms` INT DEFAULT NULL COMMENT '覆盖:连接超时(ms)',
    `read_timeout_ms` INT DEFAULT NULL COMMENT '覆盖:读取超时(ms)',
    `max_retry_count` INT DEFAULT NULL COMMENT '覆盖:最大重试次数',
    `retry_backoff_initial_ms` INT DEFAULT NULL COMMENT '覆盖:退避初始延迟(ms)',
    `retry_backoff_multiplier` DECIMAL(5,2) DEFAULT NULL COMMENT '覆盖:退避指数乘数',
    `retry_backoff_max_ms` INT DEFAULT NULL COMMENT '覆盖:退避最大延迟(ms)',
    `success_http_codes` VARCHAR(64) DEFAULT NULL COMMENT '覆盖:成功HTTP状态码',
    `success_body_pattern` VARCHAR(512) DEFAULT NULL COMMENT '覆盖:响应体匹配表达式',
    `success_body_match_mode` VARCHAR(16) DEFAULT NULL COMMENT '覆盖:匹配模式',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE KEY `uk_subscriber_event` (`subscriber_code`, `event_type_code`),
    INDEX `idx_event_type_code` (`event_type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅关系表';
```

- [ ] **Step 2: Update H2 test schema**

Append to `src/test/resources/schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscriber_code VARCHAR(64) NOT NULL,
    event_type_code VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    managed_by VARCHAR(16) NOT NULL DEFAULT 'SUBSCRIBER',
    path_template VARCHAR(255) DEFAULT NULL,
    query_template TEXT DEFAULT NULL,
    header_template TEXT DEFAULT NULL,
    body_template TEXT DEFAULT NULL,
    connect_timeout_ms INT DEFAULT NULL,
    read_timeout_ms INT DEFAULT NULL,
    max_retry_count INT DEFAULT NULL,
    retry_backoff_initial_ms INT DEFAULT NULL,
    retry_backoff_multiplier DECIMAL(5,2) DEFAULT NULL,
    retry_backoff_max_ms INT DEFAULT NULL,
    success_http_codes VARCHAR(64) DEFAULT NULL,
    success_body_pattern VARCHAR(512) DEFAULT NULL,
    success_body_match_mode VARCHAR(16) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_subscriber_event UNIQUE (subscriber_code, event_type_code)
);
```

Update `src/test/resources/cleanup.sql` — add before event_type deletion:

```sql
DELETE FROM subscription;
```

- [ ] **Step 3: Write SubscriptionEntity**

```java
package com.rc.notification.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订阅关系实体，与 subscription 表一一映射
 */
@TableName("subscription")
public class SubscriptionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("subscriber_code")
    private String subscriberCode;

    @TableField("event_type_code")
    private String eventTypeCode;

    @TableField("status")
    private String status;

    @TableField("managed_by")
    private String managedBy;

    @TableField("path_template")
    private String pathTemplate;

    @TableField("query_template")
    private String queryTemplate;

    @TableField("header_template")
    private String headerTemplate;

    @TableField("body_template")
    private String bodyTemplate;

    @TableField("connect_timeout_ms")
    private Integer connectTimeoutMs;

    @TableField("read_timeout_ms")
    private Integer readTimeoutMs;

    @TableField("max_retry_count")
    private Integer maxRetryCount;

    @TableField("retry_backoff_initial_ms")
    private Integer retryBackoffInitialMs;

    @TableField("retry_backoff_multiplier")
    private BigDecimal retryBackoffMultiplier;

    @TableField("retry_backoff_max_ms")
    private Integer retryBackoffMaxMs;

    @TableField("success_http_codes")
    private String successHttpCodes;

    @TableField("success_body_pattern")
    private String successBodyPattern;

    @TableField("success_body_match_mode")
    private String successBodyMatchMode;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubscriberCode() { return subscriberCode; }
    public void setSubscriberCode(String subscriberCode) { this.subscriberCode = subscriberCode; }
    public String getEventTypeCode() { return eventTypeCode; }
    public void setEventTypeCode(String eventTypeCode) { this.eventTypeCode = eventTypeCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getManagedBy() { return managedBy; }
    public void setManagedBy(String managedBy) { this.managedBy = managedBy; }
    public String getPathTemplate() { return pathTemplate; }
    public void setPathTemplate(String pathTemplate) { this.pathTemplate = pathTemplate; }
    public String getQueryTemplate() { return queryTemplate; }
    public void setQueryTemplate(String queryTemplate) { this.queryTemplate = queryTemplate; }
    public String getHeaderTemplate() { return headerTemplate; }
    public void setHeaderTemplate(String headerTemplate) { this.headerTemplate = headerTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(Integer connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public Integer getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(Integer readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(Integer maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    public Integer getRetryBackoffInitialMs() { return retryBackoffInitialMs; }
    public void setRetryBackoffInitialMs(Integer retryBackoffInitialMs) { this.retryBackoffInitialMs = retryBackoffInitialMs; }
    public BigDecimal getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public void setRetryBackoffMultiplier(BigDecimal retryBackoffMultiplier) { this.retryBackoffMultiplier = retryBackoffMultiplier; }
    public Integer getRetryBackoffMaxMs() { return retryBackoffMaxMs; }
    public void setRetryBackoffMaxMs(Integer retryBackoffMaxMs) { this.retryBackoffMaxMs = retryBackoffMaxMs; }
    public String getSuccessHttpCodes() { return successHttpCodes; }
    public void setSuccessHttpCodes(String successHttpCodes) { this.successHttpCodes = successHttpCodes; }
    public String getSuccessBodyPattern() { return successBodyPattern; }
    public void setSuccessBodyPattern(String successBodyPattern) { this.successBodyPattern = successBodyPattern; }
    public String getSuccessBodyMatchMode() { return successBodyMatchMode; }
    public void setSuccessBodyMatchMode(String successBodyMatchMode) { this.successBodyMatchMode = successBodyMatchMode; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 4: Write SubscriptionMapper**

```java
package com.rc.notification.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rc.notification.infrastructure.persistence.entity.SubscriptionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubscriptionMapper extends BaseMapper<SubscriptionEntity> {
}
```

- [ ] **Step 5: Write Subscription domain model**

```java
package com.rc.notification.domain.subscription;

import com.rc.notification.infrastructure.persistence.entity.SubscriptionEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订阅关系领域模型
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

    public static Subscription fromEntity(SubscriptionEntity entity) {
        if (entity == null) return null;
        Subscription s = new Subscription();
        s.setId(entity.getId());
        s.setSubscriberCode(entity.getSubscriberCode());
        s.setEventTypeCode(entity.getEventTypeCode());
        s.setStatus(entity.getStatus());
        s.setManagedBy(entity.getManagedBy());
        s.setPathTemplate(entity.getPathTemplate());
        s.setQueryTemplate(entity.getQueryTemplate());
        s.setHeaderTemplate(entity.getHeaderTemplate());
        s.setBodyTemplate(entity.getBodyTemplate());
        s.setConnectTimeoutMs(entity.getConnectTimeoutMs());
        s.setReadTimeoutMs(entity.getReadTimeoutMs());
        s.setMaxRetryCount(entity.getMaxRetryCount());
        s.setRetryBackoffInitialMs(entity.getRetryBackoffInitialMs());
        s.setRetryBackoffMultiplier(entity.getRetryBackoffMultiplier());
        s.setRetryBackoffMaxMs(entity.getRetryBackoffMaxMs());
        s.setSuccessHttpCodes(entity.getSuccessHttpCodes());
        s.setSuccessBodyPattern(entity.getSuccessBodyPattern());
        s.setSuccessBodyMatchMode(entity.getSuccessBodyMatchMode());
        s.setCreateTime(entity.getCreateTime());
        s.setUpdateTime(entity.getUpdateTime());
        return s;
    }

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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubscriberCode() { return subscriberCode; }
    public void setSubscriberCode(String subscriberCode) { this.subscriberCode = subscriberCode; }
    public String getEventTypeCode() { return eventTypeCode; }
    public void setEventTypeCode(String eventTypeCode) { this.eventTypeCode = eventTypeCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getManagedBy() { return managedBy; }
    public void setManagedBy(String managedBy) { this.managedBy = managedBy; }
    public String getPathTemplate() { return pathTemplate; }
    public void setPathTemplate(String pathTemplate) { this.pathTemplate = pathTemplate; }
    public String getQueryTemplate() { return queryTemplate; }
    public void setQueryTemplate(String queryTemplate) { this.queryTemplate = queryTemplate; }
    public String getHeaderTemplate() { return headerTemplate; }
    public void setHeaderTemplate(String headerTemplate) { this.headerTemplate = headerTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(Integer connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public Integer getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(Integer readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(Integer maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    public Integer getRetryBackoffInitialMs() { return retryBackoffInitialMs; }
    public void setRetryBackoffInitialMs(Integer retryBackoffInitialMs) { this.retryBackoffInitialMs = retryBackoffInitialMs; }
    public BigDecimal getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public void setRetryBackoffMultiplier(BigDecimal retryBackoffMultiplier) { this.retryBackoffMultiplier = retryBackoffMultiplier; }
    public Integer getRetryBackoffMaxMs() { return retryBackoffMaxMs; }
    public void setRetryBackoffMaxMs(Integer retryBackoffMaxMs) { this.retryBackoffMaxMs = retryBackoffMaxMs; }
    public String getSuccessHttpCodes() { return successHttpCodes; }
    public void setSuccessHttpCodes(String successHttpCodes) { this.successHttpCodes = successHttpCodes; }
    public String getSuccessBodyPattern() { return successBodyPattern; }
    public void setSuccessBodyPattern(String successBodyPattern) { this.successBodyPattern = successBodyPattern; }
    public String getSuccessBodyMatchMode() { return successBodyMatchMode; }
    public void setSuccessBodyMatchMode(String successBodyMatchMode) { this.successBodyMatchMode = successBodyMatchMode; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 6: Write SubscriptionRepository interface**

```java
package com.rc.notification.domain.subscription;

import java.util.List;

/**
 * 订阅关系 Repository 接口
 */
public interface SubscriptionRepository {
    Subscription findById(Long id);
    Subscription findBySubscriberAndEventType(String subscriberCode, String eventTypeCode);
    List<Subscription> findActiveByEventTypeCode(String eventTypeCode);
    List<Subscription> findBySubscriberCode(String subscriberCode);
    List<Subscription> findByFilters(String subscriberCode, String eventTypeCode, String status, int page, int size);
    long countByFilters(String subscriberCode, String eventTypeCode, String status);
    Subscription save(Subscription subscription);
    Subscription update(Subscription subscription);
    boolean existsBySubscriberAndEventType(String subscriberCode, String eventTypeCode);
}
```

- [ ] **Step 7: Write SubscriptionRepositoryImpl**

```java
package com.rc.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rc.notification.domain.subscription.Subscription;
import com.rc.notification.domain.subscription.SubscriptionRepository;
import com.rc.notification.infrastructure.persistence.entity.SubscriptionEntity;
import com.rc.notification.infrastructure.persistence.mapper.SubscriptionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final SubscriptionMapper subscriptionMapper;

    public SubscriptionRepositoryImpl(SubscriptionMapper subscriptionMapper) {
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    public Subscription findById(Long id) {
        return Subscription.fromEntity(subscriptionMapper.selectById(id));
    }

    @Override
    public Subscription findBySubscriberAndEventType(String subscriberCode, String eventTypeCode) {
        LambdaQueryWrapper<SubscriptionEntity> w = new LambdaQueryWrapper<>();
        w.eq(SubscriptionEntity::getSubscriberCode, subscriberCode);
        w.eq(SubscriptionEntity::getEventTypeCode, eventTypeCode);
        return Subscription.fromEntity(subscriptionMapper.selectOne(w));
    }

    @Override
    public List<Subscription> findActiveByEventTypeCode(String eventTypeCode) {
        LambdaQueryWrapper<SubscriptionEntity> w = new LambdaQueryWrapper<>();
        w.eq(SubscriptionEntity::getEventTypeCode, eventTypeCode);
        w.eq(SubscriptionEntity::getStatus, "ACTIVE");
        return subscriptionMapper.selectList(w).stream().map(Subscription::fromEntity).toList();
    }

    @Override
    public List<Subscription> findBySubscriberCode(String subscriberCode) {
        LambdaQueryWrapper<SubscriptionEntity> w = new LambdaQueryWrapper<>();
        w.eq(SubscriptionEntity::getSubscriberCode, subscriberCode);
        return subscriptionMapper.selectList(w).stream().map(Subscription::fromEntity).toList();
    }

    @Override
    public List<Subscription> findByFilters(String subscriberCode, String eventTypeCode, String status, int page, int size) {
        LambdaQueryWrapper<SubscriptionEntity> w = buildFilterWrapper(subscriberCode, eventTypeCode, status);
        w.orderByDesc(SubscriptionEntity::getUpdateTime);
        return subscriptionMapper.selectPage(new Page<>(page, size), w)
                .getRecords().stream().map(Subscription::fromEntity).toList();
    }

    @Override
    public long countByFilters(String subscriberCode, String eventTypeCode, String status) {
        return subscriptionMapper.selectCount(buildFilterWrapper(subscriberCode, eventTypeCode, status));
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionEntity entity = subscription.toEntity();
        subscriptionMapper.insert(entity);
        subscription.setId(entity.getId());
        return subscription;
    }

    @Override
    public Subscription update(Subscription subscription) {
        subscriptionMapper.updateById(subscription.toEntity());
        return subscription;
    }

    @Override
    public boolean existsBySubscriberAndEventType(String subscriberCode, String eventTypeCode) {
        LambdaQueryWrapper<SubscriptionEntity> w = new LambdaQueryWrapper<>();
        w.eq(SubscriptionEntity::getSubscriberCode, subscriberCode);
        w.eq(SubscriptionEntity::getEventTypeCode, eventTypeCode);
        return subscriptionMapper.selectCount(w) > 0;
    }

    private LambdaQueryWrapper<SubscriptionEntity> buildFilterWrapper(String subscriberCode, String eventTypeCode, String status) {
        LambdaQueryWrapper<SubscriptionEntity> w = new LambdaQueryWrapper<>();
        if (subscriberCode != null && !subscriberCode.isBlank()) {
            w.eq(SubscriptionEntity::getSubscriberCode, subscriberCode);
        }
        if (eventTypeCode != null && !eventTypeCode.isBlank()) {
            w.eq(SubscriptionEntity::getEventTypeCode, eventTypeCode);
        }
        if (status != null && !status.isBlank()) {
            w.eq(SubscriptionEntity::getStatus, status);
        }
        return w;
    }
}
```

- [ ] **Step 8: Verify compilation and tests**

Run: `cd notification-service && mvn test -q`
Expected: BUILD SUCCESS, all 28 tests pass

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: add Subscription entity, repository and database migration"
```

---

### Task 4: EffectiveConfig & Config Merge Logic

**Files:**
- Create: `src/main/java/com/rc/notification/domain/subscription/EffectiveConfig.java`
- Create: `src/main/java/com/rc/notification/domain/subscription/EffectiveConfigResolver.java`
- Create: `src/test/java/com/rc/notification/integration/EffectiveConfigResolverTest.java`

- [ ] **Step 1: Write failing test for config merge**

```java
package com.rc.notification.integration;

import com.rc.notification.domain.config.SupplierConfig;
import com.rc.notification.domain.subscription.EffectiveConfig;
import com.rc.notification.domain.subscription.EffectiveConfigResolver;
import com.rc.notification.domain.subscription.Subscription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EffectiveConfigResolver unit tests - no Spring context needed
 */
class EffectiveConfigResolverTest {

    @Test
    @DisplayName("No subscription: all fields from SubscriberConfig")
    void noSubscription_usesBaseConfig() {
        SupplierConfig base = buildBase();
        EffectiveConfig result = EffectiveConfigResolver.resolve(base, null);

        assertEquals("https://api.test.com", result.getBaseUrl());
        assertEquals("encrypted-cred", result.getCredentialsEncrypted());
        assertEquals("/default/path", result.getPathTemplate());
        assertEquals(3000, result.getConnectTimeoutMs());
        assertEquals(3, result.getMaxRetryCount());
    }

    @Test
    @DisplayName("Subscription overrides specific fields, inherits rest")
    void subscriptionOverrides_mergedCorrectly() {
        SupplierConfig base = buildBase();
        Subscription sub = new Subscription();
        sub.setPathTemplate("/override/path");
        sub.setReadTimeoutMs(10000);
        // leave other fields null -> inherit from base

        EffectiveConfig result = EffectiveConfigResolver.resolve(base, sub);

        // Overridden
        assertEquals("/override/path", result.getPathTemplate());
        assertEquals(10000, result.getReadTimeoutMs());
        // Inherited from base
        assertEquals("https://api.test.com", result.getBaseUrl());
        assertEquals("encrypted-cred", result.getCredentialsEncrypted());
        assertEquals(3000, result.getConnectTimeoutMs());
        assertEquals(3, result.getMaxRetryCount());
        assertEquals("'{\"test\": true}'", result.getBodyTemplate());
    }

    @Test
    @DisplayName("Subscription overrides all overridable fields")
    void subscriptionOverridesAll() {
        SupplierConfig base = buildBase();
        Subscription sub = new Subscription();
        sub.setPathTemplate("/sub/path");
        sub.setQueryTemplate("sub-query");
        sub.setHeaderTemplate("sub-header");
        sub.setBodyTemplate("sub-body");
        sub.setConnectTimeoutMs(1000);
        sub.setReadTimeoutMs(2000);
        sub.setMaxRetryCount(10);
        sub.setRetryBackoffInitialMs(500);
        sub.setRetryBackoffMultiplier(new BigDecimal("3.00"));
        sub.setRetryBackoffMaxMs(60000);
        sub.setSuccessHttpCodes("200,201");
        sub.setSuccessBodyPattern("ok");
        sub.setSuccessBodyMatchMode("CONTAINS");

        EffectiveConfig result = EffectiveConfigResolver.resolve(base, sub);

        // Channel-level always from base
        assertEquals("https://api.test.com", result.getBaseUrl());
        assertEquals("POST", result.getHttpMethod());
        assertEquals("APPLICATION_JSON", result.getContentTypeBehavior());
        assertEquals("encrypted-cred", result.getCredentialsEncrypted());
        // All overridable fields from subscription
        assertEquals("/sub/path", result.getPathTemplate());
        assertEquals("sub-query", result.getQueryTemplate());
        assertEquals("sub-header", result.getHeaderTemplate());
        assertEquals("sub-body", result.getBodyTemplate());
        assertEquals(1000, result.getConnectTimeoutMs());
        assertEquals(2000, result.getReadTimeoutMs());
        assertEquals(10, result.getMaxRetryCount());
        assertEquals(500, result.getRetryBackoffInitialMs());
        assertEquals(new BigDecimal("3.00"), result.getRetryBackoffMultiplier());
        assertEquals(60000, result.getRetryBackoffMaxMs());
        assertEquals("200,201", result.getSuccessHttpCodes());
        assertEquals("ok", result.getSuccessBodyPattern());
        assertEquals("CONTAINS", result.getSuccessBodyMatchMode());
    }

    private SupplierConfig buildBase() {
        SupplierConfig c = new SupplierConfig();
        c.setSupplierCode("TEST_SUB");
        c.setBaseUrl("https://api.test.com");
        c.setHttpMethod("POST");
        c.setContentTypeBehavior("APPLICATION_JSON");
        c.setCredentialsEncrypted("encrypted-cred");
        c.setPathTemplate("/default/path");
        c.setQueryTemplate("default-query");
        c.setHeaderTemplate("default-header");
        c.setBodyTemplate("'{\"test\": true}'");
        c.setConnectTimeoutMs(3000);
        c.setReadTimeoutMs(5000);
        c.setSuccessHttpCodes("200");
        c.setSuccessBodyPattern(null);
        c.setSuccessBodyMatchMode("EQUALS");
        c.setSuccessCaseSensitive(1);
        c.setMaxRetryCount(3);
        c.setRetryBackoffInitialMs(1000);
        c.setRetryBackoffMultiplier(new BigDecimal("2.00"));
        c.setRetryBackoffMaxMs(30000);
        c.setWorkerConcurrency(2);
        c.setStatus(1);
        return c;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd notification-service && mvn test -pl . -Dtest=EffectiveConfigResolverTest -q`
Expected: FAIL — `EffectiveConfig` and `EffectiveConfigResolver` do not exist

- [ ] **Step 3: Write EffectiveConfig**

```java
package com.rc.notification.domain.subscription;

import java.math.BigDecimal;

/**
 * 合并后的生效配置
 * <p>
 * 由 SubscriberConfig (通道默认) + Subscription (事件级覆盖) 合并而来，
 * 供 DeliveryWorker 直接使用，不可变。
 */
public class EffectiveConfig {

    // Channel-level (always from SubscriberConfig)
    private final String subscriberCode;
    private final String baseUrl;
    private final String httpMethod;
    private final String contentTypeBehavior;
    private final String credentialsEncrypted;
    private final Integer workerConcurrency;

    // Overridable (Subscription > SubscriberConfig)
    private final String pathTemplate;
    private final String queryTemplate;
    private final String headerTemplate;
    private final String bodyTemplate;
    private final Integer connectTimeoutMs;
    private final Integer readTimeoutMs;
    private final String successHttpCodes;
    private final String successBodyPattern;
    private final String successBodyMatchMode;
    private final Integer successCaseSensitive;
    private final Integer maxRetryCount;
    private final Integer retryBackoffInitialMs;
    private final BigDecimal retryBackoffMultiplier;
    private final Integer retryBackoffMaxMs;

    private EffectiveConfig(Builder b) {
        this.subscriberCode = b.subscriberCode;
        this.baseUrl = b.baseUrl;
        this.httpMethod = b.httpMethod;
        this.contentTypeBehavior = b.contentTypeBehavior;
        this.credentialsEncrypted = b.credentialsEncrypted;
        this.workerConcurrency = b.workerConcurrency;
        this.pathTemplate = b.pathTemplate;
        this.queryTemplate = b.queryTemplate;
        this.headerTemplate = b.headerTemplate;
        this.bodyTemplate = b.bodyTemplate;
        this.connectTimeoutMs = b.connectTimeoutMs;
        this.readTimeoutMs = b.readTimeoutMs;
        this.successHttpCodes = b.successHttpCodes;
        this.successBodyPattern = b.successBodyPattern;
        this.successBodyMatchMode = b.successBodyMatchMode;
        this.successCaseSensitive = b.successCaseSensitive;
        this.maxRetryCount = b.maxRetryCount;
        this.retryBackoffInitialMs = b.retryBackoffInitialMs;
        this.retryBackoffMultiplier = b.retryBackoffMultiplier;
        this.retryBackoffMaxMs = b.retryBackoffMaxMs;
    }

    public static Builder builder() { return new Builder(); }

    // --- Getters ---
    public String getSubscriberCode() { return subscriberCode; }
    public String getBaseUrl() { return baseUrl; }
    public String getHttpMethod() { return httpMethod; }
    public String getContentTypeBehavior() { return contentTypeBehavior; }
    public String getCredentialsEncrypted() { return credentialsEncrypted; }
    public Integer getWorkerConcurrency() { return workerConcurrency; }
    public String getPathTemplate() { return pathTemplate; }
    public String getQueryTemplate() { return queryTemplate; }
    public String getHeaderTemplate() { return headerTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public Integer getConnectTimeoutMs() { return connectTimeoutMs; }
    public Integer getReadTimeoutMs() { return readTimeoutMs; }
    public String getSuccessHttpCodes() { return successHttpCodes; }
    public String getSuccessBodyPattern() { return successBodyPattern; }
    public String getSuccessBodyMatchMode() { return successBodyMatchMode; }
    public Integer getSuccessCaseSensitive() { return successCaseSensitive; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public Integer getRetryBackoffInitialMs() { return retryBackoffInitialMs; }
    public BigDecimal getRetryBackoffMultiplier() { return retryBackoffMultiplier; }
    public Integer getRetryBackoffMaxMs() { return retryBackoffMaxMs; }

    public static class Builder {
        private String subscriberCode;
        private String baseUrl;
        private String httpMethod;
        private String contentTypeBehavior;
        private String credentialsEncrypted;
        private Integer workerConcurrency;
        private String pathTemplate;
        private String queryTemplate;
        private String headerTemplate;
        private String bodyTemplate;
        private Integer connectTimeoutMs;
        private Integer readTimeoutMs;
        private String successHttpCodes;
        private String successBodyPattern;
        private String successBodyMatchMode;
        private Integer successCaseSensitive;
        private Integer maxRetryCount;
        private Integer retryBackoffInitialMs;
        private BigDecimal retryBackoffMultiplier;
        private Integer retryBackoffMaxMs;

        public Builder subscriberCode(String v) { this.subscriberCode = v; return this; }
        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        public Builder httpMethod(String v) { this.httpMethod = v; return this; }
        public Builder contentTypeBehavior(String v) { this.contentTypeBehavior = v; return this; }
        public Builder credentialsEncrypted(String v) { this.credentialsEncrypted = v; return this; }
        public Builder workerConcurrency(Integer v) { this.workerConcurrency = v; return this; }
        public Builder pathTemplate(String v) { this.pathTemplate = v; return this; }
        public Builder queryTemplate(String v) { this.queryTemplate = v; return this; }
        public Builder headerTemplate(String v) { this.headerTemplate = v; return this; }
        public Builder bodyTemplate(String v) { this.bodyTemplate = v; return this; }
        public Builder connectTimeoutMs(Integer v) { this.connectTimeoutMs = v; return this; }
        public Builder readTimeoutMs(Integer v) { this.readTimeoutMs = v; return this; }
        public Builder successHttpCodes(String v) { this.successHttpCodes = v; return this; }
        public Builder successBodyPattern(String v) { this.successBodyPattern = v; return this; }
        public Builder successBodyMatchMode(String v) { this.successBodyMatchMode = v; return this; }
        public Builder successCaseSensitive(Integer v) { this.successCaseSensitive = v; return this; }
        public Builder maxRetryCount(Integer v) { this.maxRetryCount = v; return this; }
        public Builder retryBackoffInitialMs(Integer v) { this.retryBackoffInitialMs = v; return this; }
        public Builder retryBackoffMultiplier(BigDecimal v) { this.retryBackoffMultiplier = v; return this; }
        public Builder retryBackoffMaxMs(Integer v) { this.retryBackoffMaxMs = v; return this; }
        public EffectiveConfig build() { return new EffectiveConfig(this); }
    }
}
```

- [ ] **Step 4: Write EffectiveConfigResolver**

```java
package com.rc.notification.domain.subscription;

import com.rc.notification.domain.config.SupplierConfig;

/**
 * 配置合并解析器
 * <p>
 * 合并 SubscriberConfig (通道默认) 与 Subscription (事件级覆盖)，
 * 产出不可变的 EffectiveConfig 供投递链路使用。
 */
public final class EffectiveConfigResolver {

    private EffectiveConfigResolver() {}

    /**
     * 合并配置
     *
     * @param base SubscriberConfig (原 SupplierConfig)，不为 null
     * @param sub  Subscription 事件级覆盖，可为 null (v1 兼容时)
     * @return 合并后的生效配置
     */
    public static EffectiveConfig resolve(SupplierConfig base, Subscription sub) {
        if (sub == null) {
            return fromBase(base);
        }
        return EffectiveConfig.builder()
                // Channel-level: always from base
                .subscriberCode(base.getSupplierCode())
                .baseUrl(base.getBaseUrl())
                .httpMethod(base.getHttpMethod())
                .contentTypeBehavior(base.getContentTypeBehavior())
                .credentialsEncrypted(base.getCredentialsEncrypted())
                .workerConcurrency(base.getWorkerConcurrency())
                // Overridable: subscription > base
                .pathTemplate(coalesce(sub.getPathTemplate(), base.getPathTemplate()))
                .queryTemplate(coalesce(sub.getQueryTemplate(), base.getQueryTemplate()))
                .headerTemplate(coalesce(sub.getHeaderTemplate(), base.getHeaderTemplate()))
                .bodyTemplate(coalesce(sub.getBodyTemplate(), base.getBodyTemplate()))
                .connectTimeoutMs(coalesce(sub.getConnectTimeoutMs(), base.getConnectTimeoutMs()))
                .readTimeoutMs(coalesce(sub.getReadTimeoutMs(), base.getReadTimeoutMs()))
                .successHttpCodes(coalesce(sub.getSuccessHttpCodes(), base.getSuccessHttpCodes()))
                .successBodyPattern(coalesce(sub.getSuccessBodyPattern(), base.getSuccessBodyPattern()))
                .successBodyMatchMode(coalesce(sub.getSuccessBodyMatchMode(), base.getSuccessBodyMatchMode()))
                .successCaseSensitive(base.getSuccessCaseSensitive())
                .maxRetryCount(coalesce(sub.getMaxRetryCount(), base.getMaxRetryCount()))
                .retryBackoffInitialMs(coalesce(sub.getRetryBackoffInitialMs(), base.getRetryBackoffInitialMs()))
                .retryBackoffMultiplier(coalesce(sub.getRetryBackoffMultiplier(), base.getRetryBackoffMultiplier()))
                .retryBackoffMaxMs(coalesce(sub.getRetryBackoffMaxMs(), base.getRetryBackoffMaxMs()))
                .build();
    }

    private static EffectiveConfig fromBase(SupplierConfig base) {
        return EffectiveConfig.builder()
                .subscriberCode(base.getSupplierCode())
                .baseUrl(base.getBaseUrl())
                .httpMethod(base.getHttpMethod())
                .contentTypeBehavior(base.getContentTypeBehavior())
                .credentialsEncrypted(base.getCredentialsEncrypted())
                .workerConcurrency(base.getWorkerConcurrency())
                .pathTemplate(base.getPathTemplate())
                .queryTemplate(base.getQueryTemplate())
                .headerTemplate(base.getHeaderTemplate())
                .bodyTemplate(base.getBodyTemplate())
                .connectTimeoutMs(base.getConnectTimeoutMs())
                .readTimeoutMs(base.getReadTimeoutMs())
                .successHttpCodes(base.getSuccessHttpCodes())
                .successBodyPattern(base.getSuccessBodyPattern())
                .successBodyMatchMode(base.getSuccessBodyMatchMode())
                .successCaseSensitive(base.getSuccessCaseSensitive())
                .maxRetryCount(base.getMaxRetryCount())
                .retryBackoffInitialMs(base.getRetryBackoffInitialMs())
                .retryBackoffMultiplier(base.getRetryBackoffMultiplier())
                .retryBackoffMaxMs(base.getRetryBackoffMaxMs())
                .build();
    }

    private static <T> T coalesce(T override, T fallback) {
        return override != null ? override : fallback;
    }
}
```

- [ ] **Step 5: Run tests**

Run: `cd notification-service && mvn test -q`
Expected: BUILD SUCCESS, all tests pass including new EffectiveConfigResolverTest (3 tests)

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add EffectiveConfig and EffectiveConfigResolver with config merge logic"
```

---

## Phase 2: Admin CRUD APIs

### Task 5: Publisher Admin Service & Controller

**Files:**
- Create: `src/main/java/com/rc/notification/interfaces/admin/dto/PublisherDto.java`
- Create: `src/main/java/com/rc/notification/interfaces/admin/dto/PublisherCreateRequest.java`
- Create: `src/main/java/com/rc/notification/interfaces/admin/dto/PublisherUpdateRequest.java`
- Create: `src/main/java/com/rc/notification/application/admin/PublisherAdminService.java`
- Create: `src/main/java/com/rc/notification/interfaces/admin/PublisherController.java`
- Create: `src/test/java/com/rc/notification/integration/PublisherCrudIntegrationTest.java`

- [ ] **Step 1: Write failing integration test**

```java
package com.rc.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PublisherCrudIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        session = AuthIntegrationTest.loginAndGetSession(mockMvc, objectMapper);
    }

    @Test
    @DisplayName("Create publisher returns 200 with generated apiKey")
    void createPublisher() throws Exception {
        var req = Map.of(
                "publisherCode", "order-service",
                "publisherName", "Order Service",
                "contactInfo", "dev@example.com"
        );

        mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publisherCode").value("order-service"))
                .andExpect(jsonPath("$.apiKey").isNotEmpty())
                .andExpect(jsonPath("$.status").value(1));
    }

    @Test
    @DisplayName("List publishers with pagination")
    void listPublishers() throws Exception {
        createPublisherViaApi("pub-a", "Publisher A");
        createPublisherViaApi("pub-b", "Publisher B");

        mockMvc.perform(get("/api/v1/admin/publishers")
                        .session(session)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @DisplayName("Duplicate publisherCode returns 400")
    void duplicatePublisherCode() throws Exception {
        createPublisherViaApi("dup-code", "First");

        var req = Map.of("publisherCode", "dup-code", "publisherName", "Second");
        mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Rotate API key returns new key")
    void rotateApiKey() throws Exception {
        String body = createPublisherViaApi("rotate-test", "Rotate Test");
        Long id = objectMapper.readTree(body).get("id").asLong();

        String originalKey = objectMapper.readTree(body).get("apiKey").asText();

        mockMvc.perform(post("/api/v1/admin/publishers/" + id + "/rotate-key")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").isNotEmpty());
        // Note: new key should differ from original (verified by non-empty)
    }

    private String createPublisherViaApi(String code, String name) throws Exception {
        var req = Map.of("publisherCode", code, "publisherName", name);
        return mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd notification-service && mvn test -Dtest=PublisherCrudIntegrationTest -q`
Expected: FAIL — controller endpoint not found (404)

- [ ] **Step 3: Write DTOs**

`PublisherCreateRequest.java`:
```java
package com.rc.notification.interfaces.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class PublisherCreateRequest {
    @NotBlank(message = "publisherCode 不能为空")
    private String publisherCode;
    @NotBlank(message = "publisherName 不能为空")
    private String publisherName;
    private String contactInfo;

    public String getPublisherCode() { return publisherCode; }
    public void setPublisherCode(String publisherCode) { this.publisherCode = publisherCode; }
    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
}
```

`PublisherUpdateRequest.java`:
```java
package com.rc.notification.interfaces.admin.dto;

public class PublisherUpdateRequest {
    private String publisherName;
    private String contactInfo;
    private Integer status;

    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
```

`PublisherDto.java`:
```java
package com.rc.notification.interfaces.admin.dto;

import java.time.LocalDateTime;

public class PublisherDto {
    private Long id;
    private String publisherCode;
    private String publisherName;
    private String apiKey;
    private Integer status;
    private String contactInfo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublisherCode() { return publisherCode; }
    public void setPublisherCode(String publisherCode) { this.publisherCode = publisherCode; }
    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 4: Write PublisherAdminService**

```java
package com.rc.notification.application.admin;

import com.rc.notification.domain.publisher.Publisher;
import com.rc.notification.domain.publisher.PublisherRepository;
import com.rc.notification.interfaces.admin.dto.PageResult;
import com.rc.notification.interfaces.admin.dto.PublisherCreateRequest;
import com.rc.notification.interfaces.admin.dto.PublisherDto;
import com.rc.notification.interfaces.admin.dto.PublisherUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PublisherAdminService {

    private static final Logger log = LoggerFactory.getLogger(PublisherAdminService.class);
    private final PublisherRepository publisherRepository;

    public PublisherAdminService(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }

    public PageResult<PublisherDto> listPublishers(String keyword, Integer status, int page, int size) {
        List<Publisher> list = publisherRepository.findByFilters(keyword, status, page, size);
        long total = publisherRepository.countByFilters(keyword, status);
        return new PageResult<>(list.stream().map(this::toDto).toList(), total, page, size);
    }

    public PublisherDto getPublisher(Long id) {
        Publisher p = publisherRepository.findById(id);
        if (p == null) throw new IllegalArgumentException("发布方不存在: id=" + id);
        return toDto(p);
    }

    public PublisherDto createPublisher(PublisherCreateRequest request) {
        if (publisherRepository.existsByPublisherCode(request.getPublisherCode())) {
            throw new IllegalArgumentException("发布方编码已存在: " + request.getPublisherCode());
        }
        Publisher p = new Publisher();
        p.setPublisherCode(request.getPublisherCode());
        p.setPublisherName(request.getPublisherName());
        p.setContactInfo(request.getContactInfo());
        p.setApiKey(generateApiKey());
        p.setStatus(1);
        publisherRepository.save(p);
        log.info("新增发布方: publisherCode={}", request.getPublisherCode());
        return toDto(p);
    }

    public PublisherDto updatePublisher(Long id, PublisherUpdateRequest request) {
        Publisher p = publisherRepository.findById(id);
        if (p == null) throw new IllegalArgumentException("发布方不存在: id=" + id);
        if (request.getPublisherName() != null) p.setPublisherName(request.getPublisherName());
        if (request.getContactInfo() != null) p.setContactInfo(request.getContactInfo());
        if (request.getStatus() != null) p.setStatus(request.getStatus());
        publisherRepository.update(p);
        log.info("更新发布方: id={}, publisherCode={}", id, p.getPublisherCode());
        return toDto(p);
    }

    public PublisherDto rotateApiKey(Long id) {
        Publisher p = publisherRepository.findById(id);
        if (p == null) throw new IllegalArgumentException("发布方不存在: id=" + id);
        p.setApiKey(generateApiKey());
        publisherRepository.update(p);
        log.info("轮换发布方 API Key: id={}, publisherCode={}", id, p.getPublisherCode());
        return toDto(p);
    }

    private String generateApiKey() {
        return "pk_" + UUID.randomUUID().toString().replace("-", "");
    }

    private PublisherDto toDto(Publisher p) {
        PublisherDto dto = new PublisherDto();
        dto.setId(p.getId());
        dto.setPublisherCode(p.getPublisherCode());
        dto.setPublisherName(p.getPublisherName());
        dto.setApiKey(p.getApiKey());
        dto.setStatus(p.getStatus());
        dto.setContactInfo(p.getContactInfo());
        dto.setCreateTime(p.getCreateTime());
        dto.setUpdateTime(p.getUpdateTime());
        return dto;
    }
}
```

- [ ] **Step 5: Write PublisherController**

```java
package com.rc.notification.interfaces.admin;

import com.rc.notification.application.admin.PublisherAdminService;
import com.rc.notification.interfaces.admin.dto.PageResult;
import com.rc.notification.interfaces.admin.dto.PublisherCreateRequest;
import com.rc.notification.interfaces.admin.dto.PublisherDto;
import com.rc.notification.interfaces.admin.dto.PublisherUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/publishers")
public class PublisherController {

    private final PublisherAdminService publisherAdminService;

    public PublisherController(PublisherAdminService publisherAdminService) {
        this.publisherAdminService = publisherAdminService;
    }

    @GetMapping
    public PageResult<PublisherDto> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return publisherAdminService.listPublishers(keyword, status, page, size);
    }

    @GetMapping("/{id}")
    public PublisherDto get(@PathVariable Long id) {
        return publisherAdminService.getPublisher(id);
    }

    @PostMapping
    public PublisherDto create(@Valid @RequestBody PublisherCreateRequest request) {
        return publisherAdminService.createPublisher(request);
    }

    @PutMapping("/{id}")
    public PublisherDto update(@PathVariable Long id, @RequestBody PublisherUpdateRequest request) {
        return publisherAdminService.updatePublisher(id, request);
    }

    @PostMapping("/{id}/rotate-key")
    public PublisherDto rotateKey(@PathVariable Long id) {
        return publisherAdminService.rotateApiKey(id);
    }
}
```

- [ ] **Step 6: Run tests**

Run: `cd notification-service && mvn test -q`
Expected: BUILD SUCCESS, all tests pass including PublisherCrudIntegrationTest (4 tests)

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add Publisher admin CRUD API with integration tests"
```

---

### Task 6: EventType Admin Service & Controller

**Files:**
- Create: `src/main/java/com/rc/notification/interfaces/admin/dto/EventTypeDto.java`
- Create: `src/main/java/com/rc/notification/interfaces/admin/dto/EventTypeCreateRequest.java`
- Create: `src/main/java/com/rc/notification/interfaces/admin/dto/EventTypeUpdateRequest.java`
- Create: `src/main/java/com/rc/notification/application/admin/EventTypeAdminService.java`
- Create: `src/main/java/com/rc/notification/interfaces/admin/EventTypeController.java`
- Create: `src/test/java/com/rc/notification/integration/EventTypeCrudIntegrationTest.java`

This task follows the exact same TDD pattern as Task 5. DTOs, service, controller, and integration test for EventType CRUD at `/api/v1/admin/event-types`.

- [ ] **Step 1: Write failing integration test**

Test covers: create event type, list by publisher, duplicate code rejection, update schema (version increment), deprecate event type.

Key test structure:
```java
// Setup: create a publisher first, then register event types under it
// Test create: POST /api/v1/admin/event-types { eventTypeCode, publisherCode, displayName, status: "DRAFT" }
// Test list: GET /api/v1/admin/event-types?publisherCode=order-service
// Test activate: PUT /api/v1/admin/event-types/{id} { status: "ACTIVE" }
// Test schema update: PUT /api/v1/admin/event-types/{id} { payloadSchema: "..." } -> version increments
// Test deprecate: PUT /api/v1/admin/event-types/{id} { status: "DEPRECATED" }
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Write EventType DTOs (EventTypeCreateRequest, EventTypeUpdateRequest, EventTypeDto)**

`EventTypeCreateRequest` fields: `eventTypeCode`, `publisherCode`, `displayName`, `description`, `payloadSchema`

`EventTypeUpdateRequest` fields: `displayName`, `description`, `payloadSchema`, `status`

`EventTypeDto` fields: all EventType domain model fields

- [ ] **Step 4: Write EventTypeAdminService**

Key logic:
- `createEventType`: validates publisherCode exists, checks eventTypeCode uniqueness, sets initial status to "DRAFT", version=1
- `updateEventType`: if payloadSchema changed, increment version
- Lookup helpers for v2 ingest: `findActiveByCode(eventTypeCode)` for validating incoming events

- [ ] **Step 5: Write EventTypeController at `/api/v1/admin/event-types`**

Standard REST CRUD: GET (list), GET/{id}, POST, PUT/{id}

- [ ] **Step 6: Run tests**

Run: `cd notification-service && mvn test -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add EventType admin CRUD API with integration tests"
```

---

### Task 7: Subscription Admin Service & Controller

**Files:**
- Create: `src/main/java/com/rc/notification/interfaces/admin/dto/SubscriptionDto.java`
- Create: `src/main/java/com/rc/notification/interfaces/admin/dto/SubscriptionCreateRequest.java`
- Create: `src/main/java/com/rc/notification/interfaces/admin/dto/SubscriptionUpdateRequest.java`
- Create: `src/main/java/com/rc/notification/application/admin/SubscriptionAdminService.java`
- Create: `src/main/java/com/rc/notification/interfaces/admin/SubscriptionController.java`
- Create: `src/test/java/com/rc/notification/integration/SubscriptionCrudIntegrationTest.java`

- [ ] **Step 1: Write failing integration test**

Test covers: create subscription (validates subscriber and event type exist), list by subscriber, list by event type, update override templates, suspend/resume subscription, duplicate subscriber+eventType rejection.

Key test structure:
```java
// Setup: create publisher -> create event type (ACTIVE) -> create subscriber (via existing supplier API)
// Test create: POST /api/v1/admin/subscriptions { subscriberCode, eventTypeCode, managedBy: "SUBSCRIBER" }
// Test with override: POST /api/v1/admin/subscriptions { ..., bodyTemplate: "override", readTimeoutMs: 10000 }
// Test list by subscriber: GET /api/v1/admin/subscriptions?subscriberCode=ALIYUN
// Test suspend: PUT /api/v1/admin/subscriptions/{id} { status: "SUSPENDED" }
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Write Subscription DTOs**

`SubscriptionCreateRequest` fields: `subscriberCode`, `eventTypeCode`, `managedBy`, plus all overridable template/timeout/retry fields (all optional)

`SubscriptionUpdateRequest` fields: `status`, `managedBy`, plus all overridable fields

`SubscriptionDto` fields: all Subscription domain model fields

- [ ] **Step 4: Write SubscriptionAdminService**

Key logic:
- `createSubscription`: validates subscriberCode exists in supplier_config, validates eventTypeCode exists and is ACTIVE, checks uniqueness of (subscriberCode, eventTypeCode), sets default managedBy based on caller context
- `updateSubscription`: partial update of overridable fields

- [ ] **Step 5: Write SubscriptionController at `/api/v1/admin/subscriptions`**

Standard REST CRUD: GET (list), GET/{id}, POST, PUT/{id}

- [ ] **Step 6: Run tests**

Run: `cd notification-service && mvn test -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add Subscription admin CRUD API with integration tests"
```

---

## Phase 3: Ingest v2 & Fan-out

### Task 8: Publisher Authentication Filter

**Files:**
- Create: `src/main/java/com/rc/notification/interfaces/api/PublisherAuthFilter.java`
- Create: `src/main/java/com/rc/notification/interfaces/api/PublisherAuthFilterConfig.java`

- [ ] **Step 1: Write PublisherAuthFilter**

```java
package com.rc.notification.interfaces.api;

import com.rc.notification.domain.publisher.Publisher;
import com.rc.notification.domain.publisher.PublisherRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * v2 API 发布方鉴权过滤器
 * <p>
 * 校验 X-Publisher-Key header，将 publisherCode 注入 request attribute
 */
public class PublisherAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PublisherAuthFilter.class);
    public static final String PUBLISHER_CODE_ATTR = "publisherCode";
    private static final String API_KEY_HEADER = "X-Publisher-Key";

    private final PublisherRepository publisherRepository;

    public PublisherAuthFilter(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"REJECTED\",\"message\":\"Missing X-Publisher-Key header\"}");
            return;
        }

        Publisher publisher = publisherRepository.findByApiKey(apiKey);
        if (publisher == null || publisher.getStatus() == null || publisher.getStatus() != 1) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"REJECTED\",\"message\":\"Invalid or disabled publisher key\"}");
            return;
        }

        request.setAttribute(PUBLISHER_CODE_ATTR, publisher.getPublisherCode());
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Write PublisherAuthFilterConfig**

```java
package com.rc.notification.interfaces.api;

import com.rc.notification.domain.publisher.PublisherRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherAuthFilterConfig {

    @Bean
    public FilterRegistrationBean<PublisherAuthFilter> publisherAuthFilterRegistration(
            PublisherRepository publisherRepository) {
        FilterRegistrationBean<PublisherAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new PublisherAuthFilter(publisherRepository));
        reg.addUrlPatterns("/api/v2/*");
        reg.setOrder(2);
        return reg;
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd notification-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add PublisherAuthFilter for v2 API authentication"
```

---

### Task 9: Ingest v2 API with Fan-out

**Files:**
- Create: `src/main/java/com/rc/notification/interfaces/api/dto/IngestV2Request.java`
- Create: `src/main/java/com/rc/notification/interfaces/api/dto/IngestV2Response.java`
- Create: `src/main/java/com/rc/notification/interfaces/api/dto/DispatchDetail.java`
- Create: `src/main/java/com/rc/notification/interfaces/api/EventIngestionV2Controller.java`
- Modify: `src/main/java/com/rc/notification/application/service/IngestionService.java`
- Create: `src/test/java/com/rc/notification/integration/IngestV2IntegrationTest.java`

- [ ] **Step 1: Write failing integration test**

```java
package com.rc.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class IngestV2IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestConfig testConfig;

    private String publisherApiKey;

    @BeforeEach
    void setUp() throws Exception {
        testConfig.clearAll();
        MockHttpSession session = AuthIntegrationTest.loginAndGetSession(mockMvc, objectMapper);

        // Create publisher
        var pubReq = Map.of("publisherCode", "order-service", "publisherName", "Order Service");
        String pubBody = mockMvc.perform(post("/api/v1/admin/publishers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pubReq)))
                .andReturn().getResponse().getContentAsString();
        publisherApiKey = objectMapper.readTree(pubBody).get("apiKey").asText();

        // Create event type
        var etReq = Map.of("eventTypeCode", "ORDER_CREATED",
                "publisherCode", "order-service",
                "displayName", "Order Created");
        mockMvc.perform(post("/api/v1/admin/event-types")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(etReq)));
        // Activate event type
        Long etId = objectMapper.readTree(mockMvc.perform(post("/api/v1/admin/event-types")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(etReq)))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        // (activation via update endpoint)

        // Create two subscribers
        createSubscriber(session, "SUB_A");
        createSubscriber(session, "SUB_B");

        // Create subscriptions
        createSubscription(session, "SUB_A", "ORDER_CREATED");
        createSubscription(session, "SUB_B", "ORDER_CREATED");
    }

    @Test
    @DisplayName("Fan-out: event dispatched to all subscribers")
    void fanOutToAllSubscribers() throws Exception {
        var req = Map.of(
                "eventId", "evt-fanout-001",
                "eventType", "ORDER_CREATED",
                "payload", Map.of("orderId", "123")
        );

        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .header("X-Publisher-Key", publisherApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.dispatches.length()").value(2));
    }

    @Test
    @DisplayName("Targeted: event dispatched to specified subscriber only")
    void targetedDelivery() throws Exception {
        var req = Map.of(
                "eventId", "evt-targeted-001",
                "eventType", "ORDER_CREATED",
                "subscriberCode", "SUB_A",
                "payload", Map.of("orderId", "456")
        );

        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .header("X-Publisher-Key", publisherApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatches.length()").value(1))
                .andExpect(jsonPath("$.dispatches[0].subscriberCode").value("SUB_A"));
    }

    @Test
    @DisplayName("Missing X-Publisher-Key returns 401")
    void missingApiKey() throws Exception {
        var req = Map.of("eventId", "evt-nokey", "eventType", "ORDER_CREATED",
                "payload", Map.of("x", 1));

        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("No subscribers: returns ACCEPTED with empty dispatches")
    void noSubscribers() throws Exception {
        // Create event type with no subscriptions
        MockHttpSession session = AuthIntegrationTest.loginAndGetSession(mockMvc, objectMapper);
        var etReq = Map.of("eventTypeCode", "LONELY_EVENT",
                "publisherCode", "order-service",
                "displayName", "Lonely Event");
        mockMvc.perform(post("/api/v1/admin/event-types")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(etReq)));

        var req = Map.of("eventId", "evt-lonely", "eventType", "LONELY_EVENT",
                "payload", Map.of("x", 1));

        mockMvc.perform(post("/api/v2/notifications/ingest")
                        .header("X-Publisher-Key", publisherApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.dispatches.length()").value(0));
    }

    private void createSubscriber(MockHttpSession session, String code) throws Exception {
        var req = new com.rc.notification.interfaces.admin.dto.SupplierConfigCreateRequest();
        req.setSupplierCode(code);
        req.setSupplierName("Test " + code);
        req.setBaseUrl("https://api.test.com");
        req.setBodyTemplate("'{\"test\": true}'");
        req.setStatus(1);
        req.setMaxRetryCount(3);
        req.setRetryBackoffInitialMs(1000);
        req.setRetryBackoffMultiplier(new BigDecimal("2.00"));
        req.setRetryBackoffMaxMs(30000);
        req.setWorkerConcurrency(1);
        mockMvc.perform(post("/api/v1/admin/suppliers")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    private void createSubscription(MockHttpSession session, String subscriberCode, String eventTypeCode) throws Exception {
        var req = Map.of("subscriberCode", subscriberCode, "eventTypeCode", eventTypeCode);
        mockMvc.perform(post("/api/v1/admin/subscriptions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Write v2 DTOs**

`IngestV2Request.java`:
```java
package com.rc.notification.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class IngestV2Request {
    @NotBlank(message = "eventId 不能为空")
    private String eventId;
    @NotBlank(message = "eventType 不能为空")
    private String eventType;
    @NotNull(message = "payload 不能为空")
    private Map<String, Object> payload;
    private String subscriberCode;  // optional, targeted delivery
    private String traceId;         // optional

    // --- Getters & Setters ---
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    public String getSubscriberCode() { return subscriberCode; }
    public void setSubscriberCode(String subscriberCode) { this.subscriberCode = subscriberCode; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
```

`DispatchDetail.java`:
```java
package com.rc.notification.interfaces.api.dto;

public class DispatchDetail {
    private String subscriberCode;
    private String status;  // QUEUED / IDEMPOTENT_HIT / REJECTED

    public DispatchDetail(String subscriberCode, String status) {
        this.subscriberCode = subscriberCode;
        this.status = status;
    }

    public String getSubscriberCode() { return subscriberCode; }
    public void setSubscriberCode(String subscriberCode) { this.subscriberCode = subscriberCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

`IngestV2Response.java`:
```java
package com.rc.notification.interfaces.api.dto;

import java.util.List;

public class IngestV2Response {
    private String eventId;
    private String status;  // ACCEPTED / REJECTED
    private String message;
    private List<DispatchDetail> dispatches;

    public IngestV2Response(String eventId, String status, String message, List<DispatchDetail> dispatches) {
        this.eventId = eventId;
        this.status = status;
        this.message = message;
        this.dispatches = dispatches;
    }

    public static IngestV2Response accepted(String eventId, List<DispatchDetail> dispatches) {
        return new IngestV2Response(eventId, "ACCEPTED", "事件已接收", dispatches);
    }

    public static IngestV2Response rejected(String eventId, String reason) {
        return new IngestV2Response(eventId, "REJECTED", reason, List.of());
    }

    // --- Getters & Setters ---
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<DispatchDetail> getDispatches() { return dispatches; }
    public void setDispatches(List<DispatchDetail> dispatches) { this.dispatches = dispatches; }
}
```

- [ ] **Step 4: Add `enqueueForSubscriber` method to IngestionService**

Extract the core enqueue logic (lock + idempotency check + queue add) from existing `ingest()` into a reusable method:

```java
/**
 * 为单个订阅方执行入队（供 v2 fan-out 调用）
 * <p>
 * 幂等 key 粒度：eventId:subscriberCode
 *
 * @return dispatch status: "QUEUED" / "IDEMPOTENT_HIT" / "DEAD_LETTERED"
 */
public String enqueueForSubscriber(String eventId, String subscriberCode, String eventTypeCode,
                                    Map<String, Object> payload, String traceId) {
    String dispatchId = eventId + ":" + subscriberCode;
    String lockKey = LOCK_PREFIX + dispatchId;
    String statusKey = STATUS_PREFIX + dispatchId;

    RLock lock = redissonClient.getLock(lockKey);
    try {
        boolean acquired = lock.tryLock(0, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (!acquired) {
            return "IDEMPOTENT_HIT";
        }
        try {
            var bucket = redissonClient.getBucket(statusKey);
            Object currentStatus = bucket.get();
            if (currentStatus != null) {
                String statusStr = currentStatus.toString();
                log.info("幂等命中(v2): dispatchId={}, currentStatus={}", dispatchId, statusStr);
                metricsRegistry.recordIngest(subscriberCode, "idempotent_hit");
                return statusStr.equals(STATUS_DEAD_LETTERED) ? "DEAD_LETTERED" : "IDEMPOTENT_HIT";
            }

            bucket.set(STATUS_PROCESSING, Duration.ofHours(STATUS_TTL_HOURS));

            String queueName = QUEUE_PREFIX + subscriberCode;
            RQueue<String> queue = redissonClient.getQueue(queueName);
            String message = serializeV2Event(eventId, subscriberCode, eventTypeCode, payload, traceId);
            try {
                queue.add(message);
            } catch (Exception e) {
                bucket.delete();
                throw e;
            }

            log.info("事件入队成功(v2): dispatchId={}, eventType={}", dispatchId, eventTypeCode);
            metricsRegistry.recordIngest(subscriberCode, "accepted");
            return "QUEUED";
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    } catch (RedisException e) {
        throw new RedisUnavailableException("Redis 不可用", e);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RedisUnavailableException("获取锁被中断", e);
    }
}

private String serializeV2Event(String eventId, String subscriberCode, String eventTypeCode,
                                 Map<String, Object> payload, String traceId) {
    try {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("eventId", eventId);
        eventMap.put("traceId", traceId);
        eventMap.put("supplierCode", subscriberCode);
        eventMap.put("eventTypeCode", eventTypeCode);
        eventMap.put("payload", payload);
        eventMap.put("timestamp", System.currentTimeMillis());
        return objectMapper.writeValueAsString(eventMap);
    } catch (JsonProcessingException e) {
        throw new RuntimeException("事件序列化失败", e);
    }
}
```

- [ ] **Step 5: Write EventIngestionV2Controller**

```java
package com.rc.notification.interfaces.api;

import com.rc.notification.application.service.IngestionService;
import com.rc.notification.domain.event.EventType;
import com.rc.notification.domain.event.EventTypeRepository;
import com.rc.notification.domain.subscription.Subscription;
import com.rc.notification.domain.subscription.SubscriptionRepository;
import com.rc.notification.interfaces.api.dto.DispatchDetail;
import com.rc.notification.interfaces.api.dto.IngestV2Request;
import com.rc.notification.interfaces.api.dto.IngestV2Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class EventIngestionV2Controller {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionV2Controller.class);

    private final IngestionService ingestionService;
    private final EventTypeRepository eventTypeRepository;
    private final SubscriptionRepository subscriptionRepository;

    public EventIngestionV2Controller(IngestionService ingestionService,
                                       EventTypeRepository eventTypeRepository,
                                       SubscriptionRepository subscriptionRepository) {
        this.ingestionService = ingestionService;
        this.eventTypeRepository = eventTypeRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostMapping("/api/v2/notifications/ingest")
    public ResponseEntity<IngestV2Response> ingestV2(
            @Valid @RequestBody IngestV2Request request,
            HttpServletRequest httpRequest) {
        try {
            String publisherCode = (String) httpRequest.getAttribute(PublisherAuthFilter.PUBLISHER_CODE_ATTR);

            // Validate event type
            EventType eventType = eventTypeRepository.findByEventTypeCode(request.getEventType());
            if (eventType == null || !"ACTIVE".equals(eventType.getStatus())) {
                return ResponseEntity.badRequest().body(
                        IngestV2Response.rejected(request.getEventId(),
                                "事件类型不存在或未激活: " + request.getEventType()));
            }
            if (!eventType.getPublisherCode().equals(publisherCode)) {
                return ResponseEntity.badRequest().body(
                        IngestV2Response.rejected(request.getEventId(),
                                "事件类型不属于当前发布方"));
            }

            // Determine targets
            String traceId = request.getTraceId();
            if (traceId == null || traceId.isEmpty()) {
                traceId = "T-" + UUID.randomUUID();
            }

            List<Subscription> targets;
            if (request.getSubscriberCode() != null && !request.getSubscriberCode().isBlank()) {
                Subscription sub = subscriptionRepository.findBySubscriberAndEventType(
                        request.getSubscriberCode(), request.getEventType());
                if (sub == null || !"ACTIVE".equals(sub.getStatus())) {
                    return ResponseEntity.badRequest().body(
                            IngestV2Response.rejected(request.getEventId(),
                                    "订阅关系不存在或已暂停: " + request.getSubscriberCode()));
                }
                targets = List.of(sub);
            } else {
                targets = subscriptionRepository.findActiveByEventTypeCode(request.getEventType());
            }

            // Fan-out: enqueue for each target
            List<DispatchDetail> dispatches = new ArrayList<>();
            for (Subscription sub : targets) {
                String status = ingestionService.enqueueForSubscriber(
                        request.getEventId(), sub.getSubscriberCode(),
                        request.getEventType(), request.getPayload(), traceId);
                dispatches.add(new DispatchDetail(sub.getSubscriberCode(), status));
            }

            return ResponseEntity.ok(IngestV2Response.accepted(request.getEventId(), dispatches));

        } catch (IngestionService.RedisUnavailableException e) {
            log.error("Redis 不可用(v2)，返回 503", e);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Retry-After", "5");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .headers(headers)
                    .body(IngestV2Response.rejected(request.getEventId(), "服务暂时不可用"));
        }
    }
}
```

- [ ] **Step 6: Run tests**

Run: `cd notification-service && mvn test -q`
Expected: BUILD SUCCESS, all tests pass including IngestV2IntegrationTest

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add Ingest v2 API with fan-out routing and publisher authentication"
```

---

## Phase 4: Worker Integration

### Task 10: Worker Config Resolution via Subscription Merge

**Files:**
- Modify: `src/main/java/com/rc/notification/application/worker/DeliveryWorker.java`

- [ ] **Step 1: Modify DeliveryWorker.processEvent to use EffectiveConfigResolver**

In `processEvent()`, after parsing `eventMeta`, check for `eventTypeCode`:

```java
// After line: retryCount = eventMeta.containsKey("retryCount") ? ...

// Resolve effective config (subscription merge)
String eventTypeCode = (String) eventMeta.get("eventTypeCode");
SupplierConfig baseConfig = configDomainService.getBySupplierCode(supplierCode);
if (baseConfig == null || baseConfig.getStatus() == null || baseConfig.getStatus() != 1) {
    log.warn("订阅方配置不存在或已禁用，跳过: supplierCode={}", supplierCode);
    return;
}

EffectiveConfig effectiveConfig;
if (eventTypeCode != null && subscriptionRepository != null) {
    Subscription sub = subscriptionRepository.findBySubscriberAndEventType(supplierCode, eventTypeCode);
    effectiveConfig = EffectiveConfigResolver.resolve(baseConfig, sub);
} else {
    // v1 兼容：无 eventTypeCode，直接使用 base config
    effectiveConfig = EffectiveConfigResolver.resolve(baseConfig, null);
}
```

Then replace all subsequent references to `config` (SupplierConfig) with adapter calls that delegate to `effectiveConfig`. Since `FullStackHttpRequestBuilder.buildRequest` and `deriveClient` currently take `SupplierConfig`, create a thin adapter or modify those methods to accept `EffectiveConfig` as well.

The simplest approach: add a `toSupplierConfig()` method on `EffectiveConfig` that creates a SupplierConfig populated with the merged values, preserving the existing Worker contract.

Add to `EffectiveConfig`:
```java
/**
 * 转换为 SupplierConfig 兼容格式（供现有 Worker 链路使用）
 */
public SupplierConfig toSupplierConfigCompat() {
    SupplierConfig c = new SupplierConfig();
    c.setSupplierCode(this.subscriberCode);
    c.setBaseUrl(this.baseUrl);
    c.setHttpMethod(this.httpMethod);
    c.setContentTypeBehavior(this.contentTypeBehavior);
    c.setCredentialsEncrypted(this.credentialsEncrypted);
    c.setPathTemplate(this.pathTemplate);
    c.setQueryTemplate(this.queryTemplate);
    c.setHeaderTemplate(this.headerTemplate);
    c.setBodyTemplate(this.bodyTemplate);
    c.setConnectTimeoutMs(this.connectTimeoutMs);
    c.setReadTimeoutMs(this.readTimeoutMs);
    c.setSuccessHttpCodes(this.successHttpCodes);
    c.setSuccessBodyPattern(this.successBodyPattern);
    c.setSuccessBodyMatchMode(this.successBodyMatchMode);
    c.setSuccessCaseSensitive(this.successCaseSensitive);
    c.setMaxRetryCount(this.maxRetryCount);
    c.setRetryBackoffInitialMs(this.retryBackoffInitialMs);
    c.setRetryBackoffMultiplier(this.retryBackoffMultiplier);
    c.setRetryBackoffMaxMs(this.retryBackoffMaxMs);
    c.setWorkerConcurrency(this.workerConcurrency);
    c.setStatus(1);
    return c;
}
```

- [ ] **Step 2: Add SubscriptionRepository dependency to DeliveryWorker**

Add constructor parameter and field:
```java
private final SubscriptionRepository subscriptionRepository;  // nullable for v1 compat
```

Update `DeliveryWorkerFactoryImpl` to inject it.

- [ ] **Step 3: Verify existing tests still pass**

Run: `cd notification-service && mvn test -q`
Expected: BUILD SUCCESS — all existing tests pass (v1 messages have no eventTypeCode, falls back to base config)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: integrate EffectiveConfigResolver into DeliveryWorker for subscription-aware config"
```

---

## Phase 5: Event Change Detection

### Task 11: FieldFingerprint & ChangeRecord Entities

**Files:**
- Create: `src/main/resources/db/migration/V6__create_field_fingerprint_and_change_record.sql`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/entity/FieldFingerprintEntity.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/entity/ChangeRecordEntity.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/mapper/FieldFingerprintMapper.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/mapper/ChangeRecordMapper.java`
- Create: `src/main/java/com/rc/notification/domain/detection/FieldFingerprint.java`
- Create: `src/main/java/com/rc/notification/domain/detection/ChangeRecord.java`
- Create: `src/main/java/com/rc/notification/domain/detection/FieldFingerprintRepository.java`
- Create: `src/main/java/com/rc/notification/domain/detection/ChangeRecordRepository.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/FieldFingerprintRepositoryImpl.java`
- Create: `src/main/java/com/rc/notification/infrastructure/persistence/ChangeRecordRepositoryImpl.java`
- Modify: `src/test/resources/schema.sql`
- Modify: `src/test/resources/cleanup.sql`

This task follows the same entity+mapper+repository pattern as Tasks 1-3. The migration SQL:

```sql
-- V6__create_field_fingerprint_and_change_record.sql
CREATE TABLE `field_fingerprint` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_type_code` VARCHAR(128) NOT NULL,
    `field_path` VARCHAR(256) NOT NULL,
    `observed_type` VARCHAR(16) NOT NULL COMMENT 'STRING/NUMBER/BOOLEAN/OBJECT/ARRAY/NULL',
    `first_seen_at` DATETIME NOT NULL,
    `last_seen_at` DATETIME NOT NULL,
    `sample_count` INT NOT NULL DEFAULT 1,
    `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISAPPEARED',
    UNIQUE KEY `uk_event_field` (`event_type_code`, `field_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件字段指纹表(运行时检测)';

CREATE TABLE `change_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_type_code` VARCHAR(128) NOT NULL,
    `change_type` VARCHAR(32) NOT NULL COMMENT 'FIELD_ADDED/FIELD_REMOVED/FIELD_TYPE_CHANGED/SCHEMA_UPDATED',
    `field_path` VARCHAR(256) DEFAULT NULL,
    `old_value` VARCHAR(512) DEFAULT NULL,
    `new_value` VARCHAR(512) DEFAULT NULL,
    `detection_source` VARCHAR(16) NOT NULL COMMENT 'SCHEMA_DIFF/RUNTIME_INFERRED',
    `confidence` VARCHAR(8) NOT NULL DEFAULT 'MEDIUM',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING_REVIEW',
    `affected_subscriptions` TEXT DEFAULT NULL COMMENT 'JSON array of affected subscriberCodes',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_event_type_code` (`event_type_code`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件变更记录表';
```

- [ ] **Step 1: Write migration, entities, mappers, domain models, repositories**
- [ ] **Step 2: Update test schema.sql and cleanup.sql**
- [ ] **Step 3: Verify compilation and existing tests**

Run: `cd notification-service && mvn test -q`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add FieldFingerprint and ChangeRecord entities for event change detection"
```

---

### Task 12: Runtime Field Sampling Service

**Files:**
- Create: `src/main/java/com/rc/notification/application/detection/FieldSamplingService.java`
- Create: `src/test/java/com/rc/notification/integration/FieldSamplingIntegrationTest.java`
- Modify: `src/main/java/com/rc/notification/interfaces/api/EventIngestionV2Controller.java`

- [ ] **Step 1: Write failing test**

```java
// Test: given a known fingerprint set for ORDER_CREATED,
// when sampling a payload with a new field "address.city",
// then a ChangeRecord(FIELD_ADDED) is created and FieldFingerprint is inserted
```

- [ ] **Step 2: Write FieldSamplingService**

Key logic:
```java
@Service
public class FieldSamplingService {

    private final FieldFingerprintRepository fingerprintRepo;
    private final ChangeRecordRepository changeRecordRepo;
    private final AtomicMap<String, AtomicLong> sampleCounters = new ConcurrentHashMap<>();

    /**
     * 异步采样：提取 payload 字段路径，与已知指纹对比
     */
    @Async
    public void sampleAsync(String eventTypeCode, Map<String, Object> payload) {
        // Sampling rate: first 100 -> 100%, then 1%
        long count = sampleCounters
                .computeIfAbsent(eventTypeCode, k -> new AtomicLong(0))
                .incrementAndGet();
        if (count > 100 && ThreadLocalRandom.current().nextInt(100) != 0) {
            return;
        }

        // Extract all field paths from payload
        Map<String, String> fieldPaths = extractFieldPaths("payload", payload);

        // Compare against known fingerprints
        for (Map.Entry<String, String> entry : fieldPaths.entrySet()) {
            String path = entry.getKey();
            String type = entry.getValue();
            upsertFingerprint(eventTypeCode, path, type);
        }
    }

    // extractFieldPaths: recursively walk payload, return Map<jsonPath, type>
    // upsertFingerprint: insert or update FieldFingerprint, create ChangeRecord if new
}
```

- [ ] **Step 3: Wire into EventIngestionV2Controller**

After determining targets, before enqueue loop:
```java
// Async field sampling (does not block enqueue)
fieldSamplingService.sampleAsync(request.getEventType(), request.getPayload());
```

- [ ] **Step 4: Run tests**

Run: `cd notification-service && mvn test -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add runtime field sampling for event change detection"
```

---

### Task 13: Change Detection & Impact Analysis Service

**Files:**
- Create: `src/main/java/com/rc/notification/application/detection/ChangeDetectionService.java`

- [ ] **Step 1: Write ChangeDetectionService**

Key logic:
- `detectSchemaChange(eventTypeCode, oldSchema, newSchema)`: diff two JSON Schemas, generate ChangeRecord(s) with confidence=HIGH
- `analyzeImpact(changeRecordId)`: scan all ACTIVE subscriptions for eventTypeCode, statically check if their JSONata templates reference the changed fieldPath
- Called by EventTypeAdminService when payloadSchema is updated

- [ ] **Step 2: Wire into EventTypeAdminService.updateEventType**

When payloadSchema changes, call `changeDetectionService.detectSchemaChange(...)`.

- [ ] **Step 3: Verify existing tests still pass**

Run: `cd notification-service && mvn test -q`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add schema-based change detection and subscription impact analysis"
```

---

## Phase 6: Cleanup & Verification

### Task 14: Final Integration Verification

- [ ] **Step 1: Run full test suite**

Run: `cd notification-service && mvn clean test`
Expected: BUILD SUCCESS, all tests pass (original 28 + new tests)

- [ ] **Step 2: Verify v1 API backward compatibility**

Run: `cd notification-service && mvn test -Dtest=IngestionIntegrationTest -q`
Expected: All 6 original ingestion tests pass unchanged

- [ ] **Step 3: Verify application starts**

Run: `cd notification-service && mvn spring-boot:run -Dspring-boot.run.profiles=test &`
Check: application starts without errors, Swagger UI accessible

- [ ] **Step 4: Review cleanup.sql deletion order**

Final `cleanup.sql` should delete in dependency order:
```sql
DELETE FROM change_record;
DELETE FROM field_fingerprint;
DELETE FROM subscription;
DELETE FROM event_type;
DELETE FROM publisher;
DELETE FROM notification_dlq_log;
DELETE FROM supplier_config;
```

- [ ] **Step 5: Commit any fixes from verification**

```bash
git add -A
git commit -m "fix: final integration verification and cleanup"
```
