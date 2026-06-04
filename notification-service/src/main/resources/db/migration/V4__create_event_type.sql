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
