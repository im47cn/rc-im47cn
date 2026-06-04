# Notification Service 运维手册

## 1. DLQ 批量重试流程

当供应商恢复服务后，需要将死信队列中的待处理消息重新投递。

### 场景：供应商故障恢复后批量重试

**前提条件**：确认供应商服务已恢复正常。

**操作步骤**：

1. **确认供应商恢复**
   - 通过管理后台的仿真功能（`/admin/suppliers/:id/simulate`）发送测试请求，验证供应商接口可用
   - 检查 Prometheus 指标 `notification_delivery_total{supplier_code="<code>", outcome="success"}` 是否有新增

2. **查看死信积压**
   - 登录管理后台 `/admin/dlq`
   - 筛选条件：`supplier_code` = 目标供应商，`dlq_status` = 待处理(0)
   - 记录待处理总数

3. **执行批量重试**
   - 在 DLQ 列表页勾选需要重试的记录（或全选当前页）
   - 点击"批量重试"按钮
   - 输入操作人标识（用于审计追踪）
   - 确认执行

4. **监控重试结果**
   - 观察 Prometheus 指标：
     - `notification_delivery_total{supplier_code="<code>", outcome="success"}` 应持续增长
     - `notification_dlq_pending` 应持续下降
   - 检查审计日志 `logs/audit.log` 中的投递结果
   - 若部分重试仍然失败，会再次进入 DLQ，需排查具体错误

5. **清理确认**
   - 对确认无需重试的死信，执行"忽略"操作（`dlq_status` 置为 2）
   - 记录操作人以备审计

### API 参考

```bash
# 查询待处理死信
curl -b session_cookie "http://<host>:8080/api/v1/admin/dlq?supplierCode=ALI_YUN&dlqStatus=0&page=1&size=100"

# 批量重试
curl -X POST -b session_cookie \
  -H "Content-Type: application/json" \
  -d '{"supplierCode": "ALI_YUN"}' \
  "http://<host>:8080/api/v1/admin/dlq/batch-retry"

# 单条重试
curl -X POST -b session_cookie \
  -H "X-Operator: ops_zhangsan" \
  "http://<host>:8080/api/v1/admin/dlq/123/retry"

# 标记忽略
curl -X POST -b session_cookie \
  -H "X-Operator: ops_zhangsan" \
  "http://<host>:8080/api/v1/admin/dlq/123/ignore"
```

---

## 2. 发布方注册与事件管理

系统支持 Publisher-Subscriber 发布订阅模型，发布方通过 V2 API 发送事件，系统根据订阅关系自动扇出投递。

### 2.1 注册发布方

**操作步骤**：

1. 登录管理后台，进入"发布方管理"
2. 点击"新增发布方"，填写：
   - **publisherCode**: 唯一标识，如 `order-service`
   - **publisherName**: 显示名称
   - **contactInfo**: 联系方式（可选）
3. 提交后系统自动生成 API Key
4. 将 API Key 提供给发布方，用于 `X-Publisher-Key` 请求头认证

```bash
# 创建发布方
curl -b cookies.txt -X POST http://<host>:8080/api/v1/admin/publishers \
  -H 'Content-Type: application/json' \
  -d '{"publisherCode": "order-service", "publisherName": "订单服务"}'

# 轮换 API Key（旧 Key 立即失效）
curl -b cookies.txt -X POST http://<host>:8080/api/v1/admin/publishers/<id>/rotate-key
```

### 2.2 定义事件类型

**操作步骤**：

1. 进入"事件类型管理"，点击"新增事件类型"
2. 填写：
   - **eventTypeCode**: 唯一标识，如 `ORDER_CREATED`
   - **publisherCode**: 归属发布方
   - **displayName**: 显示名称
   - **payloadSchema**: 可选 JSON Schema（用于入口校验和漂移检测）
3. 新建事件类型默认状态为 **DRAFT**，需手动激活为 **ACTIVE** 后才能接收事件

**状态生命周期**：`DRAFT` → `ACTIVE` → `DEPRECATED`

