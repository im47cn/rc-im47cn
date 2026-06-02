# Requirements Document

## Introduction

企业级 API 通知中台（防腐层系统），解决内部多个核心业务系统与外部异构供应商进行 HTTP(S) 异步通知时的强耦合、格式异构、网络不稳定导致的投递失败等问题。系统作为高内聚的防腐层（ACL），将外部供应商 API 的复杂性完全屏蔽在中台内部，为上游业务系统提供统一、高可用的标准通知投递服务。

## Alignment with Product Vision

本系统直接实现 product.md 定义的三大业务目标：

1. **业务解耦** — 上游系统仅对接标准接口，外部 API 变更不影响核心链路
2. **投递可靠性** — At-Least-Once 语义 + 自愈重试，关键通知至少送达一次
3. **降低研发成本** — 新供应商接入由"开发代码"转为"配置表达式"，耗时从天级缩短至分钟级

## Requirements

### REQ-1: 统一事件摄取

**User Story:** 作为内部业务线开发人员，我希望通过一个标准化的 REST API 提交通知事件，这样我无需关心外部供应商的任何 API 细节。

#### Acceptance Criteria

1. WHEN 上游系统发送 POST 请求至摄取端点 THEN 系统 SHALL 校验 Payload 结构并返回标准响应（含唯一事件 ID）
2. IF Payload 缺少必填字段（supplier_code, event_type, payload） THEN 系统 SHALL 返回 400 错误并明确指出缺失字段
3. WHEN 同一 requestId 在幂等窗口内重复提交 THEN 系统 SHALL 返回幂等命中响应（202）而非重复入队
4. IF Redis 不可用 THEN 系统 SHALL 返回 503 Service Unavailable 并携带 Retry-After 头

### REQ-2: 防腐层报文转换引擎

**User Story:** 作为技术运营人员，我希望通过 JSON 模板和 JSONata 表达式配置供应商的映射规则，这样接入新供应商无需编写代码。

#### Acceptance Criteria

1. WHEN 事件到达投递阶段 THEN 系统 SHALL 使用供应商配置的 JSONata 模板将标准 Payload 转换为供应商特定格式
2. WHEN JSONata 表达式包含不安全操作（如无限循环、访问系统资源） THEN 系统 SHALL 在沙箱环境中拦截并拒绝执行
3. IF 模板配置了动态加密预处理函数 THEN 系统 SHALL 在报文组装阶段执行加密并将结果嵌入最终请求
4. WHEN 供应商配置变更 THEN 系统 SHALL 在 60 秒内通过缓存失效机制感知并应用新配置

### REQ-3: 供应商级隔离队列与投递

**User Story:** 作为技术运营人员，我希望每个供应商有独立的投递队列，这样单一供应商的故障不会波及其他供应商的通知投递。

#### Acceptance Criteria

1. WHEN 事件入队 THEN 系统 SHALL 根据 supplier_code 路由至该供应商的独立 Redisson 延迟队列
2. WHEN Worker 从队列取出事件 THEN 系统 SHALL 按供应商配置组装完整 HTTP 请求（URL、Header、Body）并发送
3. IF 供应商响应匹配配置的成功判定规则（HTTP 状态码 / 响应体内容匹配） THEN 系统 SHALL 标记投递成功
4. WHEN 新供应商配置被添加 THEN 系统 SHALL 自动为其创建独立的 Worker 线程和队列

### REQ-4: 定制化指数避让重试

**User Story:** 作为技术运营人员，我希望为不同供应商独立配置重试策略，这样可以根据供应商的容错特性优化投递成功率。

#### Acceptance Criteria

1. WHEN 投递失败且未达最大重试次数 THEN 系统 SHALL 按指数避让公式计算下次投递延迟并重新入队
2. IF 供应商配置了自定义重试参数（最大次数、初始延迟、退避因子） THEN 系统 SHALL 使用供应商级参数而非全局默认值
3. WHEN 重试次数达到最大值 THEN 系统 SHALL 将事件转入死信队列

