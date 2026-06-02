# Implementation Tasks

## Task Dependency Graph

```
T1 (项目骨架) → T2 (数据模型) → T3 (配置缓存) → T4 (JSONata引擎)
                                                       ↓
T5 (凭证保险柜) ──────────────────────────────→ T6 (事件接收层)
                                                       ↓
T7 (Worker管理器) → T8 (HTTP请求构建) → T9 (投递执行与重试)
                                              ↓
T10 (审计日志) → T11 (死信队列) → T12 (DLQ管理API)
                                       ↓
T13 (健康检查与Metrics)                T14 (供应商配置API) ← T3, T5
                                       T15 (仿真API) ← T4, T8
T1 → T16 (鉴权过滤器) → T17 (前端骨架) → T18 (登录页)
                                              ↓
T20 (Monaco组件) → T21 (仿真面板) ──→ T22 (供应商表单页)
                                              ↑
T18 → T19 (供应商列表页) ─────────────────────┘
T18 → T23 (DLQ列表页)
                    ↓
T15, T16 → T24 (后端集成测试)
T22, T23 → T25 (前端构建集成) → T26 (部署文档)
```

---

## Phase 1: 基础骨架与数据层

- [x] 1. 项目骨架初始化
  - Files: `pom.xml`, `application.yml`, DDD 四层包目录结构
  - 创建 Spring Boot 3.2.x 项目结构，配置 Maven 依赖（Redisson、MyBatis-Plus、Caffeine、OkHttp3、jsonata-java），建立 DDD 四层包结构（interfaces/application/domain/infrastructure）
  - _Leverage: .spec-workflow/steering/structure.md, .spec-workflow/steering/tech.md_
  - _Requirements: REQ-1, REQ-3 (基础设施支撑)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Architect specializing in Spring Boot and DDD | Task: Create Spring Boot 3.2.x project skeleton following structure.md DDD four-layer architecture (interfaces/application/domain/infrastructure), configure Maven dependencies per tech.md (Redisson Starter, MyBatis-Plus, Caffeine, OkHttp3, jsonata-java, Spring Boot Actuator), set up application.yml with profiles (dev/prod) | Restrictions: Do not add business logic, only scaffolding. Match package naming conventions in structure.md exactly. Do not introduce dependencies not listed in tech.md | Success: `mvn clean compile` passes, Spring Boot starts successfully, `/actuator/health` returns UP, all DDD layer packages exist. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 2h

- [x] 2. 数据模型与持久层
  - Files: `infrastructure/persistence/entity/SupplierConfigEntity.java`, `infrastructure/persistence/entity/NotificationDlqLogEntity.java`, `infrastructure/persistence/mapper/SupplierConfigMapper.java`, `infrastructure/persistence/mapper/NotificationDlqLogMapper.java`, SQL migration scripts
  - 创建 `supplier_config` 和 `notification_dlq_log` 两张表的 DDL，编写 MyBatis-Plus Entity 与 Mapper。字段必须与 design.md Data Models 章节完全对齐（含 `worker_concurrency`、`credentials_encrypted`、`updated_by` 等）
  - Dependencies: T1
  - _Leverage: design.md Data Models 章节的 SQL DDL_
  - _Requirements: REQ-3, REQ-5, REQ-7_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Developer specializing in MyBatis-Plus and database design | Task: Create DDL scripts for `supplier_config` and `notification_dlq_log` tables exactly matching design.md Data Models section, implement MyBatis-Plus Entity classes with proper annotations and Mapper interfaces. Include `worker_concurrency` INT field in supplier_config | Restrictions: Field names, types, defaults, and indexes must match design.md DDL exactly. Do not add fields not in the design. Use MyBatis-Plus annotations (not XML mappers) | Success: DDL scripts execute cleanly on MySQL 8.0, Entity fields match DDL 1:1, unit tests verify basic CRUD operations on both tables. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

---

## Phase 2: 核心引擎层

- [x] 3. 配置缓存与热加载机制
  - Files: `domain/config/SupplierConfigDomainService.java`, `infrastructure/cache/SupplierConfigCacheManager.java`, `infrastructure/cache/ConfigEvictionListener.java`, `application/event/SupplierConfigActivatedEvent.java`
  - 实现三层缓存失效模型（Redis Pub/Sub 广播驱逐 + 60s 看门狗扫描 update_time + Caffeine 10min expireAfterWrite），配置变更触发 `SupplierConfigActivatedEvent` 领域事件
  - Dependencies: T2
  - _Leverage: design.md Cache Governance & Hot Loading Lifecycle 章节_
  - _Requirements: REQ-2 (AC4: 60秒内感知配置变更), REQ-3 (AC4: 新供应商自动创建队列)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Developer specializing in caching and event-driven architecture | Task: Implement three-tier cache invalidation model: (1) Redis Pub/Sub listener on topic `Notification:Config:Evict` that calls `cache.invalidate(supplierCode)`, (2) Scheduled task every 60s scanning MySQL `update_time` as watchdog fallback, (3) Caffeine cache with `expireAfterWrite(10, MINUTES)`. On cache miss reload, publish `SupplierConfigActivatedEvent` via Spring ApplicationEvent | Restrictions: Caffeine must be the single source of truth for in-process reads. Do not bypass cache for normal reads. Redis Pub/Sub topic name must be `Notification:Config:Evict` exactly | Success: Unit tests verify: Pub/Sub eviction triggers cache miss and reload; watchdog detects update_time change; Caffeine expiry triggers reload; SupplierConfigActivatedEvent fires on new config detection. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 4h