```bash
# 创建事件类型
curl -b cookies.txt -X POST http://<host>:8080/api/v1/admin/event-types \
  -H 'Content-Type: application/json' \
  -d '{
    "eventTypeCode": "ORDER_CREATED",
    "publisherCode": "order-service",
    "displayName": "订单创建事件",
    "payloadSchema": "{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"}}}"
  }'

# 激活事件类型（更新 status 为 ACTIVE）
curl -b cookies.txt -X PUT http://<host>:8080/api/v1/admin/event-types/<id> \
  -H 'Content-Type: application/json' \
  -d '{"status": "ACTIVE"}'
```

### 2.3 配置订阅关系

**操作步骤**：

1. 进入"订阅关系管理"，点击"新增订阅"
2. 填写：
   - **subscriberCode**: 订阅方编码（对应已有的供应商 supplierCode）
   - **eventTypeCode**: 订阅的事件类型
   - **managedBy**: 管理方（SUBSCRIBER / PUBLISHER / PLATFORM）
3. 可选：填写覆盖字段（优先级高于订阅方基础配置）
   - JSONata 模板覆盖（pathTemplate、bodyTemplate 等）
   - 超时与重试策略覆盖

**配置合并规则**：Subscription 覆盖字段 > SupplierConfig 基础字段（coalesce 语义）

```bash
# 创建订阅
curl -b cookies.txt -X POST http://<host>:8080/api/v1/admin/subscriptions \
  -H 'Content-Type: application/json' \
  -d '{
    "subscriberCode": "ALI_YUN",
    "eventTypeCode": "ORDER_CREATED",
    "bodyTemplate": "{\"phone\": payload.mobile, \"event\": eventType}"
  }'
```

### 2.4 V2 事件投递验证

发布方通过 V2 API 发送事件，系统自动根据订阅关系扇出投递。

```bash
# V2 事件投递（需要 X-Publisher-Key 认证）
curl -X POST http://<host>:8080/api/v2/notifications/ingest \
  -H 'Content-Type: application/json' \
  -H 'X-Publisher-Key: <publisher-api-key>' \
  -d '{
    "eventId": "order-001",
    "eventType": "ORDER_CREATED",
    "payload": {"orderId": "ORD-2026-001", "mobile": "13800138000"}
  }'

# 期望响应
# {"eventId":"order-001","status":"ACCEPTED","dispatches":[{"subscriberCode":"ALI_YUN","status":"QUEUED"}]}

# 定向投递（仅发送给指定订阅方）
curl -X POST http://<host>:8080/api/v2/notifications/ingest \
  -H 'Content-Type: application/json' \
  -H 'X-Publisher-Key: <publisher-api-key>' \
  -d '{
    "eventId": "order-002",
    "eventType": "ORDER_CREATED",
    "subscriberCode": "ALI_YUN",
    "payload": {"orderId": "ORD-2026-002", "mobile": "13900139000"}
  }'
```

---

## 3. Schema 漂移检测与处理

系统在 V2 事件投递时异步采样 Payload 字段，检测 Schema 漂移。

### 采样策略

- 前 100 条事件：100% 采样
- 之后：1% 随机采样
- 采样不阻塞主投递链路（异步执行）

### 检测类型

| 变更类型 | 说明 | 检测来源 |
|---------|------|---------|
| `FIELD_ADDED` | 运行时出现新字段 | 运行时采样 / Schema Diff |
| `FIELD_REMOVED` | Schema 中删除字段 | Schema Diff |
| `FIELD_TYPE_CHANGED` | 字段类型变化 | 运行时采样 / Schema Diff |

### 处理流程

1. 系统自动生成 `ChangeRecord`（状态为 `PENDING_REVIEW`）
2. 变更记录包含 `affectedSubscriptions` 字段，列出引用了变更字段的订阅方
3. 管理员在"事件类型详情"页面查看变更记录和字段指纹
4. 评估影响后，通知相关订阅方调整模板

### API 参考

