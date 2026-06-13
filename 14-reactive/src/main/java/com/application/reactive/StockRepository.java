package com.application.reactive;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository. Note return types:
 *   Mono<T>  â€” 0 or 1 item
 *   Flux<T>  â€” 0..N items (a stream)
 *
 * Methods don't block â€” they return publishers that emit when data is ready.
 */
public interface StockRepository extends ReactiveCrudRepository<Stock, String> {
    Flux<Stock> findAllByOrderBySymbolAsc();
    Mono<Stock> findBySymbol(String symbol);
}