- [x] 4. JSONata 沙箱转换引擎
  - Files: `domain/translation/JsonataTranslationEngine.java`, `domain/translation/TranslationEngineException.java`
  - 封装 `jsonata-java` 库，实现 `transform()` 和 `transformWithBindings()` 接口，确保沙箱安全（禁止 RCE）
  - Dependencies: T1
  - _Leverage: design.md Component 3: JsonataTranslationEngine_
  - _Requirements: REQ-2 (AC1: JSONata转换, AC2: 沙箱安全, AC3: 加密预处理)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Security Engineer specializing in expression engine sandboxing | Task: Wrap `jsonata-java` library as `JsonataTranslationEngine` with `transform(String jsonataExpression, Object inputContext)` and `transformWithBindings(String jsonataExpression, Object inputContext, Map bindings)`. Ensure sandbox safety: no filesystem/network/reflection access from expressions. Catch evaluation errors and wrap in `TranslationEngineException` preserving character offset | Restrictions: Do not expose raw jsonata-java APIs. All exceptions must be wrapped in TranslationEngineException with error offset. Do not allow expression execution timeout > 5s | Success: Unit tests cover: normal 4-way expression transformation (Path/Query/Header/Body), invalid syntax throws TranslationEngineException with offset, RCE attempts blocked. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

- [x] 5. 凭证加解密保险柜
  - Files: `domain/credential/CredentialVault.java`
  - 实现 AES-256-GCM 加解密，Master Key 通过环境变量 `CREDENTIAL_MASTER_KEY` 注入
  - Dependencies: T1
  - _Leverage: design.md Component 5: CredentialVault_
  - _Requirements: REQ-7 (AC1: AES-256-GCM加密, AC2: 运行时解密, AC3: 脱敏)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Security Engineer specializing in cryptography | Task: Implement `CredentialVault` with `decrypt(String encryptedCredentials)` returning `Map<String, Object>` and `encrypt(Map<String, Object> plainCredentials)` returning encrypted string. Use AES-256-GCM with random IV per encryption. Master Key injected via `@Value("${credential.master-key}")` from env var `CREDENTIAL_MASTER_KEY` | Restrictions: Master Key must never be hardcoded or logged. GCM tag must be verified on decryption. Throw clear error on missing CREDENTIAL_MASTER_KEY at startup. Decrypted plaintext must only exist in memory, never persisted | Success: Unit tests verify: encrypt-decrypt roundtrip consistency, GCM tamper detection fails decryption, missing Master Key throws on bean initialization. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 2h

---

## Phase 3: 入队与调度层

- [x] 6. 事件接收控制器
  - Files: `interfaces/api/EventIngestionController.java`, `application/service/IngestionService.java`, `interfaces/api/dto/NotificationEventDto.java`, `interfaces/api/dto/IngestResponse.java`
  - 实现事件摄取端点：参数校验、Redis 分布式锁（5s TTL）、幂等状态判重（24h TTL）、压入 Redisson 队列、Redis 不可用时 503 降级
  - Dependencies: T2, T3, T5
  - _Leverage: design.md Component 1: EventIngestionController, Model 3: Redis Idempotent State Model_
  - _Requirements: REQ-1 (全部AC), REQ-4 (入队路径)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Developer specializing in high-concurrency API design | Task: Implement `EventIngestionController` with POST `/api/v1/notifications/ingest` endpoint. Flow: (1) Validate NotificationEventDto (supplier_code, event_type, payload required), (2) Acquire Redis distributed lock `lock:dispatch:{biz_sign}` with 5s TTL, (3) Check idempotent state `status:dispatch:{biz_sign}` - reject if SUCCESS/PROCESSING/DEAD_LETTERED, (4) Set state to PROCESSING with 24h TTL, (5) Push to Redisson queue by supplier_code, (6) Release lock in finally block, (7) Return 200 with event ID. Catch RedisConnectionException and return 503 with Retry-After:5 header | Restrictions: Lock must be released in finally block. DEAD_LETTERED state must return specific error message directing to admin console. Do not perform any JSONata transformation at ingestion time | Success: Tests cover: normal ingestion returns 200, duplicate biz_sign returns 202 idempotent hit, DEAD_LETTERED state rejected with guidance message, Redis unavailable returns 503 with Retry-After header. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 4h

