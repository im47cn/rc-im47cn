# Project Structure

## Directory Organization

```
notification-service/                              # 后端服务 (Spring Boot)
├── src/main/java/com/rc/notification/
│   ├── interfaces/                                # 用户接口层
│   │   ├── api/                                   #   事件摄取 API (EventIngestionController)
│   │   └── admin/                                 #   管理后台 API
│   │       ├── AuthController.java                #     登录/登出
│   │       ├── AdminAuthFilter.java               #     Session 鉴权过滤器
│   │       ├── SupplierConfigController.java      #     供应商配置 CRUD
│   │       ├── SimulationController.java          #     JSONata 在线仿真
│   │       └── DlqManagementController.java       #     死信队列管理
│   ├── application/                               # 应用层
│   │   ├── service/                               #   入队编排 (IngestionService)
│   │   ├── worker/                                #   Worker 管理与投递执行
│   │   ├── admin/                                 #   管理后台业务逻辑
│   │   ├── dlq/                                   #   死信队列服务
│   │   └── event/                                 #   领域事件发布订阅
│   ├── domain/                                    # 领域层
│   │   ├── config/                                #   供应商配置实体与路由规则
│   │   ├── translation/                           #   JSONata 沙箱转换引擎
│   │   └── credential/                            #   凭证加解密保险柜 (CredentialVault)
│   └── infrastructure/                            # 基础设施层
│       ├── persistence/                           #   MyBatis-Plus Entity/Mapper
│       ├── cache/                                 #   Caffeine 缓存 + Redis Pub/Sub
│       ├── http/                                  #   OkHttp 动态客户端 (FullStackHttpRequestBuilder)
│       ├── audit/                                 #   Logback 异步审计组件
│       ├── health/                                #   Actuator 健康检查
│       └── metrics/                               #   Micrometer 业务指标
├── src/main/resources/
│   ├── application.yml
│   └── logback-spring.xml
├── src/test/java/                                 # 测试套件
│   ├── unit/                                      #   单元测试
│   └── integration/                               #   端到端集成测试
└── pom.xml

notification-admin-ui/                             # 管理后台前端 (Vue 3 SPA)
├── src/
│   ├── api/                                       #   Axios 接口封装
│   ├── components/                                #   通用组件 (MonacoEditor, SimulationPanel, CredentialForm)
│   ├── views/                                     #   页面 (Login, SupplierList, SupplierForm, Simulation, DlqList)
│   ├── router/                                    #   Vue Router 路由定义
│   ├── stores/                                    #   Pinia 状态管理
│   └── utils/                                     #   Axios 实例与工具函数
├── vite.config.ts
├── tsconfig.json
└── package.json
```

## Naming Conventions

### Files

- **Services/Handlers**: 统一采用 `PascalCase` 并以 `Service` 或 `Worker` 结尾，如 `NotificationDeliveryWorker.java`。
- **Engine Components**: 统一以 `Engine` 或 `Strategy` 结尾，如 `JsonataTranslationEngine.java`。
- **Controllers**: 统一以 `Controller` 结尾，如 `EventIngestionController.java`。
- **Filters**: 统一以 `Filter` 结尾，如 `AdminAuthFilter.java`。
- **Vue Components**: 统一采用 `PascalCase`，如 `MonacoEditor.vue`、`SimulationPanel.vue`。
- **Vue Pages**: 统一以 `View` 结尾，如 `SupplierListView.vue`、`DlqListView.vue`。
