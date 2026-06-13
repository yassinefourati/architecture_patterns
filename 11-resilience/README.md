# 11 — Resilience4j (Circuit Breaker / Retry / TimeLimiter)

A resilient client wrapping a deliberately flaky dependency. Watch the circuit breaker open under sustained failure, then recover when the downstream comes back.

## Structure

```
com.example.resilience
├── FlakyDownstream.java     Simulated dependency — 30% transient failures, 10% slow calls, toggleable health
├── ResilientClient.java     @Retry + @CircuitBreaker + @TimeLimiter + fallback
└── ApiController.java       /api/call, /api/cb/state, /admin/downstream/health/{up|down}
```

## Run

```bash
mvn spring-boot:run
```

Port **8087**.

## Try it

### 1. Normal traffic (most succeed, some retry, some fallback)

```bash
for i in $(seq 1 20); do
  curl -s "http://localhost:8087/api/call?input=req-$i"
  echo
done

# Check circuit breaker state
curl -s http://localhost:8087/api/cb/state | jq
```

You'll see a mix of `ok: req-N` and `FALLBACK for req-N`. State should still be `CLOSED` — failure rate is below 50%.

### 2. Force a full outage and watch the circuit open

```bash
# Mark downstream as DOWN
curl -X POST http://localhost:8087/admin/downstream/health/down

# Hammer it
for i in $(seq 1 15); do
  curl -s "http://localhost:8087/api/call?input=outage-$i"
  echo
done

# Now it's OPEN — calls fail fast via the fallback
curl -s http://localhost:8087/api/cb/state | jq
```

`state: "OPEN"` — Resilience4j is now short-circuiting calls. The downstream is given a 10-second break.

### 3. Recover

```bash
# Bring it back
curl -X POST http://localhost:8087/admin/downstream/health/up

# Wait at least 10s for the OPEN→HALF_OPEN transition, then call again
sleep 11
curl -s http://localhost:8087/api/call
curl -s http://localhost:8087/api/cb/state | jq
```

A few successful calls in HALF_OPEN state close the breaker again.

### 4. Observe via actuator

```bash
curl http://localhost:8087/actuator/circuitbreakers | jq
curl http://localhost:8087/actuator/metrics/resilience4j.circuitbreaker.state | jq
curl http://localhost:8087/actuator/health | jq
```

## What to notice

- **Three patterns layered**: `@TimeLimiter` (kill slow calls) → `@CircuitBreaker` (stop calling broken services) → `@Retry` (handle transient blips). They compose; Resilience4j applies them in a specific outer-to-inner order regardless of declaration order, but for clarity the code declares them in the recommended visual order.
- **`CompletableFuture<T>` return type required** for `@TimeLimiter` — the time limiter cancels the future when it exceeds the threshold.
- **Fallback signature**: same return type as the wrapped method, original args plus a `Throwable` at the end. Different fallbacks per exception type are possible — Resilience4j picks the most specific match.
- **Circuit-breaker states**:
  - `CLOSED`: calls flow normally, failures are counted in a sliding window.
  - `OPEN`: all calls short-circuit to the fallback. No load on the broken downstream.
  - `HALF_OPEN`: after `wait-duration-in-open-state`, a few probe calls test the waters. Success → `CLOSED`; failure → `OPEN` again.
- **Slow calls count as failures** when `slow-call-rate-threshold` is configured. A degraded-but-up service trips the breaker just like a hard down one.
- **Metrics for free**: every Resilience4j primitive publishes Micrometer metrics. Pair with Prometheus + Grafana for production observability.
- **`spring-boot-starter-aop`** is needed because Resilience4j Spring annotations work via AOP proxies. Self-invocation within the same class bypasses the proxy and the resilience features — always call through Spring-injected beans.
