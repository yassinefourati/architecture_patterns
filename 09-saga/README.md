# 09 — Saga (Orchestration)

Cross-service workflow with compensating actions. The orchestrator drives `payment → inventory → shipping`; on failure it undoes everything that succeeded.

## Structure

```
com.example.saga
├── payment/PaymentService.java          step 1 — charge / refund (compensation)
├── inventory/InventoryService.java      step 2 — reserve / release (compensation)
├── shipping/ShippingService.java        step 3 — schedule / cancel (compensation)
├── orchestrator/
│   ├── OrderSagaContext.java            tracks completed steps
│   └── OrderSagaOrchestrator.java       executes forward; compensates on error
└── order/OrderController.java           HTTP entry point
```

This module uses in-memory services for clarity; in a real system each step would
be its own service (or REST call) wrapped in its own transaction.

## Run

```bash
mvn spring-boot:run
```

Port **8089**. Seeds three products: `WIDGET-1` (10), `WIDGET-2` (5), `WIDGET-3` (0).

## Try it

### Happy path

```bash
curl -X POST http://localhost:8089/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId":"cust-1",
    "productCode":"WIDGET-1",
    "quantity":2,
    "amount":50.00,
    "shippingAddress":"123 Main St"
  }'
```

Logs show: `payment charged → inventory reserved → shipping scheduled`.

### Failure at step 2 (inventory rejects → payment refunded)

```bash
curl -X POST http://localhost:8089/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId":"cust-1",
    "productCode":"WIDGET-3",
    "quantity":1,
    "amount":50.00,
    "shippingAddress":"123 Main St"
  }'
```

Logs show: `payment charged → inventory FAILS → payment refunded` (no shipping step).

### Failure at step 3 (shipping rejects → inventory released, payment refunded)

```bash
curl -X POST http://localhost:8089/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId":"cust-1",
    "productCode":"WIDGET-1",
    "quantity":1,
    "amount":50.00,
    "shippingAddress":"INVALID address"
  }'
```

Logs show: `payment charged → inventory reserved → shipping FAILS → shipping nothing to cancel → inventory released → payment refunded`.

### Failure at step 1 (no compensation needed)

```bash
curl -X POST http://localhost:8089/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId":"cust-1",
    "productCode":"WIDGET-1",
    "quantity":1,
    "amount":99999.00,
    "shippingAddress":"123 Main St"
  }'
```

Payment rejects amounts > $10,000. 

## What to notice

- **Compensation is in reverse order** — `shipping cancel → inventory release → payment refund`. The orchestrator only undoes what actually completed.
- **Each compensation must be idempotent**. If a refund call partially succeeds and is retried, the refund must not double-credit. Here we use `remove()` to enforce single execution.
- **Compensation isn't rollback**. There's no "undo a charge" — there's "issue a refund". The original event remains; the saga adds a corrective event.
- **Failure within compensation** is logged and swallowed in this demo. A production saga records failures to a durable store and surfaces them to operators — a failed compensation often requires manual intervention.
- **No 2PC needed**. Each step is independently atomic in its own service. The saga gives the workflow eventual consistency without a distributed transaction coordinator.
- **Orchestration vs choreography**: this module is orchestrated. For choreography, each service would react to upstream events (Kafka topics) and publish its own outcome events — see module 06 for the in-process equivalent.