- [x] 7. Worker 管理器与优雅停机
  - Files: `application/worker/SupplierWorkerManager.java`, `application/worker/WorkerCapacityExhaustedException.java`
  - 实现 SmartLifecycle，支持按 worker_concurrency 拉起多 Worker、线程池硬上限治理、配置变更动态扩缩、4 阶段优雅停机
  - Dependencies: T3
  - _Leverage: design.md Component 2: SupplierWorkerManager, Graceful Shutdown Lifecycle_
  - _Requirements: REQ-3 (AC4: 自动创建Worker), REQ-4 (调度基础)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Concurrency Engineer specializing in thread lifecycle management | Task: Implement `SupplierWorkerManager` with SmartLifecycle. On startup: load all active supplier configs, for each create N worker threads (per worker_concurrency) sharing one RDelayedQueue. Listen for SupplierConfigActivatedEvent to dynamically launch new workers. Enforce max-worker-threads hard limit (default 200), throw WorkerCapacityExhaustedException when exceeded. Graceful shutdown: (1) set shutdownRequested flag, (2) await in-flight requests up to shutdown-await-seconds (default 30), (3) interrupt remaining workers who re-queue their current task, (4) cleanup registries. getPhase() returns Integer.MAX_VALUE - 1 | Restrictions: Workers must share RDelayedQueue using atomic poll for natural mutual exclusion. Do not use synchronized blocks for queue access. Thread registry must be ConcurrentHashMap | Success: Tests verify: startup launches correct worker count per config, new supplier event triggers dynamic worker creation, capacity exceeded throws exception, shutdown completes within timeout with zero message loss. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 5h

---

## Phase 4: 投递执行层

- [x] 8. HTTP 请求构建器
  - Files: `infrastructure/http/FullStackHttpRequestBuilder.java`
  - 组装四路 JSONata 输出为 OkHttp Request，基于共享连接池通过 `client.newBuilder()` 衍生专属超时 Client
  - Dependencies: T4
  - _Leverage: design.md Component 4: FullStackHttpRequestBuilder_
  - _Requirements: REQ-2 (AC1: 报文转换), REQ-3 (AC2: HTTP请求组装)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java HTTP Client Engineer | Task: Implement `FullStackHttpRequestBuilder` with `buildRequest(SupplierConfig config, Object inputContext)` and `deriveClient(SupplierConfig config)`. Use JsonataTranslationEngine to evaluate 4-way templates (path_template, query_template, header_template, body_template) against UnifiedInputContext. Assemble OkHttp Request with resolved URL, headers, and body. deriveClient uses global OkHttpClient.newBuilder() to set per-supplier connect_timeout_ms and read_timeout_ms | Restrictions: Global OkHttpClient must be shared (connection pool reuse). Null/empty templates should use sensible defaults (empty path=base_url as-is, empty query=no params, empty header=Content-Type only). Do not execute the request, only build it | Success: Unit tests verify: 4-way parameters correctly assembled, per-supplier timeouts applied, empty templates handled gracefully, APPLICATION_JSON and APPLICATION_FORM_URLENCODED content types supported. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

- [x] 9. 投递执行与指数退避重试
  - Files: `application/worker/DeliveryWorker.java`, `application/worker/UnifiedInputContextBuilder.java`
  - Worker 核心投递循环：构建 UnifiedInputContext → JSONata 转换 → HTTP 执行 → 成功判定 → 失败时指数退避重新入队
  - Dependencies: T6, T7, T8
  - _Leverage: design.md Error Handling scenarios 1-4, Model 3: Redis State Lifecycle_
  - _Requirements: REQ-3 (AC3: 成功判定), REQ-4 (全部AC), REQ-5 (AC1: DLQ落盘)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Developer specializing in async message processing and retry patterns | Task: Implement `DeliveryWorker` Runnable that loops: (1) blocking poll from RDelayedQueue, (2) check shutdownRequested flag, (3) build UnifiedInputContext (event payload + CredentialVault.decrypt auth + traceId + timestamp), (4) call FullStackHttpRequestBuilder.buildRequest + deriveClient, (5) execute HTTP request, (6) evaluate success per supplier config (success_http_codes + success_body_pattern with match_mode and case_sensitive), (7) on success: update Redis state to SUCCESS with TTL shrink to 1h + audit log, (8) on transient failure: calculate delay = min(initial_ms * multiplier^retry_count, max_ms), re-queue with delay + audit log, (9) on max retry exceeded: call DeadLetterService + audit log, (10) on JSONata error: direct to DLQ as non-retryable. Handle InterruptedException by re-queuing current task | Restrictions: SUCCESS state must atomically update status AND shrink TTL. JSONata errors must never trigger retry. UnifiedInputContext must be immutable after construction | Success: Integration tests verify: successful delivery updates Redis state to SUCCESS with 1h TTL, failed delivery re-queues with correct exponential delay, max retry triggers DLQ, JSONata error goes directly to DLQ without retry, interrupted worker re-queues task. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 5h

