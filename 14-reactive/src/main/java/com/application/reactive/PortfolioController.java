package com.application.reactive;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates how reactive composition pays off when fanning out to multiple
 * downstream services. We make N HTTP requests CONCURRENTLY without spawning
 * N threads â€” Mono.zip waits for all of them to complete and emits the combined result.
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final WebClient webClient;
    private final StockRepository repository;

    public PortfolioController(StockRepository repository, WebClient.Builder webClientBuilder) {
        this.repository = repository;
        // Calls back into ourselves for the demo
        this.webClient = webClientBuilder.baseUrl("http://localhost:8095").build();
    }

    /**
     * Fetch several stocks in parallel and aggregate.
     * Each downstream call is non-blocking; the gateway thread is free between them.
     */
    @GetMapping
    public Mono<PortfolioResponse> portfolio(@RequestParam List<String> symbols) {
        // Trigger all DB lookups in parallel â€” they all share the R2DBC connection pool
        // and dispatch onto the event loop, NOT a thread per request.
        return reactor.core.publisher.Flux.fromIterable(symbols)
            .flatMap(s -> repository.findBySymbol(s.toUpperCase())
                .map(stock -> Map.entry(stock.getSymbol(), stock.getLastPrice())))
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .map(prices -> new PortfolioResponse(prices, prices.values().stream()
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)))
            .timeout(Duration.ofSeconds(2));   // entire chain fails fast if it stalls
    }

    public record PortfolioResponse(Map<String, java.math.BigDecimal> prices, java.math.BigDecimal total) {}
}
