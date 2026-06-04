# Event Registration & Subscription Model Design

> Date: 2026-06-03
> Status: Approved
> Approach: Incremental Evolution (Option A)

## 1. Background & Problem

Current architecture is supplier-centric: callers must specify `supplierCode` to route events, which contradicts the platform's stated goal as an Anti-Corruption Layer (ACL). Key issues:

- Business systems must know supplier identifiers (tight coupling)
- 1:1 binding: one event can only be delivered to one supplier
- `eventType` field exists in DTO but has no registration, routing, or validation logic
- All templates are attached to supplier config; same supplier cannot use different templates for different event types

## 2. Target Model: Publish-Subscribe

```
Publisher (business system)  --registers-->  EventType
Subscriber (formerly supplier) --subscribes-->  EventType via Subscription
Platform routes events based on subscription relationships (fan-out)
```

Three roles:
- **Publisher**: registers event types, sends events, does not care about downstream
- **Subscriber** (renamed from Supplier): subscribes to event types, configures delivery (templates/timeouts/retry)
- **Platform**: event catalog management, fallback configuration, permission control

## 3. Domain Model

### 3.1 New Entities

#### Publisher

| Field | Type | Description |
|-------|------|-------------|
| publisherCode | VARCHAR(64) PK | Unique identifier (e.g. "order-service") |
| publisherName | VARCHAR(128) | Display name |
| apiKey | VARCHAR(256) | Ingest API authentication credential |
| status | TINYINT | 0=disabled, 1=enabled |
| contactInfo | VARCHAR(256) | Contact for incident notification |
| createTime | DATETIME | |
| updateTime | DATETIME | |

#### EventType

| Field | Type | Description |
|-------|------|-------------|
| eventTypeCode | VARCHAR(128) PK | Unique identifier (e.g. "ORDER_CREATED") |
| publisherCode | VARCHAR(64) FK | Owning publisher |
| displayName | VARCHAR(128) | Display name |
| description | TEXT | |
| payloadSchema | TEXT | Optional JSON Schema for payload validation |
| status | VARCHAR(16) | DRAFT / ACTIVE / DEPRECATED |
| version | INT | Incremented on schema change |
| createTime | DATETIME | |
| updateTime | DATETIME | |

#### Subscription

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT PK | Auto-increment |
| subscriberCode | VARCHAR(64) FK | Which subscriber |
| eventTypeCode | VARCHAR(128) FK | Which event type |
| status | VARCHAR(16) | ACTIVE / SUSPENDED |
| managedBy | VARCHAR(16) | SUBSCRIBER / PUBLISHER / PLATFORM |
| pathTemplate | TEXT | Override, null = inherit from SubscriberConfig |
| queryTemplate | TEXT | Override |
| headerTemplate | TEXT | Override |
| bodyTemplate | TEXT | Override |
| connectTimeoutMs | INT | Override |
| readTimeoutMs | INT | Override |
| maxRetryCount | INT | Override |
| retryBackoffInitialMs | INT | Override |
| retryBackoffMultiplier | DECIMAL(5,2) | Override |
| retryBackoffMaxMs | INT | Override |
| successHttpCodes | VARCHAR(64) | Override |
| successBodyPattern | VARCHAR(512) | Override |
| successBodyMatchMode | VARCHAR(16) | Override |
| createTime | DATETIME | |
| updateTime | DATETIME | |

UNIQUE constraint: (subscriberCode, eventTypeCode)

managedBy default logic:
- Subscriber creates -> SUBSCRIBER
- Publisher creates (for external subscriber) -> PUBLISHER
- Platform admin creates -> PLATFORM

#### FieldFingerprint (runtime detection)

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT PK | Auto-increment |
| eventTypeCode | VARCHAR(128) FK | |
| fieldPath | VARCHAR(256) | JSON Path (e.g. "payload.address.city") |
| observedType | VARCHAR(16) | STRING / NUMBER / BOOLEAN / OBJECT / ARRAY / NULL |
| firstSeenAt | DATETIME | |
| lastSeenAt | DATETIME | |
| sampleCount | INT | |
| status | VARCHAR(16) | ACTIVE / DISAPPEARED |

