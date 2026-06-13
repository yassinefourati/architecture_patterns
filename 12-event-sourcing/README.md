# 12 — Event Sourcing

The aggregate's state is **derived** by replaying an append-only log of events. The events ARE the source of truth; the read model is just a cached projection.

Hand-rolled (no Axon, no EventStoreDB) — the algorithm is more important than the framework. Adding Axon or Marten later is mechanical once you understand this.

## Structure

```
com.example.es
├── domain/
│   ├── AccountEvent.java        Sealed event hierarchy (4 events)
│   └── Account.java             Aggregate — folds events into state; commands raise new events
├── eventstore/
│   ├── EventStore.java          Interface (append, loadStream, loadAfter)
│   ├── JdbcEventStore.java      JDBC implementation with optimistic concurrency via UNIQUE(aggregate_id, version)
│   └── AccountRepository.java   Loads via replay, saves uncommitted events
├── projection/
│   └── AccountSummaryProjection.java   Polls event store, builds the read model
└── web/AccountController.java   Commands (POST) + queries (GET from projection)
```

## Run

```bash
mvn spring-boot:run
```

Port **8093**.

## Try it

```bash
# Open an account
ID=$(curl -s -X POST http://localhost:8093/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"holder":"Alice","openingBalance":100.00}' | jq -r .id)
echo "Account: $ID"

# Run some operations
curl -X POST http://localhost:8093/api/accounts/$ID/deposits \
  -H "Content-Type: application/json" -d '{"amount":50.00}'

curl -X POST http://localhost:8093/api/accounts/$ID/withdrawals \
  -H "Content-Type: application/json" -d '{"amount":30.00}'

curl -X POST http://localhost:8093/api/accounts/$ID/deposits \
  -H "Content-Type: application/json" -d '{"amount":25.00}'

# Wait a moment for the projection to catch up
sleep 1

# Current state — from the projection
curl -s http://localhost:8093/api/accounts/$ID | jq

# Full history — replay every event that built that state
curl -s http://localhost:8093/api/accounts/$ID/history | jq

# Try to overdraw — 422, invariant enforced on the rehydrated aggregate
curl -X POST http://localhost:8093/api/accounts/$ID/withdrawals \
  -H "Content-Type: application/json" -d '{"amount":99999.00}'
```

## What to notice

- **The event store is the database.** `event_store` is append-only. The `account_summary` table is regenerable from events — you could drop it and rebuild from scratch.
- **Two-phase aggregate**: decision functions (`deposit`, `withdraw`) produce events but route through `apply()` to mutate state. This means rehydration uses the SAME `apply()` method — no duplicated state logic.
- **Optimistic concurrency**: `UNIQUE(aggregate_id, aggregate_version)` on the event store. Two concurrent writers both trying to append version 5 to the same aggregate — one wins, the other gets `ConcurrencyException` → HTTP 409. The losing client reloads and retries.
- **Eventually consistent reads**: the projection polls every 200ms. Write a command, then read — you may briefly see stale data. The history endpoint reads from the event store directly and is always current.
- **Time travel**: pass any version cutoff into `loadStream` and you reconstruct the aggregate's state at that point. Free auditability, free debugging.
- **Schema evolution**: new event types are added; old events stay valid forever. You **never migrate event data** — you write upcasters that translate old shapes into new ones at load time.
- **Snapshots** (not shown here): for aggregates with thousands of events, periodically save `Account` state to a snapshot table and replay only events newer than the snapshot. Same pattern, optimization for replay cost.

## When to use it

**Use event sourcing when:**
- Audit trail is a hard business requirement (finance, healthcare, legal)
- You need to ask "what was the state at time T?" frequently
- The domain has rich behavior worth recording (orders, contracts, workflows)

**Don't use it when:**
- Plain CRUD is enough — the operational burden isn't worth it
- The team is unfamiliar with eventual consistency
- Most reads need real-time accuracy (projections lag)
