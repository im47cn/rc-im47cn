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

CREATE TABLE IF NOT EXISTS field_fingerprint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type_code VARCHAR(128) NOT NULL,
    field_path VARCHAR(256) NOT NULL,
    observed_type VARCHAR(16) NOT NULL,
    first_seen_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    sample_count INT NOT NULL DEFAULT 1,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uk_event_field UNIQUE (event_type_code, field_path)
);

CREATE TABLE IF NOT EXISTS change_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type_code VARCHAR(128) NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    field_path VARCHAR(256) DEFAULT NULL,
    old_value VARCHAR(512) DEFAULT NULL,
    new_value VARCHAR(512) DEFAULT NULL,
    detection_source VARCHAR(16) NOT NULL,
    confidence VARCHAR(8) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING_REVIEW',
    affected_subscriptions TEXT DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