UNIQUE constraint: (eventTypeCode, fieldPath)

#### ChangeRecord

| Field | Type | Description |
|-------|------|-------------|
| id | BIGINT PK | Auto-increment |
| eventTypeCode | VARCHAR(128) FK | |
| changeType | VARCHAR(32) | FIELD_ADDED / FIELD_REMOVED / FIELD_TYPE_CHANGED / SCHEMA_UPDATED |
| fieldPath | VARCHAR(256) | Affected field |
| oldValue | VARCHAR(512) | Before (type or schema fragment) |
| newValue | VARCHAR(512) | After |
| detectionSource | VARCHAR(16) | SCHEMA_DIFF / RUNTIME_INFERRED |
| confidence | VARCHAR(8) | HIGH / MEDIUM / LOW |
| status | VARCHAR(16) | PENDING_REVIEW / CONFIRMED / DISMISSED |
| affectedSubscriptions | TEXT | JSON array of affected subscriberCodes |
| createdAt | DATETIME | |

### 3.2 Renamed Entity

`SupplierConfig` -> `SubscriberConfig`

All existing fields remain. The entity serves as the channel-level default configuration. Subscription-level overrides take precedence via coalesce logic.

### 3.3 Configuration Override Rule

```
Effective config = Subscription field ?? SubscriberConfig default field

Channel-level fields (always from SubscriberConfig, never overridden):
  - baseUrl
  - credentialsEncrypted
  - httpMethod
  - contentTypeBehavior
  - workerConcurrency

Overridable fields (Subscription > SubscriberConfig):
  - pathTemplate, queryTemplate, headerTemplate, bodyTemplate
  - connectTimeoutMs, readTimeoutMs
  - maxRetryCount, retryBackoffInitialMs, retryBackoffMultiplier, retryBackoffMaxMs
  - successHttpCodes, successBodyPattern, successBodyMatchMode, successCaseSensitive
```

Merge pseudocode:

```java
EffectiveConfig resolve(String subscriberCode, String eventTypeCode) {
    SubscriberConfig base = subscriberConfigRepo.getByCode(subscriberCode);
    Subscription sub = subscriptionRepo.find(subscriberCode, eventTypeCode);

    if (sub == null) return EffectiveConfig.from(base);  // v1 compat

    return EffectiveConfig.builder()
        .baseUrl(base.getBaseUrl())
        .credentials(base.getCredentialsEncrypted())
        .httpMethod(base.getHttpMethod())
        .pathTemplate(coalesce(sub.getPathTemplate(), base.getPathTemplate()))
        .bodyTemplate(coalesce(sub.getBodyTemplate(), base.getBodyTemplate()))
        .readTimeoutMs(coalesce(sub.getReadTimeoutMs(), base.getReadTimeoutMs()))
        .maxRetryCount(coalesce(sub.getMaxRetryCount(), base.getMaxRetryCount()))
        // ... remaining overridable fields
        .build();
}
```

## 4. Idempotency & Status Model

### 4.1 Per-Subscriber Status

```
Current: status:dispatch:{eventId}             -> single status value
Changed: status:dispatch:{eventId}:{subscriberCode} -> per-subscriber status

Lock:    lock:dispatch:{eventId}:{subscriberCode}    -> per-subscriber granularity
```

### 4.2 Aggregate View

```
Redis Hash: dispatch:summary:{eventId}
  { "ALIYUN": "PROCESSING", "WECHAT": "SUCCESS", "DINGTALK": "DEAD_LETTERED" }

Updated atomically on each per-subscriber status transition.

Aggregate status derivation:
  ALL_SUCCESS    = all subscribers SUCCESS
  PARTIAL_FAIL   = at least one DEAD_LETTERED, others SUCCESS
  ALL_PENDING    = all subscribers PROCESSING
  IN_PROGRESS    = mix of PROCESSING and SUCCESS (no failures yet)
```

