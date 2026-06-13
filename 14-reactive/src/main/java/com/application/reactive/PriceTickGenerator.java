package com.application.reactive;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a market data feed. Every 500ms, picks a random stock and emits a
 * synthetic price change. Real systems would consume from a WebSocket or
 * Kafka topic â€” but the reactive shape (Flux<PriceTick>) is identical.
 *
 * Uses Sinks.Many for multiplexing: many subscribers can independently consume
 * the stream without us caring how many there are.
 */
@Component
public class PriceTickGenerator {

    private final StockRepository repository;
    private final Sinks.Many<PriceTick> sink = Sinks.many().multicast().onBackpressureBuffer();

    private final List<String> symbols = List.of("AAPL", "GOOG", "MSFT", "NVDA", "TSLA");

    public PriceTickGenerator(StockRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void start() {
        // A non-blocking timer. Every tick:
        //   1. Pick a random symbol
        //   2. Load it, mutate its price, save it
        //   3. Emit a PriceTick onto the sink
        Flux.interval(Duration.ofMillis(500))
            .flatMap(t -> {
                String symbol = symbols.get(ThreadLocalRandom.current().nextInt(symbols.size()));
                return repository.findBySymbol(symbol)
                    .flatMap(stock -> {
                        BigDecimal delta = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(-2.0, 2.0))
                            .setScale(2, RoundingMode.HALF_UP);
                        BigDecimal newPrice = stock.getLastPrice().add(delta).max(BigDecimal.ONE);
                        stock.setLastPrice(newPrice);
                        stock.setUpdatedAt(Instant.now());
                        return repository.save(stock);
                    })
                    .map(s -> new PriceTick(s.getSymbol(), s.getLastPrice(), s.getUpdatedAt()));
            })
            .subscribe(sink::tryEmitNext);
    }

    public Flux<PriceTick> stream() {
        return sink.asFlux();
    }

    public record PriceTick(String symbol, BigDecimal price, Instant occurredAt) {}
}
