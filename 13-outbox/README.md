# 13 — Outbox Pattern (transactional messaging)

The classic solution to the **dual-write problem**: how do you reliably update your database AND publish a message about it, without distributed transactions?

**The trick:** instead of two writes (DB + broker), do one (DB only). Write a row to an `outbox` table *in the same transaction* as the business data. A separate relay reads from the outbox and publishes to the broker.

If the transaction commits → outbox row exists → relay will eventually publish.
If the transaction rolls back → no outbox row → nothing was supposed to be published anyway.

## Structure

```
com.example.outbox
├── order/
│   ├── OrderController.java       /api/orders
│   ├── OrderService.java          @Transactional save + outbox enqueue
│   └── AdminController.java       /admin/status, /admin/broker/*
├── outbox/                         PRODUCER SIDE
│   ├── OutboxMessage.java         JPA entity
│   ├── OutboxPublisher.java       Enqueue in CURRENT transaction (Propagation.MANDATORY)
│   ├── OutboxRepository.java      Locks pending rows for relay
│   └── OutboxRelay.java           @Scheduled poller → broker
└── inbox/                          CONSUMER SIDE
    ├── MockBroker.java            Simulates Kafka, with random failures + duplicates
    ├── InboxRecord.java           Idempotency table
    ├── InboxRepository.java
    └── IdempotentOrderConsumer.java   Try INSERT inbox → if dup-key, skip
```

## Run

```bash
mvn spring-boot:run
```

Port **8094**.

## Try it

### 1. Place an order — happy path

```bash
curl -X POST http://localhost:8094/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerEmail":"alice@example.com","amount":99.99,"failAfterOutbox":false}'

# Watch the logs: order persisted → outbox row → relay polls → broker → consumer
# Some random broker failures will retry. Some duplicate deliveries will be skipped.

# After a moment, inspect:
curl http://localhost:8094/admin/status | jq
```

You'll see `outboxTotal` going up and `consumerProcessed` matching, with `consumerDuplicatesSkipped` proving idempotency is working.

### 2. Atomic rollback — the killer demo

```bash
curl -X POST http://localhost:8094/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerEmail":"bob@example.com","amount":50.00,"failAfterOutbox":true}'
```

Returns HTTP 500. **Critical observation**: even though the outbox row was inserted, the transaction rolled back. The order does **not** exist, the outbox row does **not** exist, and the consumer never sees a phantom event. Check the status — `outboxTotal` did not change from before.

This is the dual-write problem solved. With a naive approach (DB save + `kafkaTemplate.send()` in the same method), the Kafka publish could succeed while the DB rolled back, leaving downstream services thinking an order exists that doesn't.

### 3. Stress the consumer's idempotency

```bash
# Turn the broker's duplicate-delivery up to 100% (every message delivered twice)
# It's already on by default; this is just to confirm.
curl -X POST http://localhost:8094/admin/broker/at-least-once/on

# Place 5 orders
for i in 1 2 3 4 5; do
  curl -s -X POST http://localhost:8094/api/orders \
    -H "Content-Type: application/json" \
    -d "{\"customerEmail\":\"x$i@example.com\",\"amount\":10.0,\"failAfterOutbox\":false}" > /dev/null
done

sleep 2
curl http://localhost:8094/admin/status | jq
```

`consumerDuplicatesSkipped` will be roughly equal to `consumerProcessed` — every message arrived twice, but the inbox table stopped the duplicates from being processed twice.

## What to notice

- **`Propagation.MANDATORY`** on `OutboxPublisher.enqueue()` — refuses to run unless already in a transaction. This is intentional: the whole point is atomicity with the business write.
- **The relay marks `PROCESSED` after publishing.** If the JVM crashes between publish and status update, the message is republished on restart. That's why consumers MUST be idempotent — see the inbox table for one way.
- **The inbox pattern**: try `INSERT` first, catch the duplicate-key error, treat that as "already processed." Both the inbox insert and the side effect must commit together; otherwise a crash between them leaves you stuck.
- **`@Lock(PESSIMISTIC_WRITE)`** on the outbox query prevents two relay instances from picking the same row. In Postgres, use `SELECT ... FOR UPDATE SKIP LOCKED` for high-throughput concurrent relays.
- **Polling has cost.** This demo uses a 500ms poll. In Postgres, **Debezium** reads the WAL via logical replication and pushes outbox inserts to Kafka without any polling — same algorithm, far better throughput.
- **The outbox table grows forever** unless you have a cleanup job. Periodically delete PROCESSED rows older than your retention SLA (e.g., 7 days), keep FAILED ones for forensics.

## When to use it

**Use when:**
- You need reliable event publishing alongside a database write
- You can't afford to lose events (financial, audit, integration with other services)
- The broker is Kafka, RabbitMQ, AWS SNS, etc. — anything that's a separate system from your DB

**Don't use when:**
- The "event" is just an in-process notification — use `ApplicationEventPublisher` (see module 06)
- You can tolerate occasional missed events and have reconciliation jobs
- Your DB and broker are the same system (e.g., Postgres LISTEN/NOTIFY, where the notify is transactional)
