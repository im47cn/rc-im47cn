# Notification Service 部署指南

## 1. 环境要求

| 组件 | 最低版本 | 说明 |
|------|---------|------|
| JDK | 17 LTS (OpenJDK) | Spring Boot 3.2.x 强制要求 |
| MySQL | 8.0+ | InnoDB 引擎，utf8mb4 字符集 |
| Redis | 7.0+ | 生产环境必须 Sentinel 或 Cluster 模式 |
| Maven | 3.9+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| pnpm | 8+ | 前端包管理器 |

---

## 2. 环境变量清单

| 变量名 | 必填 | 默认值 | 说明 |
|--------|------|--------|------|
| `CREDENTIAL_MASTER_KEY` | **是（生产）** | `CHANGE_ME_IN_PRODUCTION` | AES-256-GCM 加密主密钥，用于供应商凭证加解密。**必须为 32 字节 Base64 编码字符串，严禁使用默认值上线** |
| `ADMIN_USERNAME` | 否 | `admin` | 管理后台登录用户名 |
| `ADMIN_PASSWORD` | 否 | `admin` | 管理后台登录密码。**生产环境必须修改** |
| `SPRING_DATASOURCE_URL` | 否 | `jdbc:mysql://localhost:3306/notification?...` | MySQL JDBC 连接地址 |
| `SPRING_DATASOURCE_USERNAME` | 否 | `root` | MySQL 用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 否 | `root` | MySQL 密码 |
| `SPRING_DATA_REDIS_HOST` | 否 | `localhost` | Redis 主机地址（单机模式） |
| `SPRING_DATA_REDIS_PORT` | 否 | `6379` | Redis 端口（单机模式） |
| `CORS_ALLOWED_ORIGINS` | 否 | — | 预留，当前前端集成在后端 static 资源中 |

### Spring 配置属性参考

以下属性在 `application.yml` 中定义，可通过环境变量或外部配置文件覆盖：

```yaml
# 服务端口
server:
  port: 8080

# Worker 线程池配置
notification:
  worker:
    max-worker-threads: 200        # Worker 线程硬上限
    shutdown-await-seconds: 30     # 优雅停机等待窗口（秒）

# 凭证主密钥
credential:
  master-key: ${CREDENTIAL_MASTER_KEY:CHANGE_ME_IN_PRODUCTION}

# 管理后台账号
admin:
  username: ${ADMIN_USERNAME:admin}
  password: ${ADMIN_PASSWORD:admin}
```

---

## 3. MySQL Schema 初始化

DDL 脚本位于项目目录：

```
notification-service/src/main/resources/db/migration/
  V1__create_supplier_config.sql        # 供应商配置表
  V2__create_notification_dlq_log.sql   # 死信队列日志表
```

执行初始化：

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS notification DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 执行 DDL 脚本（按版本号顺序）
mysql -u root -p notification < src/main/resources/db/migration/V1__create_supplier_config.sql
mysql -u root -p notification < src/main/resources/db/migration/V2__create_notification_dlq_log.sql
```

### 核心表结构概览

- **`supplier_config`**: 供应商配置（base_url、JSONata 模板、超时、重试策略、凭证密文等）
- **`notification_dlq_log`**: 死信保险库（完整运行时上下文、错误信息、重试计数、处理状态）

---

## 4. Redis 配置

### 开发/测试环境

单节点模式，使用默认配置即可：

```yaml
spring.data.redis:
  host: localhost
  port: 6379
```

### 生产环境（必须使用 Sentinel 或 Cluster）

**生产环境禁止 Redis 单节点部署。** Redis 承载分布式锁、幂等状态判重和延迟队列三大核心链路，单点故障将导致全局不可用。

#### Sentinel 模式配置示例

```yaml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - sentinel-1:26379
          - sentinel-2:26379
          - sentinel-3:26379
      password: <redis-password>
```

#### Cluster 模式配置示例

```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - redis-1:6379
          - redis-2:6379
          - redis-3:6379
          - redis-4:6379
          - redis-5:6379
          - redis-6:6379
      password: <redis-password>
```

### Redis Key 命名空间

| Key 模式 | 用途 | TTL |
|----------|------|-----|
| `lock:dispatch:{biz_sign}` | 分布式并发防重入锁 | 5s |
| `status:dispatch:{biz_sign}` | 幂等状态判重 | 24h（成功后缩短至 1h） |
| Redisson 队列（按 supplierCode 隔离） | 延迟消息队列 | 持久化 |
| `Notification:Config:Evict` | Pub/Sub 缓存驱逐通道 | N/A |

---

## 5. 前端构建与集成

前端项目位于 `notification-admin-ui/`，生产构建输出集成到 Spring Boot 静态资源中。

```bash
cd notification-admin-ui
pnpm install
pnpm build    # 产物输出至 notification-service/src/main/resources/static/admin/
```

构建完成后，前端通过 Spring Boot 的 `/admin/` 路径提供服务，后端自动处理 SPA history mode 回退。

---

## 6. 后端构建与打包

```bash
cd notification-service

