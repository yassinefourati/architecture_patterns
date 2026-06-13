# 14 — Reactive (WebFlux + R2DBC)

A non-blocking stack from HTTP all the way down to the database. Built on Project Reactor (`Mono` / `Flux`) and Netty.

**The big idea:** instead of one thread per request waiting for I/O, a small pool of event-loop threads handles thousands of concurrent requests by suspending and resuming when data is ready. Same hardware, far higher concurrency — *if* the entire chain is non-blocking.

## Structure

```
com.example.reactive
├── Stock.java                     R2DBC entity (NOT JPA)
├── StockRepository.java           Returns Mono<Stock> / Flux<Stock>
├── StockController.java           REST + Server-Sent Events streaming
├── PriceTickGenerator.java        Sinks.Many — multicasts price ticks to all subscribers
└── PortfolioController.java       Reactive fan-out with Flux.flatMap
```

## Run

```bash
mvn spring-boot:run
```

Port **8095**.

## Try it

### Standard request/response

```bash
curl http://localhost:8095/api/stocks
curl http://localhost:8095/api/stocks/AAPL
```

Looks identical to a blocking controller — the reactive shape is internal.

### The interesting part: SSE streaming

```bash
# Stream every price tick as it happens
curl -N http://localhost:8095/api/stocks/ticks

# Filter to one symbol
curl -N "http://localhost:8095/api/stocks/ticks?symbol=NVDA"
```

`-N` disables curl's buffering. You'll see new events every ~500ms as the generator publishes ticks. The connection stays open indefinitely. **No thread is held while waiting between ticks** — that's the point.

Open the SSE stream in 50 terminal windows simultaneously. Memory and CPU usage barely change. With a blocking stack, you'd need 50 threads parked on the response — at ~1MB stack each, that's 50MB just for idle waiters.

### Reactive composition

```bash
curl "http://localhost:8095/api/portfolio?symbols=AAPL,GOOG,MSFT,NVDA"
```

All four DB lookups happen concurrently on the event loop — not serially, and without thread-per-query. With `Mono.zip` or `Flux.flatMap`, fan-out is one line of code.

## What to notice

- **No `spring-boot-starter-web`** — adding it would pull in Tomcat and conflict with WebFlux's Netty. The POM excludes it intentionally.
- **R2DBC ≠ JPA.** No Hibernate, no lazy loading, no `EntityManager`. Spring Data Relational provides repositories that return `Mono`/`Flux` instead of `Optional`/`List`. Mapping is via `@Table` and `@Column` from `org.springframework.data.relational.core.mapping`.
- **Mono vs Flux**: `Mono<T>` emits at most one value (single result, void), `Flux<T>` emits 0..N values (collections, streams).
- **`Sinks.Many.multicast().onBackpressureBuffer()`** — broadcasts to all subscribers; if a slow subscriber falls behind, items buffer rather than block the publisher.
- **The "reactive contract" must hold end-to-end**. ONE blocking call (`Thread.sleep`, JDBC, `restTemplate.exchange`) anywhere in a reactive chain starves the event loop and ruins the whole point. WebClient (not RestTemplate), R2DBC (not JDBC), reactive Kafka clients, etc.
- **`Schedulers.boundedElastic()`** is the escape hatch — wrap unavoidable blocking calls in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` to confine them to a separate thread pool.

## When to use reactive

**Use when:**
- You need very high concurrency on limited threads (10k+ open connections — chat servers, IoT gateways, streaming APIs)
- Long-lived streams (SSE, WebSockets, server push)
- Compute-light apps dominated by I/O fan-out across multiple downstream services

**Don't use when:**
- The team isn't fluent in `Mono`/`Flux`. The mental model is a real ramp; stack traces are harder to read; debuggers don't naturally step through reactive chains.
- Your workload is CPU-bound — reactive doesn't help; it just makes the code harder.
- You depend on libraries that don't have reactive equivalents (some JDBC drivers, certain auth/session libraries).
- Java 21 virtual threads are an option. Spring MVC on virtual threads gives you most of the concurrency benefit with **none** of the cognitive overhead. For many apps that's now the right answer.

## Reactive vs Virtual Threads (Java 21)

| Concern | WebFlux/Reactor | Spring MVC + virtual threads |
|---|---|---|
| Concurrency | Excellent (event loop) | Excellent (1 VT per request, cheap) |
| Code style | `Mono`/`Flux` operators | Imperative blocking |
| Debugging | Hard — async stack traces | Easy — synchronous traces |
| Existing libraries | Need reactive variants | Most blocking libs work |
| Backpressure | First-class, built in | Not natively expressed |

For most new apps in 2025+, virtual threads remove the main reason to choose reactive. Reactive still wins for **streaming** (SSE, WebSockets, real-time push) and **explicit backpressure**.
