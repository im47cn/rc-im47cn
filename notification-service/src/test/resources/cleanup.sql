-- Clean up test data before each test method
DELETE FROM change_record;
DELETE FROM field_fingerprint;
DELETE FROM subscription;
DELETE FROM event_type;
DELETE FROM publisher;
DELETE FROM notification_dlq_log;
DELETE FROM supplier_config;