## 5. Ingest API

### 5.1 v2 API (new)

```
POST /api/v2/notifications/ingest
Header: X-Publisher-Key: {apiKey}
Content-Type: application/json

{
  "eventId": "biz-unique-id",
  "eventType": "ORDER_CREATED",
  "payload": { ... },
  "traceId": "optional-trace-id",
  "subscriberCode": "ALIYUN"           // optional, targeted delivery
}
```

Changes vs v1:
- `supplierCode` removed as required field, replaced by optional `subscriberCode`
- `X-Publisher-Key` header for publisher authentication
- `tenantCode`, `userId`, `cmd` moved into payload (platform does not interpret business semantics)

### 5.2 v2 Response

```json
{
  "eventId": "biz-unique-id",
  "status": "ACCEPTED",
  "dispatches": [
    { "subscriberCode": "ALIYUN", "status": "QUEUED" },
    { "subscriberCode": "WECHAT", "status": "QUEUED" },
    { "subscriberCode": "DINGTALK", "status": "IDEMPOTENT_HIT" }
  ]
}
```

### 5.3 Fan-out Routing Logic

```
ingestV2(event):
  1. X-Publisher-Key -> validate publisher identity, get publisherCode
  2. Validate eventType is registered, ACTIVE, and belongs to publisherCode
  3. Trigger async field sampling (runtime detection track)

  4. Determine delivery targets:
     if subscriberCode specified:
       targets = [ find Subscription(subscriberCode, eventType) ]
       not found -> REJECTED
     else:
       targets = [ find all ACTIVE Subscriptions for eventType ]
       empty -> ACCEPTED (event received, no subscribers currently)

  5. For each target, generate dispatchId = eventId + ":" + subscriberCode
     Execute existing idempotency check + enqueue logic per target

  6. Return response with per-subscriber dispatch status
```

### 5.4 v1 Compatibility Layer

```
POST /api/v1/notifications/ingest (unchanged contract)

Internal adapter:
  - supplierCode -> subscriberCode targeted delivery
  - No X-Publisher-Key -> skip publisher auth (compatibility period)
  - Response format unchanged (single status, no dispatches array)
```

## 6. Event Change Detection

### 6.1 Dual-Track Detection

**Track 1: Schema Precise Diff** (when payloadSchema exists)
- Publisher updates EventType.payloadSchema
- Platform auto-diffs old vs new schema
- Generates ChangeRecord with confidence=HIGH

**Track 2: Runtime Inference** (when no payloadSchema)
- Ingest entry point samples actual payload
- Compares against known FieldFingerprint records
- Detects structural drift, generates ChangeRecord with confidence=MEDIUM/LOW, status=PENDING_REVIEW

### 6.2 Runtime Sampling Strategy

- Sampling rate: first 100 events per eventType = 100% -> then 1% random
- Detection window: sliding 1-hour window; new field path in window -> trigger ChangeRecord
- Disappearance: field unseen for 3 consecutive windows (3 hours) -> mark DISAPPEARED
- Sampling executes async at Ingest entry point, does NOT block enqueue main path

### 6.3 Impact Analysis

When a ChangeRecord is generated:
- Platform statically analyzes Subscription JSONata templates
- Identifies which subscriptions reference the changed/disappeared field
- Populates affectedSubscriptions in ChangeRecord
- Display warning in admin UI; does NOT auto-interrupt delivery

### 6.4 Admin UI: Event Detail Page

```
Event detail page tabs:
  [Field Structure] - tree view of payload fields with status indicators
  [Change History]  - chronological list of ChangeRecords with detection source
  [Affected Subscriptions] - which subscriptions' templates reference changed fields
```

