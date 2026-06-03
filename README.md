# Notification Service — API 通知中台

## 1. 问题理解

简述对需求的理解：企业内部业务系统需要调用外部异构供应商 API 进行通知，
本系统作为防腐层（ACL）统一屏蔽下游差异，确保通知可靠送达。

## 2. 系统边界

### 选择解决的问题
- 协议异构性（Path/Query/Header/Body 四路参数映射）
- 投递可靠性（At-Least-Once + 指数退避重试 + DLQ 兜底）
- 供应商级别资源隔离（独立队列 + 独立 Worker 线程）
- 零代码接入新供应商（JSONata 声明式模板 + 在线仿真）

### 明确不解决的问题（及理由）
- 下游幂等性：At-Least-Once 语义下可能重复投递，幂等由下游自行保证
- 供应商级精细限流：MVP 仅做全局线程上限控制，不做 per-supplier rate limiting
- 消息顺序保证：业务场景不要求严格有序，追求吞吐优先
- 响应结果回传：业务系统不关心外部 API 返回值，通知为 fire-and-forget 语义

## 3. 整体架构与核心设计

### 架构图

```text
上游业务 → [Ingest API] → [Redis 队列(per-supplier)] → [Worker 线程池] → [JSONata 转换] → [OkHttp 投递] → 下游供应商
                              ↓ 失败重试（指数退避）
                         [DLQ MySQL] ← 超过最大重试次数
```

### 核心组件
- **DDD 四层架构**：interfaces / application / domain / infrastructure
- **JSONata 沙箱引擎**：替代 SpEL，彻底消除 RCE 风险
- **CredentialVault**：AES-256-GCM 加密存储供应商凭证
- **SupplierWorkerManager**：动态创建/销毁 per-supplier Worker，SmartLifecycle 优雅停机

## 4. 可靠性与失败处理

### 投递语义：At-Least-Once
- Redis 分布式锁防并发击穿（5s TTL）
- 24 小时幂等状态窗口（成功后缩短至 1h）
- 重试均走 Redisson 延迟队列，支持供应商级定制退避策略

### 外部系统长期不可用
1. 指数退避重试（可配置：初始间隔、倍数、最大间隔、最大次数）
2. 超过最大重试 → 写入 DLQ（MySQL），状态降级为 DEAD_LETTERED
3. 触发 P1 级告警（Prometheus 指标 notification_dlq_pending）
4. 供应商恢复后，运维通过管理后台一键批量重试

### Redis 不可用降级
- 入队链路：返回 503 + Retry-After，由上游自身重试机制保障
- 消费链路：Worker 阻塞等待重连，消息不丢失
- 配置缓存：Caffeine 本地缓存兜底（10 分钟 TTL）

## 5. 关键工程决策与取舍

### 取舍说明

| 决策点 | AI 建议 | 我的决定 | 理由 |
|--------|---------|----------|------|
| 表达式引擎 | SpEL（路线 A）| JSONata | SpEL 的 SimpleEvaluationContext 过于严苛，StandardEvaluationContext 有 RCE 风险；JSONata 天然沙箱、零 JVM 逃逸 |
| 格式拼装职责 | 留给业务端（选项 B，AI 强烈推荐） | 中台承担（选项 A） | 通知中台就是防腐层，把格式复杂性留给业务端违背 ACL 本意 |
| 技术栈 | Go 1.22+（AI 推荐） | Java 17 + Spring Boot | 团队技术栈熟练度，Java 生态成熟度 |
| 消息队列 | Redis List / Stream | Redisson RDelayedQueue | 开箱即用的精准延迟投递，MVP 阶段最小底层开销 |
| 队列隔离 | 全局共享队列 | per-supplier 物理隔离 | 防止慢速供应商 IO 阻塞线程，导致全局雪崩 |
| 认证方式 | 完整 SSO/LDAP | 硬编码账号 + 环境变量覆盖 | MVP 够用，后续可替换 |

## 6. 未来演进方向

- **认证**：Session → 企业 SSO / LDAP / OAuth 2.0
- **限流**：全局线程上限 → per-supplier 令牌桶限流
- **消息队列**：Redis → RocketMQ / Kafka（当单实例 Redis 吞吐达到瓶颈时）
- **多租户**：当前单一 namespace → 基于 tenant 的逻辑隔离
- **可观测性**：Prometheus 指标 → 分布式链路追踪（OpenTelemetry）
- **水平扩展**：当前已支持多实例竞争消费，未来可引入 K8s HPA 自动扩缩

## 7. 技术栈

| 组件 | 选型 | 版本 |
|------|------|------|
| 后端 | Spring Boot | 3.2.12 |
| JDK | OpenJDK | 17 LTS |
| 前端 | Vue 3 + Element Plus + Vite | 3.5 / 2.9 / 6.0 |
| 消息队列 | Redis + Redisson | 7.0+ / 3.27.2 |
| 转换引擎 | jsonata-java | 0.9.7 |
| HTTP 客户端 | OkHttp | 4.12.0 |
| 数据库 | MySQL + MyBatis-Plus | 8.0+ / 3.5.5 |
| 监控 | Micrometer + Prometheus | — |

## 8. 快速开始

```bash
# 基础设施
docker run -d --name redis -p 6379:6379 redis:7-alpine
docker run -d --name mysql -p 3306:3306 \
    -e MYSQL_ROOT_PASSWORD=root \
    -e MYSQL_DATABASE=notification \
    mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

# 后端
cd notification-service
mvn clean package -DskipTests
java -jar target/notification-service-*.jar -Dspring-boot.run.profiles=dev

# 前端
cd notification-admin-ui
pnpm install && pnpm build
docker build -t notification-admin-ui . && docker run -d -p 80:80 notification-admin-ui
```

详见 [部署指南](docs/deployment.md) 和 [运维手册](docs/runbook.md)。

## 9. 项目结构

```
notification-service/                    # 后端服务 (Spring Boot)
├── src/main/java/com/rc/notification/
│   ├── interfaces/                      # 用户接口层 (API + Admin)
│   ├── application/                     # 应用层 (编排、Worker、DLQ)
│   ├── domain/                          # 领域层 (配置、转换引擎、凭证保险柜)
│   └── infrastructure/                  # 基础设施层 (持久化、缓存、HTTP、审计)
├── src/test/java/                       # 29 个集成测试
└── pom.xml

notification-admin-ui/                   # 管理后台前端 (Vue 3 SPA)
├── src/
│   ├── views/                           # 页面 (Login, SupplierList/Form, Simulation, DlqList)
│   ├── components/                      # 组件 (MonacoEditor, SimulationPanel, CredentialForm)
│   └── api/                             # Axios 接口封装
└── package.json
```

## 10. AI 使用说明

详见 [docs/HUMAN.md](docs/HUMAN.md)，包含完整的 AI 交互过程、未采纳的建议、以及个人关键决策。
