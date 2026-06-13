# 02 — Hexagonal Architecture (Ports & Adapters)

The domain sits in the center. **Ports** are interfaces owned by the domain; **adapters** are implementations on the edge.

## Structure

```
com.example.hexagonal
├── domain/                          PURE — no Spring, no JPA
│   ├── model/                       Order, OrderId, OrderLine, OrderStatus
│   └── port/
│       ├── in/                      PlaceOrderUseCase, FindOrderUseCase  (driving)
│       └── out/                     OrderRepositoryPort                  (driven)
├── application/                     Use-case implementations (Spring @Service lives here)
│   ├── PlaceOrderService.java
│   └── FindOrderService.java
└── adapter/
    ├── in/web/                      OrderController → depends on inbound ports
    └── out/persistence/             OrderPersistenceAdapter → implements outbound port
                                      OrderJpaEntity, OrderSpringDataRepository
```

## Run

```bash
mvn spring-boot:run
```

Port **8082**.

## Try it

```bash
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerEmail":"alice@example.com",
    "lines":[{"productCode":"WIDGET-1","quantity":2,"unitPrice":19.99}]
  }'
```

## What to notice

- **`domain/model/Order.java` has zero framework imports.** Compile it without Spring or JPA on the classpath — it still works.
- **Two separate models**: `Order` (domain) and `OrderJpaEntity` (persistence). `OrderPersistenceAdapter` translates between them.
- **The controller depends on `PlaceOrderUseCase`, not `PlaceOrderService`.** You could replace the implementation without touching the controller.
- **Dependency direction**: everything points toward the domain. `application` depends on `domain`; `adapter` depends on `application` + `domain`. The domain depends on nothing.
- To swap JPA for MongoDB, you'd write a new adapter implementing `OrderRepositoryPort`. The domain doesn't change.