```bash
# 查询字段指纹（某事件类型的所有已知字段）
curl -b cookies.txt "http://<host>:8080/api/v1/admin/field-fingerprints?eventTypeCode=ORDER_CREATED"

# 查询变更记录
curl -b cookies.txt "http://<host>:8080/api/v1/admin/change-records?eventTypeCode=ORDER_CREATED&status=PENDING_REVIEW"
```

---

## 4. 新订阅方接入清单

新订阅方上线的完整步骤，涵盖 订阅方注册 → 订阅关系配置 → 端到端验证 全流程。

### Step 1: 收集订阅方信息

- [ ] 订阅方接口文档（URL、HTTP Method、请求/响应格式）
- [ ] 鉴权方式（API Key、Bearer Token、签名等）
- [ ] 超时建议值（connect_timeout_ms、read_timeout_ms）
- [ ] 成功判定规则（HTTP 状态码、响应体匹配模式）
- [ ] 预期流量和并发需求（决定 worker_concurrency）
- [ ] 需要订阅的事件类型列表

### Step 2: 注册订阅方（供应商配置）

1. 登录管理后台，进入"订阅方管理"→"新增订阅方"
2. 填写基础信息：
   - **supplier_code**: 唯一标识，如 `ALI_YUN`（建议大写下划线格式）
   - **supplier_name**: 业务名称
   - **base_url**: 目标域名，如 `https://api.sms.com`
   - **http_method**: POST / PUT / PATCH / GET
   - **content_type_behavior**: APPLICATION_JSON 或 APPLICATION_FORM_URLENCODED
3. 配置超时与重试（作为该订阅方的默认策略）：
   - **connect_timeout_ms**: 建议 3000（3 秒）
   - **read_timeout_ms**: 建议 5000（5 秒）
   - **max_retry_count**: 建议 3
   - **retry_backoff_initial_ms**: 建议 1000
   - **retry_backoff_multiplier**: 建议 2.00
   - **retry_backoff_max_ms**: 建议 30000
4. 配置成功判定规则：
   - **success_http_codes**: `200` 或 `200,201`
   - **success_body_pattern**: 响应体匹配表达式（可选）
   - **success_body_match_mode**: EQUALS 或 CONTAINS
5. 填写凭证（自动 AES-256-GCM 加密存储）
6. 配置 worker_concurrency（默认 1，高流量供应商可设为 5+）
7. 编写默认 JSONata 模板（Path / Query / Header / Body），通过仿真面板验证
8. 提交后系统自动拉起 Worker 线程

### Step 3: 配置订阅关系

为订阅方绑定需要接收的事件类型（前提：对应事件类型已由发布方创建且状态为 ACTIVE）。

1. 进入"订阅关系管理"→"新增订阅"
2. 选择 **subscriberCode**（Step 2 中创建的供应商编码）和 **eventTypeCode**
3. 可选：填写覆盖配置（优先级高于 Step 2 中的默认策略）
   - JSONata 模板覆盖（如该事件需要特殊的 bodyTemplate）
   - 超时与重试策略覆盖
4. 设置 **managedBy**（SUBSCRIBER / PUBLISHER / PLATFORM）

**配置合并规则**：Subscription 覆盖字段 > SupplierConfig 默认字段（coalesce 语义，仅覆盖非空字段）。

### Step 4: 端到端验证

```bash
# 使用发布方 API Key 发送测试事件
curl -X POST http://<host>:8080/api/v2/notifications/ingest \
  -H 'Content-Type: application/json' \
  -H 'X-Publisher-Key: <publisher-api-key>' \
  -d '{
    "eventId": "test-001",
    "eventType": "<已订阅的事件类型>",
    "payload": { "mobile": "13800138000", "content": "接入验证" }
  }'
```

验证要点：
1. 响应中 `dispatches` 数组包含新订阅方，且状态为 `QUEUED`
2. 后端日志出现该订阅方的投递成功记录
3. 检查审计日志 `logs/audit.log` 确认 `DELIVER_SUCCESS`

### 注意事项