---

## Phase 5: 可观测性与容灾

- [x] 10. 异步审计日志
  - Files: `infrastructure/audit/AuditLoggerAppender.java`, `infrastructure/audit/AuditLogRecord.java`, `logback-spring.xml`
  - 基于 Logback AsyncAppender RingBuffer 输出单行 JSON 审计日志，覆盖三种状态，含敏感数据脱敏
  - Dependencies: T9
  - _Leverage: design.md Component 6: AuditLoggerAppender, Model 4: AuditLogSchema_
  - _Requirements: REQ-7 (AC3: 审计日志脱敏)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Developer specializing in logging and observability | Task: Implement `AuditLoggerAppender` with `logAudit(AuditLogRecord record)` that writes single-line JSON to dedicated audit log file via Logback AsyncAppender. AuditLogRecord contains: timestamp, log_level, trace_id, biz_sign, supplier_code, http_method, actual_url, http_code, elapsed_time_ms, retry_count, audit_status (DELIVER_SUCCESS/DELIVER_FAILED/DELIVER_DLQ), error_summary, next_retry_delay_ms. Implement credential desensitization: strip auth fields entirely, mask URL query parameter values matching sensitive patterns (sign, key, token, secret) with `***` | Restrictions: Audit log must be single-line JSON, no internal newlines. AsyncAppender RingBuffer must not block the delivery worker thread. Desensitization must happen at serialization time, not in the caller | Success: Unit tests verify: three audit status formats match design.md examples exactly, auth credentials are completely absent from output, URL sensitive parameters are masked, async write does not block caller thread. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

- [x] 11. 死信队列机制
  - Files: `application/dlq/DeadLetterService.java`
  - 重试耗尽后 DLQ 落盘、Redis 状态原子改写为 DEAD_LETTERED（TTL 不变）、P1 告警日志
  - Dependencies: T9, T10
  - _Leverage: design.md Error Handling scenario 3: Fatal Channel Exhaustion_
  - _Requirements: REQ-5 (AC1: 完整记录持久化, AC2: 告警日志)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Developer specializing in fault tolerance and disaster recovery | Task: Implement `DeadLetterService` with method to handle retry-exhausted events: (1) INSERT into notification_dlq_log with full unified_context JSON, error_msg, retry_count, dlq_status=0, (2) Atomically update Redis `status:dispatch:{biz_sign}` to DEAD_LETTERED preserving original TTL, (3) Output ERROR level log with standardized format for external alerting system interception (P1 disaster alert), (4) Call AuditLoggerAppender with DELIVER_DLQ status | Restrictions: Redis state update must be atomic (use Lua script or pipeline). TTL must NOT be modified when transitioning to DEAD_LETTERED. unified_context must be stored as complete JSON string for full reproducibility | Success: Integration tests verify: DLQ record contains complete unified_context, Redis state is DEAD_LETTERED with unchanged TTL, ERROR log output contains standardized alert format, subsequent ingestion of same biz_sign is rejected with DLQ guidance message. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

- [x] 12. DLQ 管理 API
  - Files: `interfaces/admin/DlqManagementController.java`, `application/dlq/DlqManagementService.java`
  - 死信分页查询、单条/批量重试（重置 Redis 状态 + 重新入队）、标记忽略
  - Dependencies: T11
  - _Leverage: design.md Component 8: DlqManagementController_
  - _Requirements: REQ-5 (AC3: 单条/批量重试, AC4: 标记忽略)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Developer specializing in admin API design | Task: Implement `DlqManagementController` with 4 endpoints per design.md Component 8: (1) GET /api/v1/admin/dlq - paginated query with supplier_code and dlq_status filters, (2) POST /api/v1/admin/dlq/{id}/retry - single retry: reset Redis state to PROCESSING + re-queue to Redisson + update updated_by, (3) POST /api/v1/admin/dlq/batch-retry - batch retry by supplier or ID list returning success/failure counts, (4) POST /api/v1/admin/dlq/{id}/ignore - set dlq_status=2 + record operator from X-Operator header | Restrictions: Single retry must atomically reset Redis state before re-queuing. Batch retry must return aggregate results even if some fail. X-Operator header is required for audit trail | Success: API tests verify: paginated query filters work, single retry resets Redis state to PROCESSING and message appears in queue, batch retry returns correct success/failure count, ignore updates status and records operator. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

---

## Phase 6: 运维保障

