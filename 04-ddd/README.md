# 04 — Domain-Driven Design (Tactical Patterns)

A rich Order aggregate that enforces its own invariants and emits domain events.

## Structure

```
com.example.ddd.order/
├── domain/                 Aggregate, entities, value objects, events, repo interface
│   ├── Order.java          AGGREGATE ROOT — sole entry point
│   ├── OrderLine.java      Entity inside the aggregate
│   ├── Money, OrderId, ProductId, CustomerId   Value objects (records)
│   ├── DomainEvent, OrderPlaced, OrderCancelled   Sealed event hierarchy
│   ├── OrderStatus         State enum
│   └── OrderRepository     Interface owned by the domain
├── application/            OrderApplicationService, OrderPlacedHandler
├── infrastructure/         JpaOrderRepository, OrderJpaEntity (persistence model)
└── web/                    OrderController + DTOs
```

## Run

```bash
mvn spring-boot:run
```

Port **8084**.

## Try it

```bash
# 1. Open a draft
DRAFT=$(curl -s -X POST http://localhost:8084/api/orders/drafts \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-001"}' | jq -r .id)

# 2. Add lines
curl -X POST http://localhost:8084/api/orders/$DRAFT/lines \
  -H "Content-Type: application/json" \
  -d '{"productId":"PROD-A","quantity":2,"unitPrice":15.00}'

curl -X POST http://localhost:8084/api/orders/$DRAFT/lines \
  -H "Content-Type: application/json" \
  -d '{"productId":"PROD-B","quantity":1,"unitPrice":30.00}'

# 3. Place it — watch logs for the OrderPlaced event handler
curl -X POST http://localhost:8084/api/orders/$DRAFT/place

# 4. Try to add a line after placing — 422
curl -X POST http://localhost:8084/api/orders/$DRAFT/lines \
  -H "Content-Type: application/json" \
  -d '{"productId":"PROD-C","quantity":1,"unitPrice":10.00}'
```

## What to notice

- **The aggregate enforces its own rules**: try modifying a placed order, cancelling a shipped one, placing an empty draft. The exceptions come from the domain, not from a service.
- **`Order.pullEvents()` pattern**: the aggregate accumulates events; the application service publishes them after persistence. This decouples domain logic from infrastructure.
- **`@TransactionalEventListener(AFTER_COMMIT)`** in `OrderPlacedHandler` ensures listeners never see events for rolled-back transactions.
- **Separate models**: `Order` (domain) vs `OrderJpaEntity` (persistence). `JpaOrderRepository` translates between them. The domain doesn't know JPA exists.
- **Value objects are records**: `Money`, `OrderId`, `ProductId`, `CustomerId` — immutable, equality by value, validated on construction.
- **Sealed event hierarchy**: `DomainEvent permits OrderPlaced, OrderCancelled` lets the compiler verify exhaustive handling.
