# Advanced Architecture & Design Patterns — Reference Guide

A practitioner's reference covering patterns beyond the 15 in the working project. Organized by category, with when-to-use guidance, key Spring Boot tooling, and the trade-offs that actually matter.

## Table of Contents

1. [Data & Persistence Patterns](#1-data--persistence-patterns)
2. [Distributed Systems Patterns](#2-distributed-systems-patterns)
3. [Messaging Patterns](#3-messaging-patterns)
4. [API & Integration Patterns](#4-api--integration-patterns)
5. [Performance & Scalability Patterns](#5-performance--scalability-patterns)
6. [Security Patterns](#6-security-patterns)
7. [Observability Patterns](#7-observability-patterns)
8. [Deployment & Operational Patterns](#8-deployment--operational-patterns)
9. [Testing Patterns](#9-testing-patterns)
10. [Anti-Patterns to Avoid](#10-anti-patterns-to-avoid)
11. [Pattern Selection Heuristics](#11-pattern-selection-heuristics)

---

## 1. Data & Persistence Patterns

### Repository Pattern
Already covered in modules 02-04. The repository interface lives in the domain; the implementation lives at the edge. Spring Data JPA gives you this for free but tempts you to skip the domain interface — resist when the domain is non-trivial.

### Unit of Work
Spring's `@Transactional` IS the Unit of Work pattern. One transaction tracks all changes; commit applies them atomically. Don't fight it with manual `flush()` calls unless you have a specific reason.

### Specification Pattern
For dynamic queries without exploding repository methods.

```java
public interface CustomerSpecs {
    static Specification<Customer> hasEmail(String email) {
        return (root, query, cb) -> cb.equal(root.get("email"), email);
    }
    static Specification<Customer> activeAfter(Instant date) {
        return (root, query, cb) -> cb.greaterThan(root.get("lastSeen"), date);
    }
}

// Compose at the call site
List<Customer> result = customerRepository.findAll(
    hasEmail("alice@example.com").and(activeAfter(lastMonth))
);
```

Use when: dynamic filtering (search pages, admin tools). Avoid when: a handful of fixed queries — just write methods.

### Identity Map
Hibernate's first-level cache implements this — within a session, the same DB row maps to the same Java object. Subtle: detached entities lose this guarantee. `@Transactional` boundaries scope the identity map.

### Lazy Loading
Built into JPA. **The #1 source of bugs in JPA apps**:
- `LazyInitializationException` outside the transaction
- N+1 query problems (load 100 orders, then 100 separate queries for items)

Fixes:
- Set `spring.jpa.open-in-view=false` to fail loudly instead of silently triggering lazy loads
- Use `JOIN FETCH` or `@EntityGraph` for queries that need related data
- Prefer DTO projections (`@Query` returning a record) for read-only flows

### Soft Delete
Mark rows as deleted instead of removing them.

```java
@SQLDelete(sql = "UPDATE orders SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")  // Hibernate 6 — older: @Where
@Entity
public class Order { ... }
```

Use when: audit/compliance, undo, references must survive. Beware: unique constraints become tricky, queries get slower as deleted rows accumulate. Archive aggressively.

### Audit Columns (created_at, updated_at, created_by, updated_by)
Spring Data JPA Auditing handles this declaratively.

```java
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class Auditable {
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
}
```

Plus `@EnableJpaAuditing` and an `AuditorAware<String>` bean that returns the current user (from `SecurityContextHolder`).

### Optimistic Locking
`@Version` field on the entity. JPA adds `WHERE version = ?` to every UPDATE; mismatch throws `OptimisticLockException`.

```java
@Version private Long version;
```

Use this everywhere by default. The cost is one column; the benefit is detecting lost updates that would otherwise corrupt data silently.

### Pessimistic Locking
`SELECT ... FOR UPDATE` via `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Use sparingly — it serializes work and can deadlock. Better alternatives are usually optimistic locking or queue-based ordering.

### Polyglot Persistence
Different data stores for different access patterns:
- Postgres for transactional core
- Elasticsearch for search
- Redis for sessions/caching
- S3 for blobs
- ClickHouse/BigQuery for analytics

The pattern is real; the cost is real (operations, consistency, code complexity). Default to one store; add another only when the SQL or scale becomes painful.

### Change Data Capture (CDC)
Read database WAL/binlog with **Debezium** to stream changes into Kafka. Enables outbox-without-an-outbox-table, replication to analytics warehouses, and cache invalidation.

Trade-off: tight coupling to DB schema. A column rename can break every downstream consumer.

### Database-Per-Service vs Shared Database
The microservices canon says database-per-service. Reality:
- Two services sharing one database is fine if one owns the schema and the other reads via views.
- Two services that both write to the same tables is a distributed monolith pretending to be microservices.

### Materialized Views
Precomputed query results stored as tables. Postgres has them natively. Refresh on a schedule or via triggers. Massively simpler than CQRS for read-side optimization when staleness is acceptable.

---

## 2. Distributed Systems Patterns

### Choreography vs Orchestration (covered partly in module 09)

| | Choreography | Orchestration |
|---|---|---|
| Coordination | Each service reacts to events | Central coordinator calls each step |
| Coupling | Loose | Coordinator knows all steps |
| Debugging | Hard — emergent behavior | Easy — one place to look |
| Adding steps | Add a new subscriber | Modify the coordinator |
| Best for | Independent business events | Multi-step workflows |

### Two-Phase Commit (2PC)
The textbook distributed transaction. **Avoid in modern systems.** Atomikos/Bitronix exist; they're slow, fragile, and don't survive cloud-native deployment models. Use the Saga pattern (module 09) instead.

### Eventual Consistency
You will see stale data. Design for it:
- Show "pending" UI states while writes propagate
- Read-your-writes consistency for the user who just wrote (route their reads to the primary or recent cache)
- Bounded staleness SLAs ("inventory reflects within 5 seconds")

### CAP Theorem in Practice
You don't trade C, A, P uniformly. You trade them **per operation**:
- Checkout: prefer Consistency over Availability (don't oversell)
- Product browsing: prefer Availability over Consistency (it's OK to show a slightly stale catalog)

PACELC is the more useful framing: in the absence of partitions, do you favor latency or consistency?

### Strangler Fig Pattern
Migrate from a legacy monolith by routing one capability at a time through a new service via the gateway. The legacy app shrinks gradually. Originated by Martin Fowler.

Critical: write characterization tests against the legacy behavior BEFORE you start strangling. Otherwise you'll preserve bugs as features.

### Anti-Corruption Layer (ACL)
A translation layer between your clean domain and a messy external system. The ACL absorbs the external system's quirks so they don't leak inward.

Implement as: a dedicated module/package, with its own DTOs matching the external API, and a translator that converts to/from your domain model.

### Bulkhead Pattern
Isolate resources so one failure doesn't drain the others. Examples:
- Separate thread pools per downstream service
- Separate DB connection pools per workload (read vs write, transactional vs reporting)
- Separate K8s deployments per tenant tier

Resilience4j has `@Bulkhead` for thread isolation:
```java
@Bulkhead(name = "payment", type = Bulkhead.Type.THREADPOOL)
public CompletableFuture<X> call() { ... }
```

### Fail Fast vs Fail Silent
For request-driven flows: fail fast with clear errors. Retries belong at boundaries you control.
For event-driven flows: fail to a dead-letter queue with full context. Never silently drop.

### Backpressure
The consumer signals to the producer that it can't keep up. Reactive Streams (Project Reactor, RxJava) make this first-class. Kafka uses consumer lag as implicit backpressure.

Without backpressure: producers overwhelm consumers, queues grow unbounded, OOM kills the consumer.

### Idempotency Keys
For any operation that retries (HTTP retries, message redelivery, user double-click):
- Client generates a unique key per logical operation
- Server records (key, result) — second request with the same key returns the cached result without re-executing

Header convention: `Idempotency-Key`. Stripe popularized this for payment APIs. Now standard for any non-trivial write API.

### Distributed Locks
Sometimes unavoidable. Options:
- **Redis (Redlock)** — fast, fragile under failure
- **Zookeeper / etcd** — slow, correct
- **Database row lock** — slow, correct, free

The right answer is usually: avoid distributed locks by partitioning work so each unit has a single owner.

### Compensation vs Retry
Not the same:
- **Retry**: try again because the call might have failed transiently
- **Compensation**: undo because the call definitively succeeded but business state changed

A retry must be idempotent. A compensation must run only after confirming the original action happened.

### Read-Your-Writes Consistency
After a user writes, their reads must see their own write. With CQRS or replica reads, this is hard:
- Route the user's reads to the primary for N seconds after their write
- Tag the user's session with a "minimum lsn" and only serve from replicas that have caught up

---

## 3. Messaging Patterns

### Competing Consumers
N consumers on the same queue/topic share the load. Kafka: each consumer in a consumer group gets a subset of partitions. Default for most messaging.

### Publish-Subscribe
N independent consumers each get every message. Kafka: each consumer group reads the full stream.

### Dead Letter Queue (DLQ)
Messages that fail processing after N retries go to a DLQ for manual inspection. Without one, poison messages either block the queue (in-order systems) or get silently lost.

Spring Kafka:
```java
@Bean
DefaultErrorHandler errorHandler(KafkaTemplate<?, ?> template) {
    return new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(template),
        new FixedBackOff(1000L, 3)
    );
}
```

### Message Ordering
Kafka guarantees ordering within a partition, not across partitions. Choose the partition key as the entity that needs ordered events (e.g., `orderId`). All events for one order go to one partition, processed by one consumer in order.

Don't use random keys if order matters. Don't expect global ordering — it's a distributed system.

### Transactional Messaging (covered in module 13)
Use the outbox pattern. Almost always the right answer over JTA/2PC.

### Saga (covered in module 09)
The distributed workflow pattern with compensating actions.

### Event Carried State Transfer
The event payload carries everything the consumer needs (denormalized). Consumer doesn't have to call back to fetch more.

Trade-off: bigger events vs lower coupling. Usually worth it for cross-team services.

### Event Notification (thin events)
Event carries only the entity ID; consumers fetch what they need.

Trade-off: simpler events, but consumer must reach back to the source service — couples consumer to source's availability.

### Claim Check
For large messages: store the payload in object storage, send a tiny "claim check" event with just the URL. Consumers fetch on demand.

### Request-Reply over Messaging
Use a `correlation-id` to match replies to requests. Spring AMQP and Spring Kafka support this. Beware: degrades to synchronous coupling — usually a sign you should use HTTP instead.

### Schema Registry & Schema Evolution
Confluent Schema Registry + Avro/Protobuf for Kafka. Enforces:
- Producers register schemas before publishing
- Consumers cache schemas by ID
- Compatibility rules prevent breaking changes (BACKWARD, FORWARD, FULL)

Without it: every team invents their own JSON shape, breaking changes ship silently, debugging is impossible.

---

## 4. API & Integration Patterns

### REST Maturity (Richardson Model)
- Level 0: HTTP as RPC tunnel (one endpoint, POST everything)
- Level 1: Resources (different URLs for different things)
- Level 2: HTTP verbs (GET/POST/PUT/DELETE used semantically) ← most apps stop here
- Level 3: HATEOAS (responses include links to next actions)

Level 2 is almost always the right target. HATEOAS sounds good, is rarely used in practice.

### GraphQL
Single endpoint, client specifies the shape it wants. Spring for GraphQL is mature.

**Where it shines:** mobile clients with limited bandwidth, complex object graphs, multiple frontend teams sharing a backend.

**Where it hurts:** N+1 queries (need DataLoader), security (queries can be expensive), caching (no HTTP cache hierarchy).

### gRPC
Protobuf + HTTP/2. Strongly typed, code-generated, bidirectional streaming.

**Use for:** service-to-service communication where you control both ends.
**Avoid for:** external APIs (browsers can't speak gRPC directly without grpc-web).

### API Versioning
Strategies:
- **URI versioning**: `/v1/orders`, `/v2/orders` — simple, ugly
- **Header versioning**: `Accept: application/vnd.api+json; version=2` — clean, harder for clients
- **No versioning, only additive changes** — preferred; only break when truly necessary, then issue v2 alongside v1

### Backend for Frontend (BFF)
A dedicated API per frontend (web, iOS, Android). Each BFF aggregates/transforms data for its specific client. Avoids one-size-fits-all APIs that please nobody.

### API Composition / Aggregator
A service that calls multiple downstreams and merges results. Common at the gateway layer. Watch for: response time = max(downstream), error semantics get fuzzy (one downstream failed, others succeeded — partial response?).

### Pagination
- **Offset/limit**: simple, breaks under concurrent writes (you'll skip or duplicate items)
- **Cursor (keyset)**: pass `?after=<last_id>` — stable, scales
- **Token-based**: opaque continuation token — best for clients (treat it as opaque)

Cursor pagination is the default for any list that might grow.

### Rate Limiting Strategies
- **Token bucket**: allows bursts, smooths over time — most user-facing APIs
- **Fixed window**: simple, has thundering-herd at window boundaries
- **Sliding window log**: most accurate, expensive
- **Concurrency limit**: cap N in-flight requests — pairs well with bulkheads

Implement at the gateway, not in each service. Bucket4j is the canonical Java library.

### Hypermedia Controls
Even without full HATEOAS, returning related URLs in responses helps clients evolve.
```json
{ "id": 123, "status": "PLACED",
  "_links": { "cancel": "/api/orders/123/cancel", "items": "/api/orders/123/items" }}
```

---

## 5. Performance & Scalability Patterns

### Cache-Aside (Lazy Loading)
App checks cache, misses, loads from DB, populates cache.
```java
@Cacheable("products")
public Product findById(Long id) { return repository.findById(id).orElseThrow(); }

@CacheEvict(value = "products", key = "#product.id")
public void update(Product product) { repository.save(product); }
```

Spring Cache abstraction works with Caffeine (local), Redis, Hazelcast. Caffeine for single-instance, Redis for cluster.

### Write-Through / Write-Behind
- **Write-through**: write to cache + DB synchronously. Slower writes, faster reads, never stale.
- **Write-behind**: write to cache, flush to DB async. Risk of data loss on crash.

Most apps don't need either — cache-aside is enough.

### Refresh-Ahead
Cache proactively refreshes before TTL. Caffeine has this built-in (`refreshAfterWrite`). Avoids the "stampede" when a hot key expires.

### Bloom Filter for Negative Caching
Before hitting DB, check a Bloom filter to see if the key COULD exist. Eliminates DB load for known-missing keys (e.g., fraudster IPs).

### CQRS Read Models (covered in module 05)
Specialized read models per use case.

### Read Replicas
DB-level scaling. Spring's `AbstractRoutingDataSource` (also used in module 15 for tenancy) can route reads to replicas:
```java
@Transactional(readOnly = true)
public List<Order> recentOrders() { ... }   // routes to replica

@Transactional
public Order place(...) { ... }              // routes to primary
```

### Sharding
Split data across N databases by some key (user_id, geography, hash). Application or proxy layer routes by shard key.

Painful: cross-shard queries, rebalancing, hot shards. Don't shard until you've exhausted vertical scaling and read replicas.

### Connection Pool Sizing
Default Hikari `maximum-pool-size=10` is rarely right. The formula:
```
connections = ((core_count * 2) + effective_spindle_count)
```
For a 4-core SSD database: ~10 connections per app instance. For 4 instances: 40 connections total. **More connections almost always make things slower** above a threshold.

### Database Indexing
- B-tree for equality and range
- Hash for equality only (rare)
- GIN/GiST for full-text, JSON, geospatial
- Composite indexes: order matters; left-most prefix rule

Every query in your hot path needs an index. Every additional index slows writes. Audit with `EXPLAIN ANALYZE` quarterly.

### Async Processing
Push slow work off the request thread.
```java
@Async("taskExecutor")
public CompletableFuture<Report> generate(Long userId) { ... }
```
Plus `@EnableAsync` and a properly-sized `ThreadPoolTaskExecutor` bean.

### Virtual Threads (Java 21)
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
Each request runs on a virtual thread — cheap to create, parked during I/O without blocking a platform thread. For most apps this is now the right alternative to WebFlux.

### Lazy Initialization
```yaml
spring:
  main:
    lazy-initialization: true
```
Beans created on first use, not at startup. Cuts startup time dramatically; first request to each endpoint is slower. Worth it for serverless/scale-to-zero. Avoid for always-on production.

### GraalVM Native Image
AOT-compile the Spring app into a native binary. Startup in milliseconds, lower memory, but: reflection requires hints, build is slow, runtime profiling impossible. Use for serverless functions and CLI tools. Less compelling for long-running services.

### CDN & Edge Caching
For static assets and cacheable APIs. Cache-Control headers are the primary mechanism:
```
Cache-Control: public, max-age=3600, stale-while-revalidate=600
```
ETags for conditional GETs (`304 Not Modified` is the cheapest possible response).

---

## 6. Security Patterns

### OAuth 2.0 / OIDC Resource Server
The default auth for service APIs. Spring Security:
```java
http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
    jwt.jwkSetUri("https://auth.example.com/.well-known/jwks.json")
));
```
JWTs validated against the issuer's public keys. No DB lookup per request.

### Token Patterns
- **Access token**: short-lived (15min), bearer auth on every request
- **Refresh token**: long-lived (days/weeks), used only to mint new access tokens; stored in HTTP-only cookie
- **ID token (OIDC)**: contains user identity claims; not for authorization

### Authorization Strategies
- **RBAC**: roles → permissions. Simple, scales poorly with edge cases.
- **ABAC**: attributes (user, resource, environment) → policy. Flexible, harder to audit.
- **ReBAC**: relationships (user X owns resource Y). Best for sharing/collaboration domains (think Google Docs). See: Google Zanzibar, OpenFGA.

Spring Security's `@PreAuthorize("hasPermission(#order, 'write')")` supports custom permission evaluators for any of these.

### CORS
Browser-enforced. Configure once at the gateway, not in every service.
```java
@Bean
CorsConfigurationSource corsConfig() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("https://*.example.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowCredentials(true);
    return source(config);
}
```
**Never** `setAllowedOrigins(List.of("*"))` with credentials — it's a footgun.

### CSRF
Stateful (cookie-based) apps need CSRF tokens. Stateless APIs with bearer tokens don't.

```java
// API: disable CSRF, require bearer auth
http.csrf(CsrfConfigurer::disable)
    .oauth2ResourceServer(...);
```

### Defense in Depth
- **Input validation** at the API boundary (Bean Validation)
- **Output encoding** when rendering HTML (Thymeleaf does it by default)
- **Parameterized queries** always (JPA does this; raw JDBC is where SQLi happens)
- **Principle of least privilege** for DB users, IAM roles, network ACLs
- **Secrets in a vault**, not in config files (HashiCorp Vault, AWS Secrets Manager)

### Rate Limiting & Account Lockout
Brute-force protection for login endpoints. Per-IP + per-account counters with exponential backoff.

### Mass Assignment Prevention
Never bind request bodies directly to JPA entities. Use DTOs. A malicious client otherwise can set fields you didn't intend to expose.
```java
// BAD: exposes every field of User as bindable
@PostMapping("/users")
User create(@RequestBody User user) { ... }

// GOOD: explicit allow-list via DTO
@PostMapping("/users")
User create(@RequestBody CreateUserRequest request) { ... }
```

### Secrets Rotation
Production secrets (DB passwords, API keys) must be rotatable WITHOUT downtime. Spring Cloud Config + bus refresh, or Vault dynamic secrets with auto-rotation.

### Audit Logging
Separate stream from operational logs. Append-only, retained per compliance SLA. Captures: who, what, when, before/after for sensitive ops.

### Encryption at Rest & in Transit
- TLS 1.3 everywhere — gateway terminates external TLS, mTLS between internal services in zero-trust networks
- Postgres column-level encryption for PII
- KMS-managed keys; never key material in code or config

---

## 7. Observability Patterns

### Three Pillars
- **Metrics** — Micrometer → Prometheus → Grafana
- **Logs** — Logback JSON → Loki/Elastic → Grafana/Kibana
- **Traces** — Micrometer Tracing → OpenTelemetry → Tempo/Jaeger

Modern fourth pillar: **profiles** (continuous profiling via Pyroscope/Parca).

### Structured Logging
Plain text logs are uninterpretable at scale. JSON logs with consistent fields:
```json
{"timestamp":"...","level":"INFO","logger":"...","msg":"Order placed",
 "orderId":"...","customerId":"...","traceId":"..."}
```
Logstash encoder for Logback. Add `traceId` and `spanId` automatically via Micrometer Tracing.

### Correlation IDs
A request gets an ID at the edge. Every log line, every downstream call, every event carries it. Without this, debugging distributed flows is impossible.

```java
@Component
public class CorrelationIdFilter implements Filter {
    public void doFilter(...) {
        String id = req.getHeader("X-Correlation-Id");
        if (id == null) id = UUID.randomUUID().toString();
        MDC.put("correlationId", id);
        try { chain.doFilter(req, res); }
        finally { MDC.clear(); }
    }
}
```

### RED Method (for request-driven services)
- **Rate**: requests per second
- **Errors**: error percentage
- **Duration**: latency percentiles (p50, p95, p99)

These three metrics per endpoint reveal almost every operational problem.

### USE Method (for resources)
- **Utilization**: % busy
- **Saturation**: queue depth
- **Errors**: errors

For CPU, memory, disk, network, DB connections.

### Golden Signals (Google SRE)
Latency + Traffic + Errors + Saturation. Strictly superior to either RED or USE alone.

### SLI / SLO / SLA
- **SLI**: indicator (the metric — e.g., success rate)
- **SLO**: objective (the target — e.g., 99.9%)
- **SLA**: agreement (the contract — what you owe customers when you miss SLO)

Error budgets fall out of SLOs: 99.9% allows 43 minutes of downtime per month. Spend it on feature velocity; if you blow the budget, slow down.

### Tracing Span Naming
- HTTP server: `<METHOD> <route>` (e.g., `GET /api/orders/{id}`)
- HTTP client: `<METHOD>` only by default; add target host
- DB: `<operation> <table>` (e.g., `SELECT orders`)

Cardinality matters: don't put user IDs in span names. They go in attributes.

### Health Checks
- **Liveness**: am I alive? (if no, kill me) — usually `/actuator/health/liveness`
- **Readiness**: can I serve traffic? (if no, stop routing to me) — `/actuator/health/readiness`

Readiness must check DB, downstream services, queues. Liveness must NOT — otherwise a DB outage cascades into pods being killed.

### Distributed Tracing Sampling
100% sampling is expensive. Common strategies:
- **Head-based**: decide at request entry (e.g., 1% of all requests)
- **Tail-based**: collect everything, decide after the trace completes (keep errors, p99 latency, etc.)

Tail-based is more useful but requires a collector that can buffer (OpenTelemetry Collector + tail-sampling processor).

---

## 8. Deployment & Operational Patterns

### Twelve-Factor App
Still the baseline checklist after 13 years:
1. Codebase in version control
2. Dependencies explicit
3. Config in environment
4. Backing services as resources
5. Strict separation of build/release/run
6. Stateless processes
7. Port binding
8. Concurrency via process model
9. Disposability (fast startup, graceful shutdown)
10. Dev/prod parity
11. Logs as event streams
12. Admin tasks as one-off processes

Spring Boot's defaults align with most of this.

### Blue/Green Deployment
Two production environments. Deploy to the idle one (green), test, flip the load balancer. Instant rollback.

### Canary Deployment
Route a small % of traffic to the new version. Watch metrics. Gradually increase. Requires good observability.

### Rolling Deployment
Replace instances one at a time. Default in Kubernetes. Both versions run simultaneously — your code must handle this (forward/backward compatible APIs and schemas).

### Feature Flags
Decouple deploy from release. Code ships behind a flag, enabled for some users, ramped to everyone. Tools: LaunchDarkly, Unleash, in-house. Spring integration via `@ConditionalOnProperty` (static) or runtime flag checks.

Critical for: testing in production, gradual rollouts, killswitches.

### Graceful Shutdown
```yaml
server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 30s
```
Stops accepting new requests, lets in-flight requests complete, then shuts down. Without this, Kubernetes pod terminations drop requests.

### Database Migrations
**Flyway** or **Liquibase**, run on app startup or as a separate job.
- Migrations are append-only — never edit a checked-in migration
- Backwards-compatible migrations only (add column nullable, deploy code, populate, then make NOT NULL in next migration)
- Never `DROP TABLE` in the same deploy as the code that stopped using it

### Configuration Management
- **Profiles** for environment-specific config (`application-prod.yml`)
- **Externalized config** for secrets and per-instance settings (env vars, K8s ConfigMaps, Vault)
- **Spring Cloud Config** for centralized config across many services
- **Property precedence**: command-line > env > profile > application.yml

### Tenant-Aware Deployment Tiers
SaaS apps often serve free, pro, and enterprise tiers. Deploy them to separate K8s namespaces with different resource quotas, so a free-tier surge doesn't degrade enterprise SLAs. Bulkhead at the infrastructure level.

### Chaos Engineering
Inject failures in production deliberately:
- **Pod kills** (Chaos Monkey)
- **Network latency/loss** (Pumba, Toxiproxy)
- **Resource pressure** (kube-monkey, Litmus)

Don't start until you have decent observability. Otherwise you're just creating outages.

### Backpressure at Deployment
Load shedding when overloaded: respond `503 Retry-After` instead of accepting requests you can't fulfill. Better than the alternative (silent timeout cascades).

### Zero-Downtime Schema Changes
The "expand and contract" pattern:
1. **Expand**: add new column/table, dual-write old + new
2. **Migrate**: backfill old data into new structure
3. **Switch reads** to new structure
4. **Contract**: stop writing old, drop it

Never compress into one deploy. Stretched across N deploys is the only safe path.

---

## 9. Testing Patterns

### Test Pyramid
- Many unit tests (fast, isolated)
- Some integration tests (per slice — `@WebMvcTest`, `@DataJpaTest`)
- Few end-to-end tests (full stack, slow)

If it's an ice cream cone (many E2E, few unit) you'll suffer.

### Test Doubles
- **Stub**: returns canned data
- **Mock**: verifies interactions
- **Fake**: working but simplified implementation (in-memory repo)
- **Spy**: wraps a real object

Mocks are over-used. Prefer fakes when possible — they test behavior, not call patterns.

### Spring Boot Slices
- `@WebMvcTest` — controller + Spring MVC layer only
- `@DataJpaTest` — JPA + in-memory DB
- `@JsonTest` — Jackson serialization
- `@RestClientTest` — `@RestClient`, `RestTemplate` clients

Each slice loads a minimal subset of the context — much faster than `@SpringBootTest`.

### Testcontainers
Real Postgres/Kafka/Redis in Docker for integration tests. Vastly more accurate than H2 + embedded brokers.
```java
@Testcontainers
@SpringBootTest
class OrderIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
    }
}
```

### Contract Testing
Two services agree on a contract; both teams test against it.
- **Spring Cloud Contract** — producer writes contracts, consumer tests use generated stubs
- **Pact** — consumer-driven contracts, producer verifies

Use when teams ship independently. Skip when one team owns both sides.

### Property-Based Testing
**jqwik** for Java. Instead of `assertEquals(42, fn(5))`, write `forAll integers, fn is associative`. The framework generates 1000 random inputs.

Excellent for: serialization round-trips, math, parsers, anything with invariants.

### Mutation Testing
**Pitest** mutates your code (negates conditions, changes operators) and runs tests. If tests still pass, your tests aren't actually testing the code. Brutal feedback; usually reveals dozens of "tested" lines that aren't.

### Architecture Testing
**ArchUnit** for enforcing architecture rules in tests:
```java
@Test
void controllersShouldNotAccessRepositoriesDirectly() {
    classes().that().resideInAPackage("..controller..")
        .should().notAccessClassesThat().resideInAPackage("..repository..")
        .check(JavaClasses.from("com.example"));
}
```

### Smoke Tests After Deploy
Minimal tests that hit production after a deploy: can I log in? can I check out? Run on a schedule, alert on failure.

### Load Testing
- **k6** (JavaScript) — modern, scriptable
- **Gatling** (Scala) — high-throughput, detailed reports
- **JMeter** — heavy, GUI-driven

Always test the realistic shape: 80% reads, 20% writes, real cardinality. Synthetic 100-RPS-of-the-same-request tells you nothing useful.

---

## 10. Anti-Patterns to Avoid

### Distributed Monolith
Multiple services that must deploy together. Worst of both worlds.
Symptoms: shared database, synchronous chains across services, can't release a service independently.

### Anemic Domain Model
Entities with only getters/setters; all logic in services. The domain becomes a data structure, not a model.
Fix: push behavior into the domain (DDD module 04).

### God Service
One service does too much. Symptom: changes to unrelated features touch the same service.
Fix: split along bounded contexts.

### Shared Mutable State Across Services
Database schemas, message contracts, files in S3 — anything two teams write to simultaneously becomes a coordination nightmare. One owner, others read.

### Premature Microservices
Splitting before boundaries are stable. You'll spend a year refactoring service boundaries instead of building features.
Default: modular monolith (module 08), extract services when forced.

### Service-to-Service Synchronous Chains
A → B → C → D, each waiting on the next. Latency multiplies. Failure cascades. Reliability is the product of components (99.9% × 4 = 99.6%).
Fix: async messaging, parallelize calls, return partial results.

### Treating Cache as Source of Truth
"It's in Redis so it must be right." Caches are best-effort. Always have a path to rebuild from the source of truth.

### One Repository Per Entity
Symptom: 50 repositories that each do CRUD on one table. Usually means your aggregates are too small.
Fix: repository per aggregate root (module 04).

### EAV (Entity-Attribute-Value) Tables
"Customers have arbitrary fields, so let's have a (customer_id, key, value) table." Always slower than expected, queries become impossible, types decay to strings.
Fix: actual columns, or a JSON column for genuinely variable data.

### The "Smart UI" + Dumb Backend
Business logic in the frontend. Different clients implement the same logic differently. Mobile lags web. Validation is bypassable.
Fix: backend owns the rules; clients render.

### Logging-as-Architecture
"We don't need events, we'll just log." Logs aren't queryable, ordered, durable, or replayable. Use events for events.

### Custom Frameworks Built on Spring
Inevitably outgrown. New hires can't onboard. Documentation rots. Use Spring's own primitives or a well-known library.

### `@Transactional(propagation = REQUIRES_NEW)` Sprinkled Around
Usually a code smell. Suggests fighting Spring's transaction model. Real use cases exist (audit logging, outbox writes) but are rare.

### Catching Exception and Logging
```java
try { ... } catch (Exception e) { log.error("oops", e); }
```
Now the caller has no idea anything happened. Either handle it (recovery path) or let it propagate.

### Optional Parameters via Method Overloads
6 overloads of `findByX(...)` with different combinations. Use Specification pattern or a query object.

### Hibernate Open Session in View
`spring.jpa.open-in-view=true` (the default!). Holds a DB connection for the entire HTTP response. Hides N+1 queries. Set it to `false` and fix the bugs it reveals.

---

## 11. Pattern Selection Heuristics

### Start simple. Default choices:
- **Layered monolith** (module 01) until you have evidence you need more structure
- **One database** until you have evidence you need polyglot persistence
- **Synchronous REST** until you have evidence you need events
- **Spring MVC + virtual threads** until you have evidence you need reactive
- **One service** until you have evidence you need many

### Real evidence looks like:
- Two teams stepping on each other in the same codebase → modular monolith
- One module deploys 10x more often than others → extract it
- Read traffic 100x write traffic → CQRS or read replicas
- Sustained tail latency from a slow dependency → circuit breaker
- "What was the state on Jan 5?" is a real question → event sourcing
- Customer data must be physically isolated → tenant-per-DB

### False evidence:
- "We might need it someday" — you'll build the wrong abstraction
- "It's web scale" — until you have actual scale problems, it's premature
- "Netflix does it" — Netflix has problems you don't have
- "The architect said so" — get specific about which problem you're solving

### Costs that don't show up in tutorials:
- **Operational**: monitoring, on-call, runbooks per pattern
- **Cognitive**: every new pattern is something everyone must learn
- **Coordination**: cross-cutting changes get harder with more boundaries
- **Recruitment**: harder to hire for exotic stacks

### When in doubt:
1. Pick the simplest pattern that solves the actual problem in front of you.
2. Make boundaries clear so you can replace it later.
3. Write down WHY you chose this pattern (ADR — Architecture Decision Record).
4. Revisit the choice when you have new evidence.

---

## Further Reading

**Books worth your time (no particular order):**
- *Designing Data-Intensive Applications* — Kleppmann. The definitive treatment of distributed data systems.
- *Domain-Driven Design* — Evans. Foundational, dense.
- *Implementing Domain-Driven Design* — Vernon. More tactical than Evans.
- *Patterns of Enterprise Application Architecture* — Fowler. Classic; some patterns dated, vocabulary still essential.
- *Release It!* — Nygard. Stability patterns, anti-patterns, war stories.
- *Building Microservices* — Newman. Honest about trade-offs.
- *Building Event-Driven Microservices* — Bellemare. Best practical treatment of event-driven systems.
- *Software Engineering at Google* — covers many of the operational patterns above.

**Online:**
- [martinfowler.com](https://martinfowler.com) — patterns, microservices, refactoring
- [microservices.io](https://microservices.io) — Chris Richardson's pattern catalog
- [microsoft.com/azure/architecture/patterns](https://learn.microsoft.com/azure/architecture/patterns) — vendor-neutral pattern reference
- [Spring documentation](https://docs.spring.io) — the actual primary source

**For Spring specifically:**
- Read the source code of the modules you use. Spring Framework, Boot, Data, Cloud — all well-written and readable.
- Andy Wilkinson, Phil Webb, Jürgen Höller talks from SpringOne — implementation rationale.
- Josh Long's videos for working knowledge of new features.

---

## Final Note

Patterns are tools. The job of an architect is choosing which tools to NOT use. A codebase with too few patterns is rigid and verbose. A codebase with too many is fragmented and incomprehensible. Aim for the smallest number that solves your actual problems, applied consistently, with the rationale written down.

The 15 working modules in this project show how the most common patterns look in code. This document is the map of where you might go next, when the actual problem demands it.
