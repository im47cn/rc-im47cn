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
    INDEX `idx_cr_event_type_code` (`event_type_code`),
    INDEX `idx_cr_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件变更记录表';