- [x] 13. 健康检查与 Metrics
  - Files: `infrastructure/health/RedisHealthIndicator.java`, `infrastructure/health/WorkerHealthIndicator.java`, `infrastructure/metrics/NotificationMetricsRegistry.java`, `application.yml`
  - 配置 Actuator 健康检查（Liveness/Readiness），Micrometer 暴露 6 类核心业务 Metrics
  - Dependencies: T7, T12
  - _Leverage: design.md Component 7: HealthAndMetricsEndpoint_
  - _Requirements: REQ-1 (AC4: 降级联动), REQ-3 (系统可观测性)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java DevOps Engineer specializing in Spring Boot Actuator and Prometheus | Task: Configure K8s-compatible health probes: Liveness at /actuator/health/liveness (DOWN only on OOM/deadlock), Readiness at /actuator/health/readiness (composite: Redis connectivity + MySQL connectivity + active worker count > 0). Register 6 Micrometer metrics: notification.ingest.total (Counter, tags: supplier_code, result), notification.delivery.total (Counter, tags: supplier_code, outcome), notification.delivery.duration (Timer, tags: supplier_code), notification.queue.depth (Gauge, tags: supplier_code), notification.worker.active (Gauge), notification.dlq.pending (Gauge) | Restrictions: Readiness must return OUT_OF_SERVICE if any component fails. Metrics must use Micrometer tags, not custom label implementations. Do not expose sensitive data in health endpoints | Success: Readiness returns OUT_OF_SERVICE when Redis disconnected, /actuator/prometheus outputs all 6 metric families with correct tags, liveness remains UP during normal degradation scenarios. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

---

## Phase 7: 管理后台 -- 后端 API

- [x] 14. 供应商配置管理 API
  - Files: `interfaces/admin/SupplierConfigController.java`, `application/admin/SupplierConfigAdminService.java`
  - CRUD 全流程，新增/修改后广播 Pub/Sub 驱逐事件，凭证脱敏，启用/禁用联动 Worker
  - Dependencies: T3, T5
  - _Leverage: design.md Component 9: SupplierConfigController_
  - _Requirements: REQ-6 (AC2: CRUD表单), REQ-7 (AC1: 加密存储)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Developer specializing in admin CRUD API | Task: Implement `SupplierConfigController` with 5 endpoints per design.md Component 9: (1) GET /api/v1/admin/suppliers - paginated list with keyword/status filters, (2) GET /api/v1/admin/suppliers/{id} - single supplier with credentials_encrypted desensitized (show key names only, mask values), (3) POST /api/v1/admin/suppliers - create with supplier_code uniqueness check + CredentialVault.encrypt + Redis Pub/Sub CREATE broadcast, (4) PUT /api/v1/admin/suppliers/{id} - update + Pub/Sub UPDATE broadcast, (5) PATCH /api/v1/admin/suppliers/{id}/status - enable(1)/disable(0) toggling Worker lifecycle | Restrictions: credentials_encrypted must never be returned in plaintext. Pub/Sub broadcast must happen after successful DB write. Disable must trigger graceful Worker shutdown for that supplier | Success: API tests verify: CRUD full lifecycle, supplier_code uniqueness enforced, create triggers Pub/Sub and Worker launch, disable stops corresponding Workers, credential values are masked in GET responses. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 4h

- [x] 15. JSONata 在线仿真 API
  - Files: `interfaces/admin/SimulationController.java`, `application/admin/SimulationService.java`
  - 单表达式转换和完整请求预览两个端点，错误时返回 offset
  - Dependencies: T4, T8
  - _Leverage: design.md Component 10: SimulationController_
  - _Requirements: REQ-6 (AC3: 仿真功能)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Developer | Task: Implement `SimulationController` with 2 endpoints per design.md Component 10: (1) POST /api/v1/admin/simulation/transform - accept jsonataExpression + mockInputContext, return transformedResult or error with character offset, (2) POST /api/v1/admin/simulation/full-preview - accept full supplier config (4-way templates) + mockInputContext, return preview of complete HTTP request (resolved URL, Headers, Body) without sending network request | Restrictions: Must use JsonataTranslationEngine for all transformations. Error responses must include character offset from jsonata-java. full-preview must NOT execute any network calls | Success: API tests verify: valid expression returns correct result, invalid expression returns error with offset, full-preview returns complete HTTP request preview with resolved URL/Headers/Body. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

