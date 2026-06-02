CREATE TABLE `notification_dlq_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `biz_sign` VARCHAR(64) NOT NULL UNIQUE COMMENT '业务唯一标识/幂等键, 对应运行时 eventId',
    `trace_id` VARCHAR(64) NOT NULL COMMENT '全局链路追踪ID',
    `supplier_code` VARCHAR(50) NOT NULL COMMENT '供应商唯一标识',
    `unified_context` LONGTEXT NOT NULL COMMENT '极限崩溃时的完整运行时统一只读上下文 JSON 树',
    `error_msg` TEXT COMMENT '最终抛出的崩溃堆栈或网络异常错误描述',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '进入死信前已发生的重试次数',
    `dlq_status` TINYINT NOT NULL DEFAULT 0 COMMENT '死信处理状态: 0-待处理, 1-已人工重试成功, 2-已主动忽略',
    `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '最近一次人工处理操作人(工号或账号)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '进入死信库时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '状态更新时间',
    INDEX `idx_supplier_status` (`supplier_code`, `dlq_status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中台极限容灾死信保险库表';
