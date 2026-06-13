# 15 — Multi-Tenancy (DataSource-per-tenant routing)

Serve many customer organizations from one application, each with **isolated data**. This module uses **DataSource-per-tenant** — separate connection pools to separate databases (or schemas), chosen per-request based on a header.

## The three strategies

| Strategy | Isolation | Cost | When |
|---|---|---|---|
| **Shared schema** (`tenant_id` column on every row) | Lowest — bugs leak data | One pool | Many small tenants, tight DB budget |
| **Schema-per-tenant** (one schema, separate tables) | Medium — Postgres `search_path` | One pool, schema switch per query | Mid-size SaaS, Postgres |
| **Database-per-tenant** (separate DB / DataSource) | Strongest | N pools, complex | High-isolation requirements (compliance), small-to-medium tenant count |

This demo implements the **third** since it makes the routing mechanism most visible. Switching to schema-per-tenant changes only the `buildTenantDataSource` URL pattern (and adds `SET search_path` on connection acquisition).

## Structure

```
com.example.tenancy
├── tenant/
│   ├── TenantContext.java              ThreadLocal<String> — current tenant
│   ├── TenantFilter.java               Reads X-Tenant-Id, sets context, clears on exit
│   ├── TenantRoutingDataSource.java    extends AbstractRoutingDataSource
│   └── TenantDataSourceConfig.java     Builds DataSource-per-tenant, wraps in router
└── business/                            FULLY TENANT-UNAWARE
    ├── Customer.java                   No tenant_id column
    ├── CustomerRepository.java          No tenant filters
    └── CustomerController.java         Plain CRUD
```

## Run

```bash
mvn spring-boot:run
```

Port **8096**. Two tenants seeded: `acme` and `globex`.

## Try it

```bash
# Missing tenant — 400
curl -i http://localhost:8096/api/customers

# List ACME's customers (seed row only)
curl -H "X-Tenant-Id: acme" http://localhost:8096/api/customers | jq

# Add a customer to ACME
curl -X POST http://localhost:8096/api/customers \
  -H "X-Tenant-Id: acme" \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@acme.example.com"}'

# Add a different customer to GLOBEX
curl -X POST http://localhost:8096/api/customers \
  -H "X-Tenant-Id: globex" \
  -H "Content-Type: application/json" \
  -d '{"name":"Bob","email":"bob@globex.example.com"}'

# Each tenant sees only its own data — even though it's the same application
curl -H "X-Tenant-Id: acme" http://localhost:8096/api/customers | jq
curl -H "X-Tenant-Id: globex" http://localhost:8096/api/customers | jq

# Unknown tenant — 400
curl -H "X-Tenant-Id: unknown-co" http://localhost:8096/api/customers
```

## What to notice

- **`AbstractRoutingDataSource`** is the magic. Its `determineCurrentLookupKey()` is called on EVERY connection request — Spring uses that key to pick which physical DataSource to delegate to. Read [its Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/lookup/AbstractRoutingDataSource.html); it's only ~100 lines of source.
- **`TenantContext` is a ThreadLocal**, which is fine for blocking Spring MVC. With virtual threads it still works (each request gets its own VT, each VT has its own ThreadLocal). With reactive code or `@Async`, you must propagate the tenant explicitly — Spring's `ContextSnapshot` / `MicrometerContextPropagation` libraries automate this.
- **The business layer is tenant-agnostic.** That's the goal. No developer needs to remember `WHERE tenant_id = ?` on every query; the routing happens at the connection layer.
- **`TenantFilter.clear()` in `finally`** — non-negotiable. ThreadLocal leaks between requests if the same servlet thread is reused without clearing. The leak shows up as one tenant seeing another's data — a security incident.
- **Hibernate doesn't need to know** it's multi-tenant in this approach. The alternative (`hibernate.multiTenancy=SCHEMA`) ties you to Hibernate's specific strategies. `AbstractRoutingDataSource` is framework-neutral — works for JdbcTemplate, MyBatis, anything.
- **Tenant onboarding** in production: a separate admin endpoint creates the tenant's DB/schema, runs Flyway migrations, registers the new DataSource in the router via `setTargetDataSources()` followed by `afterPropertiesSet()`. Don't bake tenant config into application.yml — it has to be hot-reloadable.

## When to use which strategy

**Shared schema (`tenant_id` column):**
- 10,000+ tenants where each is small
- Reporting/analytics across tenants is common
- You're confident your code never forgets the filter (Hibernate filters or row-level security helps)

**Schema-per-tenant (one DB, many schemas):**
- Tens to low thousands of tenants
- Postgres or Oracle (good schema isolation)
- Migrations need to apply per-tenant; tooling exists for this

**Database-per-tenant (this module):**
- Strong isolation required (regulated industries, enterprise customers)
- Tenant count bounded (dozens or low hundreds — connection pools add up)
- Each tenant might need a different backup/restore SLA, different DB size, different region

## Gotchas

- **Connection pool sizing**: N tenants × pool size. With 50 tenants × 10 connections, you're at 500 idle pooled connections. Use dynamic pool sizing or share pools.
- **Background jobs need a tenant**: `@Scheduled` methods run with no tenant in context. You must iterate tenants explicitly and set context for each.
- **Tests need a tenant too**: integration tests must set `TenantContext` (or send the header) or they'll hit the default DataSource.
- **Liquibase/Flyway need configuration**: run migrations against each tenant DataSource at provisioning time, not via Spring's auto-migration on startup.