- Worker 线程总数受 `notification.worker.max-worker-threads`（默认 200）硬上限约束
- 若线程配额不足，会抛出 `WorkerCapacityExhaustedException`，需禁用低优先级供应商释放配额
- 供应商 `supplier_code` 创建后不可修改
- 订阅关系的唯一键为 `(subscriber_code, event_type_code)`，不可重复创建

---

## 5. Redis 故障处理

### 5.1 Redis Sentinel Failover（主节点切换）

**现象**：Redis 主节点故障，Sentinel 自动选举新主节点。

**系统行为**：

- **Failover 期间（通常几秒到十几秒）**：
  - 入队接口：捕获 `RedisConnectionException`，返回 `503 Service Unavailable` + `Retry-After: 5` 响应头
  - 消费链路：Worker 的阻塞 `poll` 操作暂停，Redisson 内部自动重连
  - 配置缓存：Caffeine 本地缓存继续提供只读服务（10 分钟有效期内）
  - Readiness Probe：返回 `OUT_OF_SERVICE`，K8s 自动摘流

- **Failover 完成后**：
  - Redisson 客户端自动重连新主节点
  - Worker 自动恢复消费，队列中的消息不会丢失
  - Readiness Probe 恢复 `UP`，K8s 重新注入流量
  - 60 秒看门狗扫描兜底刷新配置缓存

**操作步骤**：

1. 监控 Sentinel 日志确认 Failover 完成
2. 检查 `/actuator/health/readiness` 恢复 `UP`
3. 观察 `notification_delivery_total` 指标恢复正常增长
4. 无需人工干预，系统自动恢复

### 5.2 Redis 集群整体不可达

**现象**：所有 Redis 节点不可达（网络分区、机房断电等极端场景）。

**系统行为**：

- 入队完全停止（返回 503）
- 消费完全停止（Worker 阻塞在重连循环）
- 配置缓存最多可用 10 分钟（Caffeine TTL）
- 所有实例 Readiness 返回 `OUT_OF_SERVICE`

**操作步骤**：

1. **立即**：确认 Redis 集群状态，联系基础设施团队恢复
2. **等待恢复**：Redis 恢复后系统自动重连
3. **恢复后验证**：
   - 检查所有实例 Readiness 恢复 `UP`
   - 确认 Worker 线程恢复消费
   - 检查是否有消息在故障期间丢失（上游应有重试机制）
4. **注意**：Redis 不可用期间上游收到 503 响应的请求，需依赖上游自身的重试机制保证最终一致性

---

## 6. 扩缩容指南

### 6.1 Worker 线程调整（垂直扩展）

**场景**：单个供应商流量增长，需要增加消费并发。

**操作**：

1. 登录管理后台，编辑目标供应商配置
2. 调整 `worker_concurrency` 值（如从 1 调至 5）
3. 提交保存（自动触发 Worker 热重载）
4. 系统自动拉起新的 Worker 线程共享同一个 `RDelayedQueue`

**注意**：
- 所有供应商 `worker_concurrency` 之和不得超过 `notification.worker.max-worker-threads`（默认 200）
- 超过硬上限时抛出 `WorkerCapacityExhaustedException`
- 查看当前 Worker 使用情况：`notification_worker_active` 指标

### 6.2 调整 Worker 线程池上限

**操作**：

修改 `notification.worker.max-worker-threads` 配置值后重启服务。

```yaml
notification:
  worker:
    max-worker-threads: 500  # 从 200 调整至 500
```

或通过环境变量：

```bash
NOTIFICATION_WORKER_MAX_WORKER_THREADS=500
```

**注意**：调整前评估 JVM 内存是否充足，每个 Worker 线程约占用 1MB 栈空间。

### 6.3 水平扩展（增加实例）

**场景**：单实例 Worker 线程数已达上限，或需要提升入队接口吞吐。

**操作**：

1. K8s 中调整 `replicas` 数量
2. 所有实例共享同一套 Redis 队列和 MySQL 配置
3. 多实例 Worker 通过 Redisson 队列的原子 `poll` 操作天然互斥，无需额外协调

**注意事项**：

