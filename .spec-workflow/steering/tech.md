# Technology Stack

## Project Type

企业级 API 通知中台（高内聚防腐层异步投递服务）。

## Core Technologies

### Primary Language(s)

- **Language**: Java 17 (LTS)
- **Runtime/Compiler**: OpenJDK 17 / Spring Boot 3.2.x
- **Language-specific tools**: Maven 3.9+, Jenkins

### Key Dependencies/Libraries

- **Spring Boot Starter Web**: 提供对内标准 RESTful API 接口。
- **Spring Boot Starter Actuator**: 提供健康检查、就绪探针与 Metrics 端点。
- **Redisson Starter**: 提供基于 Redis 的动态多物理队列管理（`RDelayedQueue`）。
- **MyBatis-Plus**: 负责持久化配置读取与死信队列（DLQ）日志记录。
- **JSONata**: 核心防腐层表达式引擎，负责动态报文拼接与函数预处理。
- **Caffeine**: 本地缓存
- **OkHttp3**: HttpClient

### Application Architecture

采用基于消息驱动的防腐层（ACL）单体微服务架构，核心组件间通过物理隔离的 Redis 延迟队列进行异步解耦。

### Data Storage

- **Primary storage**: MySQL 8.0（存储供应商配置规则、投递死信日志）
- **Caching**: Redis 7.0+（单机/主从，用于 Redisson 动态延迟队列、上游请求幂等去重缓存）
- **Data formats**: JSON（标准数据交换格式）

### External Integrations

- **APIs**: 对接三方广告系统、外部CRM、外部库存系统等异构供应商。
- **Protocols**: HTTP/REST, HTTPS
- **Authentication**: 支持通过 JSONata 动态生成各供应商所需的 API Keys, OAuth 2.0 Bearer 令牌, 或自定义签名 Header。

## Technical Requirements & Constraints

### Performance Requirements

- 中台内部处理（接收、校验、JSONata 解析入队）耗时：TP99 < 20ms，TPS ≥ 10000。
- 队列调度精度：在 Redis 无高负载情况下，延迟事件触发误差控制在 ±1s 以内。

### Availability Requirements

- **Redis 高可用**：生产环境必须部署 Redis Sentinel 或 Redis Cluster 模式，禁止单节点裸跑。Redis 作为延迟队列、分布式锁和幂等状态的核心载体，其不可用将导致入队链路完全中断。开发/测试环境可使用单节点。
- **Redis 不可用降级策略**：当 Redis 连接异常时，系统进入降级模式——入队接口返回 `503 Service Unavailable` 并携带 `Retry-After` 头，由上游业务系统自行缓存重试，避免静默丢消息。Caffeine 本地缓存在 Redis 不可用期间继续提供供应商配置的只读服务。

### Security & Compliance

- **Security Requirements**: 所有供应商的敏感通信密钥（如 AppSecret）在 MySQL 中进行 AES-256-GCM 加密存储。

## Technical Decisions & Rationale

### Decision Log

1. **Redisson 物理队列隔离**: 为每个供应商分配独立的 `RDelayedQueue`。放弃了通用单队列分发方案，确保单一外部供应商不可用时，重试波及范围完全隔离。
2. **Redis Sentinel/Cluster 强制要求**: 生产环境禁止 Redis 单点部署。系统对 Redis 的依赖覆盖分布式锁、幂等状态、延迟队列三大核心链路，单点故障将导致全局不可用。
3. **降级拒绝优于静默丢失**: Redis 不可用时选择拒绝入队（503）而非本地缓存消息，因为本地缓存无法保证分布式幂等性，可能引发重复投递资损。