- [x] 16. 简易鉴权过滤器
  - Files: `interfaces/admin/AdminAuthFilter.java`, `interfaces/admin/AuthController.java`
  - 拦截 `/api/v1/admin/**`，Session 鉴权，硬编码 admin/admin 可通过外部化配置覆盖
  - Dependencies: T1
  - _Leverage: design.md Component 11: AdminAuthFilter_
  - _Requirements: REQ-6 (AC1: Session鉴权)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java Backend Developer specializing in authentication | Task: Implement `AdminAuthFilter` (servlet Filter) intercepting `/api/v1/admin/**` excluding `/api/v1/admin/login` and `/api/v1/admin/logout`. Return 401 JSON response for unauthenticated requests. Implement `AuthController` with POST /api/v1/admin/login (validate username/password from Spring config `admin.username`/`admin.password` defaulting to admin/admin, write HttpSession) and POST /api/v1/admin/logout (invalidate Session) | Restrictions: Do not use Spring Security (overkill for MVP). Credentials must be configurable via application.yml/environment variables. Do not store passwords in plaintext in config files for production | Success: Tests verify: unauthenticated /api/v1/admin/suppliers returns 401, login with correct credentials returns success and sets session, subsequent requests with session pass filter, logout clears session, /api/v1/notifications/ingest is NOT intercepted by filter. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 1.5h

---

## Phase 8: 管理后台 -- 前端 SPA

- [x] 17. 前端项目骨架
  - Files: `notification-admin-ui/` 全部初始化文件（package.json, vite.config.ts, tsconfig.json, router/index.ts, stores/auth.ts, utils/request.ts, App.vue, main.ts）
  - 初始化 Vue 3 + TypeScript + Vite 5，集成 Element Plus、Vue Router、Pinia、Axios，配置 Vite 开发代理和路由守卫
  - Dependencies: T16
  - _Leverage: design.md Admin Frontend 技术选型与前端目录结构_
  - _Requirements: REQ-6 (前端基础设施)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Developer specializing in Vue 3 and Vite | Task: Initialize Vue 3 + TypeScript + Vite 5 project in notification-admin-ui/ directory. Install and configure: Element Plus (full import for MVP), Vue Router 4 (history mode with routes per design.md page structure), Pinia (auth store), Axios (utils/request.ts with 401 interceptor redirecting to /login). Configure vite.config.ts dev proxy: /api -> http://localhost:8080. Add router guard: redirect to /login if not authenticated | Restrictions: Follow directory structure from design.md exactly. Use pnpm as package manager. Do not add unnecessary dependencies beyond what design.md specifies | Success: `pnpm dev` starts successfully, /login route renders placeholder, unauthenticated access to /suppliers redirects to /login, Vite proxy forwards /api requests to backend. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 2h

- [x] 18. 登录页
  - Files: `views/LoginView.vue`, `api/auth.ts`, `stores/auth.ts`
  - Element Plus 表单，调用登录 API，成功跳转 /suppliers
  - Dependencies: T17
  - _Leverage: design.md Admin Frontend 页面结构_
  - _Requirements: REQ-6 (AC1: 管理员登录)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Vue Frontend Developer | Task: Implement LoginView.vue with Element Plus form (username + password fields + submit button), call POST /api/v1/admin/login via api/auth.ts, on success update Pinia auth store and navigate to /suppliers, on failure show ElMessage error. Style: centered card layout | Restrictions: Do not store credentials in localStorage/Pinia (rely on HttpSession cookie). Form validation: both fields required. Use Element Plus components only | Success: Correct credentials login and redirect to /suppliers, incorrect credentials show error message, page refresh preserves session (cookie-based), logout clears auth state and redirects to /login. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 1.5h

- [x] 19. 供应商列表页
  - Files: `views/SupplierListView.vue`, `api/supplier.ts`
  - 搜索栏 + 数据表格 + 分页 + 操作列（编辑、启用/禁用、仿真）
  - Dependencies: T18
  - _Leverage: design.md Admin Frontend 页面结构_
  - _Requirements: REQ-6 (AC2: 供应商管理界面)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Vue Frontend Developer specializing in Element Plus data tables | Task: Implement SupplierListView.vue with: (1) Search bar: keyword input + status dropdown (all/enabled/disabled) + search button, (2) ElTable columns: supplier_code, supplier_name, base_url, http_method, status (ElTag green/red), worker_concurrency, actions, (3) ElPagination, (4) Action column: Edit button (navigate to /suppliers/:id/edit), Enable/Disable toggle (call PATCH status API + refresh), Simulate button (navigate to /suppliers/:id/simulate). Implement api/supplier.ts with all supplier API calls | Restrictions: Use Element Plus table/pagination components. Status toggle must confirm before executing. Table must show loading state during API calls | Success: List loads with pagination, keyword search filters results, status filter works, enable/disable toggle calls API and refreshes list, edit/simulate buttons navigate correctly. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