- 水平扩展后，每个实例都会根据自身的 `max-worker-threads` 上限拉起 Worker
- 多实例共享队列时，消息自动负载均衡（竞争消费模式）
- Redis Pub/Sub 缓存驱逐事件会广播到所有实例，配置变更自动同步
- 建议通过 K8s HPA 基于 CPU 或自定义指标（如 `notification_queue_depth`）自动扩缩

### 6.4 缩容操作

1. K8s 减少 `replicas` 或标记 Pod 为不可调度
2. Spring Boot 的 SmartLifecycle 触发优雅停机：
   - 设置 `shutdownRequested` 标志，Worker 停止拉取新任务
   - 等待在途请求完成（默认 30 秒窗口）
   - 超时后强制中断，被中断的 Worker 将当前任务重新压回队列
   - 未消费的消息保留在 Redis 队列中，由剩余实例继续消费
3. 验证消息无丢失：监控 `notification_queue_depth` 和投递指标

---

## 7. 日志与审计

### 审计日志

- **位置**: `logs/audit.log`
- **格式**: 单行 JSON，每日滚动（保留 30 天）
- **内容**: 投递成功（DELIVER_SUCCESS）、失败重试（DELIVER_FAILED）、死信降级（DELIVER_DLQ）
- **脱敏**: auth 凭证字段完全剥离，URL 中敏感查询参数值以 `***` 掩码

### 应用日志

- **位置**: 标准输出（Console）
- **格式**: `时间 [线程] 级别 类名 - 消息`

### 关键告警日志关键词

| 关键词 | 级别 | 含义 |
|--------|------|------|
| `WorkerCapacityExhaustedException` | ERROR | Worker 线程池容量耗尽 |
| `DELIVER_DLQ` | ERROR | 消息进入死信队列 |
| `RedisConnectionException` | WARN | Redis 连接异常 |
| `TranslationEngineException` | ERROR | JSONata 转换失败（配置错误） |

---

## 8. 常见问题排查

### 上游收到 503 响应

**原因**: Redis 不可用导致入队链路降级。

**处理**: 检查 Redis 连接状态，参考"5. Redis 故障处理"章节。

### Worker 线程无法拉起

**原因**: `max-worker-threads` 配额耗尽。

**处理**:
1. 检查 `notification_worker_active` 指标当前值
2. 禁用低优先级供应商释放线程配额
3. 或调整 `notification.worker.max-worker-threads` 后重启

### 供应商配置变更不生效

**原因**: 缓存未及时失效。

**处理**:
1. 检查 Redis Pub/Sub 连接是否正常
2. 等待 60 秒看门狗扫描兜底刷新
3. 极端情况下等待 Caffeine 10 分钟 TTL 自然过期

### 审计日志无输出

**原因**: Logback 配置问题或磁盘空间不足。

**处理**:
1. 检查 `logback-spring.xml` 中 AUDIT logger 配置
2. 确认 `logs/` 目录存在且有写入权限
3. 检查磁盘空间

### V2 Ingest 返回 401 REJECTED

**原因**: `X-Publisher-Key` 缺失、无效或发布方已禁用。

**处理**:
1. 确认请求携带 `X-Publisher-Key` 请求头
2. 检查发布方状态是否为启用（status=1）
3. 如 Key 泄露，通过管理后台执行 Key 轮换（`POST /api/v1/admin/publishers/<id>/rotate-key`）

### V2 Ingest 返回 eventType 相关错误

**原因**: 事件类型不存在、未激活、或不属于当前发布方。

**处理**:
1. 确认事件类型已创建且状态为 `ACTIVE`（非 DRAFT 或 DEPRECATED）
2. 确认事件类型的 `publisherCode` 与当前发布方匹配
3. 在管理后台"事件类型管理"页面核实状态

### 订阅方未收到事件

**原因**: 订阅关系缺失或状态异常。

**处理**:
1. 检查是否存在 `subscriberCode` + `eventTypeCode` 的活跃订阅（status=ACTIVE）
2. 检查 V2 Ingest 响应中的 `dispatches` 数组是否包含该订阅方
3. 如果 `dispatches` 为空，说明无匹配的活跃订阅
