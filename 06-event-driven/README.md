# 06 — Event-Driven Architecture (in-process)

Modules communicate via events. Publisher knows nothing about subscribers.

## Flow

```
POST /api/orders
  ↓
OrderService.place() ─── publishes ──→ OrderPlaced
                                          ↓ (AFTER_COMMIT)
                          ┌───────────────┴───────────────┐
                          ↓                               ↓
              InventoryEventListener           NotificationService
                          ↓                       (sends "received" email)
              reserve stock, then publish
                          ↓
              InventoryReserved  ──or──  InventoryRejected
                          ↓                               ↓
              OrderService updates status          OrderService updates status
                          ↓                               ↓
              NotificationService                 NotificationService
              ("confirmed" email)                 ("rejected" email)
```

## Structure

```
com.example.events
├── shared/Events.java            Event contract used by all modules
├── order/                        Producer + consumer of downstream events
├── inventory/                    Consumes OrderPlaced, publishes Reserved/Rejected
└── notification/                 Async consumer (multiple events)
```

## Run

```bash
mvn spring-boot:run
```

Port **8086**. Seeds three products on startup:
- `WIDGET-1` (10 in stock)
- `WIDGET-2` (5 in stock)
- `WIDGET-3` (0 in stock — will be rejected)

## Try it

```bash
# Successful order — gets CONFIRMED
curl -X POST http://localhost:8086/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerEmail":"alice@example.com","items":[{"productCode":"WIDGET-1","quantity":2}]}'

# Watch the logs to see: order publishes → inventory reserves → notifications fire
# Then GET to see status moved to CONFIRMED:
curl http://localhost:8086/api/orders/<id-from-above>

# Out-of-stock order — gets REJECTED
curl -X POST http://localhost:8086/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerEmail":"bob@example.com","items":[{"productCode":"WIDGET-3","quantity":1}]}'
```

## What to notice

- **`@TransactionalEventListener(AFTER_COMMIT)`** — listeners never see events from rolled-back transactions. Standard `@EventListener` fires *inside* the publisher's transaction, which couples them.
- **`@Async`** on the notification listener — runs on a separate thread pool. Slow email delivery doesn't block the request response.
- **Inventory both consumes and publishes** — a real saga in miniature (see module 09 for the orchestrated version).
- **No module imports another module's classes** — only `shared/Events.java`. This is the same boundary discipline microservices enforce, applied within a single process.
- **Limit of in-process events**: if the JVM crashes between publishing and the listener running, the event is lost. For durability, see the **outbox pattern** discussion at the bottom of the main `spring-boot-architecture-patterns.md`.
