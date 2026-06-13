# 10 — API Gateway (Spring Cloud Gateway)

A single entry point that routes requests to backend services and applies cross-cutting filters (auth, rate limiting, correlation IDs, path rewriting, circuit breakers).

## Structure

This module is mostly **configuration** — the value of Spring Cloud Gateway is in `application.yml`, not in Java code.

```
com.example.gateway
└── GatewayApplication.java        Empty @SpringBootApplication
src/main/resources/application.yml Route definitions and filters
```

## Run

This gateway routes to the two services from **module 07 (microservices)**. Start those first:

**Terminal 1 (inventory):**
```bash
cd ../07-microservices/inventory-service && mvn spring-boot:run
```

**Terminal 2 (order):**
```bash
cd ../07-microservices/order-service && mvn spring-boot:run
```

**Terminal 3 (gateway):**
```bash
cd 10-api-gateway && mvn spring-boot:run
```

Gateway listens on **port 8090**.

## Try it

Now everything goes through the gateway — clients never hit the services directly:

```bash
# Routed to inventory-service (8091)
curl http://localhost:8090/api/inventory/WIDGET-1

# Routed to order-service (8092)
curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productCode":"WIDGET-1","quantity":2,"customerEmail":"alice@example.com"}'

# Path-rewrite route: /api/inventory/v1/WIDGET-1 → /api/inventory/WIDGET-1
curl http://localhost:8090/api/inventory/v1/WIDGET-1

# External proxy with prefix-strip
curl http://localhost:8090/echo/get
```

## Inspect routes at runtime

The actuator endpoint shows the live routing table:

```bash
curl http://localhost:8090/actuator/gateway/routes | jq
```

## What to notice

- **Reactive (WebFlux)**: Gateway uses Netty + WebFlux. **Don't** add `spring-boot-starter-web` — it conflicts with WebFlux. The POM intentionally excludes it.
- **All filters declarative**: `AddRequestHeader`, `RewritePath`, `StripPrefix`, `RequestRateLimiter`, `CircuitBreaker` — Gateway ships with ~30 built-in filter factories. Custom filters extend `GatewayFilter`/`GlobalFilter`.
- **Headers propagate**: downstream services receive `X-Gateway` and `X-Correlation-Id` headers — useful for tracing and audit.
- **No load balancing here**: routes use `http://localhost:...` directly. With Spring Cloud LoadBalancer + service discovery, you'd use `uri: lb://inventory-service` and the gateway resolves the URI via Eureka/Consul/K8s.
- **What's missing from this demo**: auth (typically OAuth2 resource server here), rate limiting (needs Redis), and circuit breaker (needs Resilience4j — see module 11).
- **The gateway is your security boundary**: TLS termination, JWT validation, IP allow-lists, and rate limits all belong here, not in every service.