- [x] 20. Monaco Editor 组件封装
  - Files: `components/MonacoEditor.vue`
  - 支持 language、v-model、readOnly、error markers
  - Dependencies: T17
  - _Leverage: design.md Admin Frontend 核心交互设计_
  - _Requirements: REQ-6 (AC3: 模板编辑器)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Frontend Developer specializing in Monaco Editor integration | Task: Create MonacoEditor.vue component wrapping @monaco-editor/loader. Props: language (default 'json'), modelValue (v-model string), readOnly (boolean), height (default '300px'), markers (array of {startLineNumber, startColumn, endLineNumber, endColumn, message, severity}). Emit update:modelValue on content change. Apply markers as editor decorations/diagnostics when prop changes | Restrictions: Use @monaco-editor/loader (not full monaco-editor package) for smaller bundle. Dispose editor instance on component unmount to prevent memory leaks. Do not import full Monaco languages, only json | Success: Editor renders with syntax highlighting, v-model two-way binding works, external markers show as red squiggly underlines in editor, readonly mode prevents editing, component cleans up on unmount. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 2h

- [x] 21. JSONata 仿真面板组件
  - Files: `components/SimulationPanel.vue`, `api/simulation.ts`
  - 左侧 JSONata 编辑器 + 右侧 Mock Context 编辑器 + 底部结果/错误，500ms 防抖
  - Dependencies: T20
  - _Leverage: design.md Admin Frontend JSONata 实时仿真面板_
  - _Requirements: REQ-6 (AC3: 实时仿真)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Vue Frontend Developer | Task: Create SimulationPanel.vue with layout per design.md: top-left MonacoEditor (JSONata expression, language='plaintext'), top-right MonacoEditor (Mock Input Context, language='json'), bottom-left result display (readonly MonacoEditor), bottom-right full preview button. On either editor content change, debounce 500ms then call POST /api/v1/admin/simulation/transform. On success: show result. On error: parse offset from response and set markers on JSONata editor. "Full Preview" button calls /full-preview endpoint and displays complete HTTP request | Restrictions: Debounce must be 500ms exactly. Error markers must highlight the exact error position from backend offset. Do not call API if either editor is empty | Success: Type valid expression + context -> result appears within ~500ms, type invalid expression -> error marker appears on correct line/column, clear expression -> result clears, full preview button shows complete HTTP request. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

- [x] 22. 供应商配置表单页
  - Files: `views/SupplierFormView.vue`, `components/CredentialForm.vue`
  - 新增+编辑复用，三区布局：基础信息、凭证 KV、四 Tab 模板编辑器（各含仿真面板）
  - Dependencies: T19, T21
  - _Leverage: design.md Admin Frontend 供应商配置表单_
  - _Requirements: REQ-6 (AC2: 配置表单, AC4: 凭证脱敏)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Senior Vue Frontend Developer | Task: Implement SupplierFormView.vue (create + edit mode via route param) with 3 sections: (1) Basic Info: ElForm with supplier_code, supplier_name, description, base_url, http_method select, content_type_behavior select, connect/read timeout, retry config (max_count, initial_ms, multiplier, max_ms), success criteria (http_codes, body_pattern, match_mode, case_sensitive), worker_concurrency, (2) Credentials: CredentialForm.vue dynamic KV pairs - edit mode shows keys with values as '******', option to 'keep original' or 're-enter', (3) Templates: ElTabs with 4 tabs (Path/Query/Header/Body) each embedding SimulationPanel. Edit mode pre-fills all fields from GET /api/v1/admin/suppliers/{id}. Submit calls create/update API accordingly | Restrictions: credential values in edit mode must never be fetched as plaintext. 'Keep original' must not send credential field in update request. Form validation required for all mandatory fields | Success: Create flow: fill form + simulate + submit -> new supplier in list. Edit flow: pre-filled correctly + modify + submit -> config updated. Credentials: edit shows keys only, 'keep original' preserves encrypted data, 're-enter' sends new value for encryption. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 5h

- [x] 23. 死信队列列表页
  - Files: `views/DlqListView.vue`, `api/dlq.ts`
  - 表格 + 筛选 + 单条/批量重试 + 忽略
  - Dependencies: T18
  - _Leverage: design.md Admin Frontend 死信队列管理_
  - _Requirements: REQ-5 (AC3: 重试, AC4: 忽略)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Vue Frontend Developer | Task: Implement DlqListView.vue with: (1) Filter bar: supplier_code dropdown + dlq_status dropdown (pending/retried/ignored) + search button, (2) ElTable columns: biz_sign, trace_id, supplier_code, error_msg (ElTooltip for full text), retry_count, dlq_status (ElTag with color coding: pending=red, retried=green, ignored=gray), create_time, actions, (3) Action column: Retry button (prompt for operator name via ElDialog, call single retry API), Ignore button (prompt for operator, call ignore API), (4) Batch bar: ElCheckbox selection + batch retry button, (5) ElPagination | Restrictions: Retry/Ignore must prompt for operator identity (X-Operator header). Batch retry result must display success/failure counts in ElMessage. Refresh list after any mutation | Success: List loads with filters and pagination, single retry prompts operator and updates status, batch retry processes selected items and shows result counts, ignore updates status, error_msg tooltip shows full text on hover. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 3h

---

## Phase 9: 验证与交付