## 7. Permission Model (Document Only - Not Implemented This Phase)

> NOTE: This section defines the target permission model for documentation purposes.
> Implementation is deferred to a future phase. Current phase uses existing
> session-based admin authentication for all operations.

### 7.1 Roles

| Role | Description |
|------|-------------|
| PLATFORM_ADMIN | Global permissions, manages all entities |
| PUBLISHER | Manages own event types, can manage subscriptions for external subscribers |
| SUBSCRIBER | Manages own subscriptions and DLQ |

### 7.2 Permission Matrix

| Operation | PLATFORM_ADMIN | PUBLISHER | SUBSCRIBER |
|-----------|---------------|-----------|------------|
| Register event type | Yes | Own only | No |
| Update event schema | Yes | Own only | No |
| Deprecate event type | Yes | Own only | No |
| Create subscription | Yes | For external subscribers (managedBy=PUBLISHER) | Own only |
| Configure subscription templates/params | Yes | For external subscribers | Own only |
| Suspend/resume subscription | Yes | No | Own only |
| View change records | Yes | Own events | Affected subscriptions |
| Confirm/dismiss changes | Yes | Own events | No |
| View delivery status | Yes | Own events | Own subscriptions |
| DLQ management | Yes | No | Own only |
| Manage publishers/subscribers | Yes | No | No |

## 8. Worker-Side Changes (Minimal)

```
Current flow:
  Queue message -> configDomainService.getBySupplierCode(supplierCode) -> deliver

New flow:
  Queue message -> read eventTypeCode from message
    -> load Subscription(subscriberCode, eventTypeCode)
    -> merge config (Subscription overrides SubscriberConfig)
    -> deliver

  If message has no eventTypeCode (v1 compat message) -> fall back to current logic
```

Queue model unchanged: per-subscriber physical isolation `queue:notification:{subscriberCode}`.
Fan-out is simply the same event enqueued into different subscriber queues.

## 9. Admin UI Pages

### 9.1 Existing Pages (Modified)

| Page | Change |
|------|--------|
| Supplier List/Form | Rename to "Subscriber Management", fields unchanged |
| DLQ Management | Add subscriberCode filter dimension |
| Simulation | Extend: select eventType -> select subscriber -> simulate delivery |

### 9.2 New Pages

| Page | Description |
|------|-------------|
| Publisher Management | CRUD + API Key generation/rotation |
| Event Type Management | Register/edit/deprecate + Schema editor + field structure tree + change history |
| Subscription Management | Subscriber x EventType bindings + delivery config overrides |
| Event Delivery Tracking | Query by eventId -> aggregate status + per-subscriber detail |

## 10. Migration Strategy

### 10.1 Database

- `supplier_config` table: add alias view or rename to `subscriber_config`
- New tables: `publisher`, `event_type`, `subscription`, `field_fingerprint`, `change_record`
- Existing data preserved; no destructive migration

### 10.2 API

- v1 endpoint unchanged, adapter maps to internal v2 logic
- v2 endpoint added in parallel
- Callers migrate at their own pace

### 10.3 Queue Messages

- v2 messages include `eventTypeCode` field
- v1 messages lack `eventTypeCode`; Worker falls back to SubscriberConfig-only resolution
- No queue format breaking change

## 11. Impact Summary

| Layer | Change Scope |
|-------|-------------|
| Domain | New: Publisher, EventType, Subscription, FieldFingerprint, ChangeRecord; Rename: SupplierConfig -> SubscriberConfig |
| Ingest Entry | New v2 API + fan-out routing + publisher auth; v1 adapter |
| Worker Delivery | Config loading adds Subscription merge; everything else unchanged |
| Runtime Detection | Async sampling at Ingest entry + fingerprint comparison |
| Admin UI | 4 new pages + existing page rename/extension |
| Idempotency | Redis key granularity from eventId to eventId:subscriberCode |
