# Design: Swagger API Documentation & Full-Stack Testing

## Overview

For the notification service ACL gateway, add two capabilities:
1. **SpringDoc OpenAPI** self-describing API documentation for all 13 REST endpoints
2. **Full-stack test suite** covering backend (unit + integration with Testcontainers) and frontend (Vitest + Vue Test Utils)

## Part 1: Swagger API Documentation

### Technology Choice

| Option | Status | Reason |
|--------|--------|--------|
| SpringDoc OpenAPI 2.x | Selected | Spring Boot 3.x native support, active maintenance |
| Springfox 3.x | Rejected | Unmaintained since 2020, incompatible with Spring Boot 3 |

### Implementation

**Dependency:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.8</version>
</dependency>
```

**Configuration (`OpenApiConfig.java`):**
- Two API groups: `public-api` (event ingestion) and `admin-api` (management endpoints)
- Session-based authentication description
- Server URL and description

**Annotation strategy:**
- `@Tag` on each controller class
- `@Operation` + `@ApiResponse` on each endpoint method
- `@Schema` on DTO classes for field descriptions
- Reuse existing `@Valid` annotations for constraint documentation

**Endpoint groups:**

| Group | Path Pattern | Endpoints |
|-------|-------------|-----------|
| Public API | `/api/v1/notifications/**` | 1 (ingest) |
| Admin API | `/api/v1/admin/**` | 12 (auth, suppliers, dlq, simulation) |

**Access URLs:**
- Swagger UI: `/swagger-ui.html`
- OpenAPI spec: `/v3/api-docs`

**AdminAuthFilter adjustment:**
- Exclude `/swagger-ui/**` and `/v3/api-docs/**` from auth interception

## Part 2: Backend Testing

### Framework Stack

| Component | Purpose |
|-----------|---------|
| JUnit 5 | Test runner |
| Mockito | Mocking dependencies |
| MockMvc | HTTP endpoint testing |
| Testcontainers (Redis) | Real Redis for integration tests |
| Testcontainers (MySQL) | Real MySQL for integration tests |
| H2 | Fallback for lightweight tests (already configured) |

### Test Layering Strategy

#### Layer 1: Domain Unit Tests (Pure Logic, Zero Dependencies)

| Test Class | Target | Key Scenarios |
|------------|--------|---------------|
| `CredentialVaultTest` | AES-256-GCM encrypt/decrypt | Symmetric roundtrip, different key formats (Base64/UTF-8), tampered ciphertext detection, empty/null input |
| `JsonataTranslationEngineTest` | JSONata expression evaluation | Simple path extraction, nested object transform, array mapping, syntax error handling, null/missing field graceful handling |
| `BackoffCalculationTest` | Exponential backoff formula | Initial delay, multiplier progression, max ceiling cap, edge cases (retry=0, retry=max) |

#### Layer 2: Application Unit Tests (Mockito Mocks)

| Test Class | Target | Key Scenarios |
|------------|--------|---------------|
| `DeliveryWorkerTest` | Worker delivery + retry flow | Successful delivery, transient failure retry, max retry exhaustion to DLQ, non-retryable error (JSONata) skip retry |
| `SupplierConfigAdminServiceTest` | Supplier CRUD orchestration | Create with credential encryption, update with cache eviction, duplicate code rejection |
| `EventIngestionServiceTest` | Idempotent ingestion | New event accepted, duplicate rejected (PROCESSING), duplicate rejected (SUCCESS), duplicate rejected (DEAD_LETTERED) |

#### Layer 3: API Integration Tests (MockMvc)

| Test Class | Target | Key Scenarios |
|------------|--------|---------------|
| `EventIngestionControllerTest` | POST /ingest | Valid event 200, missing required fields 400, idempotent hit 200 |
| `SupplierConfigControllerTest` | CRUD /suppliers | List with pagination, create valid, create duplicate code 409, get with credential masking |
| `DlqManagementControllerTest` | DLQ operations | List with filters, single retry, batch retry, ignore |
| `SimulationControllerTest` | Simulation endpoints | Valid transform, syntax error with offset, full preview |
| `AuthControllerTest` | Login/logout | Valid login, invalid credentials 401, access without session 401 |

#### Layer 4: Infrastructure Integration Tests (Testcontainers)

| Test Class | Target | Key Scenarios |
|------------|--------|---------------|
| `RedisIdempotentStateTest` | Redis state lifecycle | PROCESSING -> SUCCESS with TTL shrink, PROCESSING -> DEAD_LETTERED with TTL preserved, lock acquire/release |
| `RedisQueueIntegrationTest` | RDelayedQueue behavior | Enqueue/dequeue, delayed delivery timing, queue isolation per supplier |
| `DlqRepositoryIntegrationTest` | MySQL DLQ persistence | Insert on retry exhaustion, unique constraint on biz_sign, status update |

### Test Configuration

**Testcontainers base class:**
```java
@SpringBootTest
@Testcontainers
abstract class IntegrationTestBase {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0").withDatabaseName("notification_test");
}
```

**Test profile (`application-test.yml`):**
- Testcontainers dynamic port binding via `@DynamicPropertySource`
- Reduced timeouts for faster test execution
- Test credential master key

## Part 3: Frontend Testing

### Framework Stack

| Component | Purpose |
|-----------|---------|
| Vitest | Test runner (Vite-native, fast) |
| @vue/test-utils | Vue component mounting and interaction |
| MSW (Mock Service Worker) | API mocking at network level |
| @pinia/testing | Pinia store testing utilities |

### Test Coverage

| Test File | Component | Key Scenarios |
|-----------|-----------|---------------|
| `CredentialForm.spec.ts` | CredentialForm.vue | Add/remove KV pairs, edit mode masking (******), getSubmitData filters keepOriginal, toggle reenter |
| `MonacoEditor.spec.ts` | MonacoEditor.vue | v-model binding, marker rendering, language switching |
| `SimulationPanel.spec.ts` | SimulationPanel.vue | 500ms debounce fires transform API, error offset to line/column conversion, fullPreview loading state |
| `SupplierFormView.spec.ts` | SupplierFormView.vue | Required field validation, Boolean-to-Integer conversion on submit, Integer-to-Boolean on load, empty template to null |
| `DlqListView.spec.ts` | DlqListView.vue | Batch selection, retry with operator prompt, ignore confirmation |
| `request.spec.ts` | utils/request.ts | 401 response triggers redirect to /login with callback |

### Frontend Test Configuration

**vitest.config.ts:**
```typescript
export default defineConfig({
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  }
})
```

**MSW setup (`src/test/setup.ts`):**
- Mock handlers for all 13 API endpoints
- Configurable response overrides per test

## Non-Goals

- Performance/load testing
- E2E browser testing (Playwright/Cypress)
- 100% code coverage target (focus on high-risk paths)
- Frontend i18n or responsive design fixes

## File Structure

```
notification-service/
  src/test/java/.../
    domain/
      CredentialVaultTest.java
      JsonataTranslationEngineTest.java
      BackoffCalculationTest.java
    application/
      DeliveryWorkerTest.java
      SupplierConfigAdminServiceTest.java
      EventIngestionServiceTest.java
    interfaces/
      EventIngestionControllerTest.java
      SupplierConfigControllerTest.java
      DlqManagementControllerTest.java
      SimulationControllerTest.java
      AuthControllerTest.java
    infrastructure/
      RedisIdempotentStateTest.java
      RedisQueueIntegrationTest.java
      DlqRepositoryIntegrationTest.java
      IntegrationTestBase.java

notification-admin-ui/
  src/test/
    setup.ts
    mocks/handlers.ts
  src/components/__tests__/
    CredentialForm.spec.ts
    MonacoEditor.spec.ts
    SimulationPanel.spec.ts
  src/views/__tests__/
    SupplierFormView.spec.ts
    DlqListView.spec.ts
  src/utils/__tests__/
    request.spec.ts
```
