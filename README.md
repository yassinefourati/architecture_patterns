# Spring Boot Architecture Patterns — Working Examples

A multi-module Maven project where each module is a self-contained, runnable Spring Boot application demonstrating one architecture pattern. Each module has its own README explaining the pattern, the structure, and how to run it.

## Modules

| # | Module | Pattern | Port | Domain |
|---|---|---|---|---|
| 01 | [`01-layered`](./01-layered) | Layered (N-Tier) | 8081 | Product catalog CRUD |
| 02 | [`02-hexagonal`](./02-hexagonal) | Hexagonal (Ports & Adapters) | 8082 | Order placement with pure-Java domain |
| 03 | [`03-clean-architecture`](./03-clean-architecture) | Clean Architecture | 8083 | Bank account transfers |
| 04 | [`04-ddd`](./04-ddd) | Domain-Driven Design | 8084 | Order aggregate with events |
| 05 | [`05-cqrs`](./05-cqrs) | CQRS | 8085 | Separate read/write models, in-process projector |
| 06 | [`06-event-driven`](./06-event-driven) | Event-Driven (in-process) | 8086 | Order → Inventory → Notification |
| 07 | [`07-microservices`](./07-microservices) | Microservices | 8091, 8092 | Two services + Feign client |
| 08 | [`08-modular-monolith`](./08-modular-monolith) | Modular Monolith (Spring Modulith) | 8088 | Order + Inventory modules with enforced boundaries |
| 09 | [`09-saga`](./09-saga) | Saga (Orchestration) | 8089 | Order → Payment → Inventory → Shipping with compensation |
| 10 | [`10-api-gateway`](./10-api-gateway) | API Gateway (Spring Cloud Gateway) | 8090 | Reverse proxy in front of module 07 |
| 11 | [`11-resilience`](./11-resilience) | Circuit Breaker / Retry / TimeLimiter | 8087 | Resilience4j with a flaky dependency |
| 12 | [`12-event-sourcing`](./12-event-sourcing) | Event Sourcing | 8093 | Account aggregate, hand-rolled event store + projection |
| 13 | [`13-outbox`](./13-outbox) | Outbox + Inbox (transactional messaging) | 8094 | Atomic DB + event publish, idempotent consumer |
| 14 | [`14-reactive`](./14-reactive) | Reactive (WebFlux + R2DBC) | 8095 | Stock prices, SSE streaming, reactive composition |
| 15 | [`15-multi-tenancy`](./15-multi-tenancy) | Multi-tenancy (DataSource-per-tenant) | 8096 | `AbstractRoutingDataSource`, tenant-agnostic business code |

## Prerequisites

- **Java 21**
- **Maven 3.9+**

## Running a Single Module

```bash
cd 02-hexagonal
mvn spring-boot:run
```

Each module uses H2 in-memory storage (or in-memory mocks), so no external infrastructure is required to demo any pattern.

## Building Everything

```bash
mvn -DskipTests install
```

## How to Read This Project

Suggested order:

1. **`01-layered`** — the baseline most developers already know.
2. **`02-hexagonal`** and **`04-ddd`** — see how the same domain looks when boundaries are enforced. The pure-Java domain in `02` is the key contrast with `01`.
3. **`03-clean-architecture`** — same idea, different naming convention. Useful for comparing the two schools of thought.
4. **`05-cqrs`** and **`06-event-driven`** — separation of concerns at the application level. Both use Spring's `@TransactionalEventListener(AFTER_COMMIT)` to safely propagate side effects.
5. **`08-modular-monolith`** — the pragmatic mid-point between monolith and microservices. Run `mvn test` to see boundary verification.
6. **`07-microservices`**, **`09-saga`**, **`10-api-gateway`**, **`11-resilience`** — the distributed-systems patterns. They build on each other: microservices introduce the failure modes that saga and resilience address; the gateway sits in front of them all.
7. **Advanced (12–15)** — production patterns for specific situations:
   - **`12-event-sourcing`** — when the audit trail is the product (finance, healthcare, contracts)
   - **`13-outbox`** — when you need reliable event publishing alongside DB writes (any service-to-service integration over a broker)
   - **`14-reactive`** — when you need very high concurrency or streaming (SSE, WebSockets, real-time push)
   - **`15-multi-tenancy`** — when one app serves many isolated customers (SaaS)

Each README ends with a "What to notice" section pointing at the specific files and classes that make the pattern visible.

## Pattern Selection Cheat Sheet

| Situation | Start with |
|---|---|
| Small CRUD app, small team | Layered (01) |
| Complex domain, long-lived app | Hexagonal + DDD (02 + 04) |
| Want microservice benefits without ops cost | Modular Monolith (08) |
| Reads and writes diverge sharply | CQRS (05) |
| Multiple services need to stay consistent | Saga (09) |
| Decouple producers from consumers | Event-driven (06) |
| Truly independent teams and scaling needs | Microservices (07) — usually after extracting from 08 |
| Single ingress, cross-cutting concerns | API Gateway (10) |
| Calling unreliable dependencies | Resilience4j (11) |
| Need full audit trail / time-travel queries | Event Sourcing (12) |
| Reliable event publishing alongside DB writes | Outbox (13) |
| High concurrency / streaming / SSE | Reactive (14) |
| SaaS with isolated customer data | Multi-tenancy (15) |

**Pragmatic path:** start with **01** or **08**, apply **04 (DDD)** inside, layer in **06 (events)** for decoupling, and only adopt **07 (microservices)** + **09 (saga)** + **10 (gateway)** + **11 (resilience)** when you have concrete evidence (team friction, scaling needs, deployment coupling) that the distributed cost will pay off.

## Notes on the Code

These modules are written for clarity over completeness — they're scaffolds you can extend, not production-ready services. In particular:

- **No tests** beyond the module-boundary verification in **08**. Add `@SpringBootTest`, `@WebMvcTest`, and Testcontainers integration tests before deploying anything that looks like this.
- **No auth** anywhere. Add Spring Security + OAuth2 resource server for any real workload.
- **No observability** beyond Actuator. Add Micrometer Tracing + OpenTelemetry for production.
- **In-memory storage**: H2 or `ConcurrentHashMap`. Real systems need Postgres/MySQL/etc., and **module 06**'s event handling needs an outbox table or a real broker (Kafka/RabbitMQ) for durability.

## Beyond These 15 Modules

See [`ADVANCED_PATTERNS.md`](./ADVANCED_PATTERNS.md) for a comprehensive reference covering ~100 additional architecture and design patterns: data, distributed systems, messaging, API design, performance, security, observability, deployment, testing, anti-patterns, and pattern-selection heuristics. The 15 working modules show how the most common patterns look in code; that document is the map of where to go next when the problem demands it.
