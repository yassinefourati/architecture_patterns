package com.application.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Wraps the flaky downstream with:
 *  - Retry: 3 attempts with exponential backoff (handles transient failures)
 *  - TimeLimiter: 1s per attempt (kills slow calls)
 *  - CircuitBreaker: opens after sustained failure rate (stops cascading)
 *  - Fallback: degraded response when all else fails
 *
 * Annotation order matters in Resilience4j: the OUTER annotation wraps first.
 * Recommended outer-to-inner: Retry â†’ CircuitBreaker â†’ TimeLimiter â†’ Bulkhead.
 * Spring resolves these by AOP, so we declare them in that order on the method.
 */
@Service
public class ResilientClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientClient.class);

    private final FlakyDownstream downstream;

    public ResilientClient(FlakyDownstream downstream) {
        this.downstream = downstream;
    }

    @Retry(name = "downstream")
    @CircuitBreaker(name = "downstream", fallbackMethod = "fallback")
    @TimeLimiter(name = "downstream")
    public CompletableFuture<String> callDownstream(String input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downstream.call(input);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        });
    }

    /** Fallback signature: same return type, original args + Throwable last. */
    @SuppressWarnings("unused")
    private CompletableFuture<String> fallback(String input, Throwable t) {
        log.warn("[resilient] fallback engaged for input='{}' cause={}", input, t.toString());
        return CompletableFuture.completedFuture("FALLBACK for " + input);
    }
}