- [x] 24. 后端端到端集成测试
  - Files: `src/test/java/integration/`
  - 覆盖完整链路：入队 → Worker → 转换 → 投递 → 审计 → 重试 → DLQ，以及管理 API 全流程
  - Dependencies: T15, T16
  - _Leverage: design.md 全文, requirements.md 全部 AC_
  - _Requirements: REQ-1 ~ REQ-7 全部验收标准_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Java QA Engineer specializing in integration testing | Task: Write end-to-end integration tests covering: (1) Happy path: ingest event -> Worker consumes -> JSONata transforms -> HTTP delivery to MockWebServer -> audit log SUCCESS, (2) Retry path: 3 failures with exponential backoff -> DLQ with DEAD_LETTERED state, (3) Idempotency: duplicate biz_sign rejected, DEAD_LETTERED state rejected with guidance, (4) Redis degradation: Redis unavailable returns 503 with Retry-After, (5) Admin API: supplier CRUD -> cache refresh -> Worker hot reload, (6) Simulation API: transform + full-preview, (7) DLQ API: query + retry + ignore. Use embedded Redis (or Testcontainers) and H2/MySQL Testcontainer | Restrictions: Each test must be independent and idempotent. Use MockWebServer for supplier endpoint simulation. Do not test against real external services | Success: All 7 test scenarios pass, code coverage > 80% for core components, tests complete within 2 minutes. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 5h

- [x] 25. 前端构建与集成
  - Files: `vite.config.ts`, Spring Boot `WebMvcConfigurer`
  - Vite 生产构建输出至 `static/admin/`，Spring Boot SPA 路由转发
  - Dependencies: T22, T23
  - _Leverage: design.md 前端目录结构, structure.md_
  - _Requirements: REQ-6 (前端部署)_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: Full-stack Developer specializing in build tooling | Task: Configure Vite production build: output to `../notification-service/src/main/resources/static/admin/` with base '/admin/'. Add Spring Boot WebMvcConfigurer to forward `/admin/**` requests (excluding static assets) to `/admin/index.html` for SPA history mode support. Ensure /api/** routes are NOT affected by SPA forwarding | Restrictions: Build output must go to Spring Boot static resources, not a separate server. SPA forwarding must not interfere with API routes or actuator endpoints. Base path must be '/admin/' | Success: `pnpm build` outputs files to correct directory, Spring Boot serves /admin/ with frontend SPA, browser refresh on /admin/suppliers does not 404, /api and /actuator routes work normally. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 2h

- [x] 26. 部署文档与上线清单
  - Files: `docs/deployment.md`, `docs/runbook.md`
  - 环境变量清单、Redis Sentinel 配置、MySQL Schema 初始化、Prometheus 采集、前端构建说明、上线检查清单
  - Dependencies: T24, T25
  - _Leverage: design.md 全文, tech.md_
  - _Requirements: 全部非功能需求_
  - _Prompt: Implement the task for spec notification-service, first run spec-workflow-guide to get the workflow guide then implement the task: Role: DevOps Engineer specializing in deployment documentation | Task: Write deployment.md covering: (1) Environment variables (CREDENTIAL_MASTER_KEY, admin.username/password, Redis Sentinel config, MySQL connection, max-worker-threads), (2) MySQL schema initialization (DDL scripts location), (3) Redis Sentinel/Cluster requirements for production, (4) Prometheus scrape config for /actuator/prometheus, (5) Frontend build and integration steps, (6) K8s liveness/readiness probe configuration. Write runbook.md with operational procedures: DLQ batch retry after vendor recovery, supplier onboarding checklist, Redis failover handling, scaling guidelines | Restrictions: Do not include actual credentials or secrets in documentation. Clearly mark production-only vs dev requirements. Reference config property names exactly as used in application.yml | Success: New team member can deploy from scratch following deployment.md, operational scenarios in runbook.md cover all error handling scenarios from design.md. Mark task as in-progress in tasks.md before starting, log implementation with log-implementation tool after completion, then mark as complete._
  - Estimate: 2h

---

## Summary

| Phase | Tasks | 预估总工时 |
|-------|-------|-----------|
| Phase 1: 基础骨架与数据层 | T1, T2 | 5h |
| Phase 2: 核心引擎层 | T3, T4, T5 | 9h |
| Phase 3: 入队与调度层 | T6, T7 | 9h |
| Phase 4: 投递执行层 | T8, T9 | 8h |
| Phase 5: 可观测性与容灾 | T10, T11, T12 | 9h |
| Phase 6: 运维保障 | T13 | 3h |
| Phase 7: 管理后台后端 | T14, T15, T16 | 8.5h |
| Phase 8: 管理后台前端 | T17, T18, T19, T20, T21, T22, T23 | 19.5h |
| Phase 9: 验证与交付 | T24, T25, T26 | 9h |
| **Total** | **26 tasks** | **80h** |