### REQ-5: 死信队列降级与运维管理

**User Story:** 作为技术运营人员，我希望能查看和管理死信队列中的失败通知，这样可以在供应商恢复后批量重试或人工处理。

#### Acceptance Criteria

1. WHEN 事件进入死信队列 THEN 系统 SHALL 将完整记录（原始 Payload、转换后请求、历次失败原因、触发时间）持久化至 MySQL
2. WHEN 事件进入死信队列 THEN 系统 SHALL 输出标准化 Error 日志供外部告警系统拦截
3. WHEN 运维人员触发单条/批量重试 THEN 系统 SHALL 原子性地重置状态并重新入队
4. IF 运维人员标记事件为"忽略" THEN 系统 SHALL 更新状态并记录操作人身份

### REQ-6: 供应商配置管理后台

**User Story:** 作为技术运营人员，我希望通过 Web 管理界面进行供应商配置的增删改查和 JSONata 表达式在线调试，这样可以快速完成供应商接入。

#### Acceptance Criteria

1. WHEN 管理员登录管理后台 THEN 系统 SHALL 通过 Session 鉴权验证身份
2. WHEN 管理员创建/编辑供应商配置 THEN 系统 SHALL 提供包含 JSONata 模板编辑器的表单界面
3. WHEN 管理员使用仿真功能 THEN 系统 SHALL 接受输入 Payload 和 JSONata 模板，实时返回转换结果
4. IF 供应商配置包含敏感凭证 THEN 系统 SHALL 在存储时加密、在界面展示时脱敏

### REQ-7: 凭证安全管理

**User Story:** 作为系统管理员，我希望供应商的敏感密钥被安全加密存储，这样即使数据库泄露也不会暴露明文凭证。

#### Acceptance Criteria

1. WHEN 供应商配置包含敏感字段（AppSecret、API Key） THEN 系统 SHALL 使用 AES-256-GCM 加密后存储至 MySQL
2. WHEN 系统需要使用凭证发送请求 THEN 系统 SHALL 在运行时解密，解密后的明文仅存在于内存中
3. WHEN 审计日志记录包含凭证信息 THEN 系统 SHALL 对凭证进行脱敏处理（仅显示前4位+掩码）

## Non-Functional Requirements

### Code Architecture and Modularity

- **Single Responsibility Principle**: 严格遵循 DDD 四层架构（interfaces / application / domain / infrastructure），每层职责单一
- **Modular Design**: 供应商隔离的队列与 Worker 设计确保故障域隔离
- **Dependency Management**: domain 层零外部依赖，infrastructure 层封装所有技术细节
- **Clear Interfaces**: 上游仅对接 EventIngestionController 标准接口

### Performance

- 摄取链路（接收、校验、JSONata 解析、入队）TP99 `< 20ms`，TPS `>= 10,000`
- 队列调度精度：延迟事件触发误差 `<= 1s`（Redis 正常负载下）
- Caffeine 本地缓存命中率 > 95%，配置读取零网络开销

### Security

- 供应商敏感凭证 AES-256-GCM 加密存储
- JSONata 沙箱执行，防止 RCE 攻击
- 管理后台 Session 鉴权，凭证界面脱敏
- 审计日志中凭证信息自动脱敏

### Reliability

- At-Least-Once 投递语义保证
- 首次投递成功率 `>= 95%`，含重试最终成功率 `>= 99.9%`
- Redis 不可用时入队降级为 503 拒绝，优先正确性而非可用性
- 优雅停机保证零消息丢失（在途任务完成或重新入队）
- 死信队列完整保留失败上下文供追溯

### Usability

- 新供应商配置接入耗时 `<= 10 分钟`
- JSONata 在线仿真器支持实时调试
- 管理后台提供供应商配置 CRUD 和死信队列可视化管理
- 开发环境轻量级运行，满足 MVP 交付标准
