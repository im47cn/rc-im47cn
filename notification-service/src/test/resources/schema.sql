-- H2 compatible schema for integration tests

CREATE TABLE IF NOT EXISTS supplier_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_code VARCHAR(50) NOT NULL UNIQUE,
    supplier_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    base_url VARCHAR(128) NOT NULL,
    http_method VARCHAR(10) NOT NULL DEFAULT 'POST',
    content_type_behavior VARCHAR(32) NOT NULL DEFAULT 'APPLICATION_JSON',
    credentials_encrypted TEXT DEFAULT NULL,
    path_template VARCHAR(255) DEFAULT NULL,
    query_template TEXT DEFAULT NULL,
    header_template TEXT DEFAULT NULL,
    body_template TEXT NOT NULL,
    connect_timeout_ms INT NOT NULL DEFAULT 3000,
    read_timeout_ms INT NOT NULL DEFAULT 5000,
    success_http_codes VARCHAR(50) DEFAULT '200',
    success_body_pattern VARCHAR(255) DEFAULT NULL,
    success_body_match_mode VARCHAR(20) DEFAULT 'EQUALS',
    success_case_sensitive TINYINT DEFAULT 1,
    max_retry_count INT NOT NULL DEFAULT 3,
    retry_backoff_initial_ms INT NOT NULL DEFAULT 1000,
    retry_backoff_multiplier DECIMAL(3,2) NOT NULL DEFAULT 2.00,
    retry_backoff_max_ms INT NOT NULL DEFAULT 30000,
    worker_concurrency INT NOT NULL DEFAULT 1,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_dlq_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_sign VARCHAR(64) NOT NULL UNIQUE,
    trace_id VARCHAR(64) NOT NULL,
    supplier_code VARCHAR(50) NOT NULL,
    unified_context LONGTEXT NOT NULL,
    error_msg TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    dlq_status TINYINT NOT NULL DEFAULT 0,
    updated_by VARCHAR(64) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_supplier_status ON notification_dlq_log (supplier_code, dlq_status);
CREATE INDEX IF NOT EXISTS idx_create_time ON notification_dlq_log (create_time);
