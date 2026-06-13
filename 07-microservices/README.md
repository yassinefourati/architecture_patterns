# 07 — Microservices

Two independently deployable services. `order-service` calls `inventory-service` over HTTP via OpenFeign.

## Structure

```
07-microservices/
├── inventory-service/    Port 8091 — owns inventory data
│   └── GET /api/inventory/{productCode}
└── order-service/        Port 8092 — owns order data, calls inventory-service
    └── POST /api/orders   (checks stock via Feign, then persists order)
```

Each service has its **own** H2 database. Neither can read the other's tables — only its public REST API.

## Run

Open two terminals.

**Terminal 1 (inventory):**
```bash
cd inventory-service
mvn spring-boot:run
```

**Terminal 2 (order):**
```bash
cd order-service
mvn spring-boot:run
```

## Try it

Inventory seeds three products: `WIDGET-1` (10), `WIDGET-2` (5), `WIDGET-3` (0).

```bash
# Check inventory directly
curl http://localhost:8091/api/inventory/WIDGET-1

# Place an order — order-service calls inventory-service under the hood
curl -X POST http://localhost:8092/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productCode":"WIDGET-1","quantity":2,"customerEmail":"alice@example.com"}'

# Out of stock — 422
curl -X POST http://localhost:8092/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productCode":"WIDGET-3","quantity":1,"customerEmail":"bob@example.com"}'
```

Stop `inventory-service` and retry the order — observe the failure mode. **Module 11 (resilience)** shows how to wrap this call in a circuit breaker so failures don't cascade.

## What to notice

- **Two separate Spring Boot apps**: each has its own `pom.xml`, main class, and database. They can deploy, scale, and fail independently.
- **`@FeignClient`** generates an HTTP client from an interface. The order service treats `InventoryClient` like a local bean.
- **URL-based discovery** here for simplicity. In production: Eureka, Consul, or Kubernetes DNS (`url` omitted, name resolved by service registry).
- **The order service crashes without resilience** when inventory is down. That's the failure mode module 11 fixes.
- **Data ownership**: order-service knows the `productCode` only as an opaque string. It doesn't have a foreign key to inventory tables — that would be a shared database, the canonical microservices anti-pattern.
