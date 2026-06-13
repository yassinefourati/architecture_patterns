package com.application.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class ApiController {

    private final ResilientClient resilientClient;
    private final FlakyDownstream downstream;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ApiController(ResilientClient resilientClient, FlakyDownstream downstream,
                         CircuitBreakerRegistry circuitBreakerRegistry) {
        this.resilientClient = resilientClient;
        this.downstream = downstream;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /** Make a resilient call. Returns either real data or "FALLBACK for {input}". */
    @GetMapping("/api/call")
    public CompletableFuture<String> call(@RequestParam(defaultValue = "ping") String input) {
        return resilientClient.callDownstream(input);
    }

    /** Inspect circuit-breaker state: CLOSED, OPEN, HALF_OPEN. */
    @GetMapping("/api/cb/state")
    public Map<String, Object> cbState() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("downstream");
        return Map.of(
            "state", cb.getState(),
            "failureRate", cb.getMetrics().getFailureRate(),
            "slowCallRate", cb.getMetrics().getSlowCallRate(),
            "successfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls(),
            "failedCalls", cb.getMetrics().getNumberOfFailedCalls(),
            "slowCalls", cb.getMetrics().getNumberOfSlowCalls()
        );
    }

    /** Toggle the simulated downstream up/down. */
    @PostMapping("/admin/downstream/health/{state}")
    public Map<String, Boolean> setHealth(@PathVariable String state) {
        boolean up = "up".equalsIgnoreCase(state);
        downstream.setHealthy(up);
        return Map.of("healthy", downstream.isHealthy());
    }
}