# 编译打包（跳过测试用于快速构建）
mvn clean package -DskipTests

# 运行
java -jar target/notification-service-*.jar \
  --spring.profiles.active=prod \
  -DCREDENTIAL_MASTER_KEY=<your-master-key> \
  -DADMIN_USERNAME=<admin-user> \
  -DADMIN_PASSWORD=<admin-pass>
```

---

## 7. Prometheus 监控采集配置

服务暴露 Prometheus 指标端点：`/actuator/prometheus`

### Prometheus scrape_config

```yaml
scrape_configs:
  - job_name: 'notification-service'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['<service-host>:8080']
    # 如使用 K8s Service Discovery，替换为:
    # kubernetes_sd_configs:
    #   - role: pod
    # relabel_configs:
    #   - source_labels: [__meta_kubernetes_pod_label_app]
    #     regex: notification-service
    #     action: keep
```

### 核心业务指标

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `notification_ingest_total` | Counter | `supplier_code`, `result` | 入队请求总数（accepted/rejected/idempotent_hit） |
| `notification_delivery_total` | Counter | `supplier_code`, `outcome` | 投递尝试总数（success/failed/dlq） |
| `notification_delivery_duration` | Timer | `supplier_code` | 单次投递耗时 |
| `notification_queue_depth` | Gauge | `supplier_code` | 各供应商队列积压深度 |
| `notification_worker_active` | Gauge | - | 当前活跃 Worker 线程数 |
| `notification_dlq_pending` | Gauge | - | 待处理死信数量 |

### 建议告警规则

```yaml
groups:
  - name: notification-service
    rules:
      - alert: DlqPendingHigh
        expr: notification_dlq_pending > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "死信队列积压超过 10 条"

      - alert: DeliveryFailureRateHigh
        expr: rate(notification_delivery_total{outcome="failed"}[5m]) / rate(notification_delivery_total[5m]) > 0.5
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "投递失败率超过 50%"

      - alert: WorkerCapacityNearLimit
        expr: notification_worker_active / 200 > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Worker 线程使用率超过 90%"
```

---

## 8. K8s 部署配置

### Liveness / Readiness Probe

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notification-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: notification-service
  template:
    metadata:
      labels:
        app: notification-service
    spec:
      containers:
        - name: notification-service
          image: <registry>/notification-service:<tag>
          ports:
            - containerPort: 8080
          env:
            - name: CREDENTIAL_MASTER_KEY
              valueFrom:
                secretKeyRef:
                  name: notification-secrets
                  key: credential-master-key
            - name: ADMIN_USERNAME
              valueFrom:
                secretKeyRef:
                  name: notification-secrets
                  key: admin-username
            - name: ADMIN_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: notification-secrets
                  key: admin-password
            - name: SPRING_DATASOURCE_URL
              value: "jdbc:mysql://mysql-service:3306/notification?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai"
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: notification-secrets
                  key: db-username
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: notification-secrets
                  key: db-password
            - name: SPRING_DATA_REDIS_SENTINEL_MASTER
              value: "mymaster"
            - name: SPRING_DATA_REDIS_SENTINEL_NODES
              value: "redis-sentinel-0:26379,redis-sentinel-1:26379,redis-sentinel-2:26379"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
            failureThreshold: 3
          resources:
            requests:
              cpu: "500m"
              memory: "512Mi"
            limits:
              cpu: "2"
              memory: "2Gi"
```

### Readiness Probe 组合检查项

Readiness 探针 (`/actuator/health/readiness`) 组合检查以下条件，任一失败返回 `OUT_OF_SERVICE`：

1. Redis 连接可用
2. MySQL 连接可用
3. 至少一个 Worker 线程处于活跃状态

当返回 `OUT_OF_SERVICE` 时，K8s 自动将该 Pod 从 Service Endpoints 摘除，停止接收流量。

---

## 9. 上线检查清单

### 上线前

- [ ] `CREDENTIAL_MASTER_KEY` 已设置为安全的 32 字节密钥，非默认值
- [ ] `ADMIN_USERNAME` / `ADMIN_PASSWORD` 已修改为强密码
- [ ] MySQL 数据库已创建，DDL 脚本已执行
- [ ] Redis Sentinel/Cluster 已部署并可达（禁止单节点）
- [ ] 前端已构建并集成到后端 static 资源中
- [ ] Prometheus scrape 配置已添加
- [ ] K8s Secret 已创建（包含所有敏感配置）
- [ ] K8s Deployment Probe 配置已验证
- [ ] `notification.worker.max-worker-threads` 已根据预期供应商数量调整

### 上线后验证

- [ ] `/actuator/health/liveness` 返回 `UP`
- [ ] `/actuator/health/readiness` 返回 `UP`
- [ ] `/actuator/prometheus` 可正常采集指标
- [ ] 前端页面可通过 `/admin/` 路径正常访问
- [ ] 管理后台可正常登录
- [ ] 发送测试通知事件，验证完整链路（入队 -> 投递 -> 审计日志）
- [ ] 审计日志文件 `logs/audit.log` 正常输出
