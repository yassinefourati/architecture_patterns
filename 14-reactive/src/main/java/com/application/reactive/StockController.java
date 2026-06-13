package com.application.reactive;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Reactive controller: handlers return Mono/Flux instead of plain values.
 * Spring WebFlux subscribes to the returned publisher and writes data to the
 * response as it's emitted â€” without holding a thread.
 */
@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockRepository repository;
    private final PriceTickGenerator priceFeed;

    public StockController(StockRepository repository, PriceTickGenerator priceFeed) {
        this.repository = repository;
        this.priceFeed = priceFeed;
    }

    /** Classic request/response â€” Mono<Stock>. Returns immediately, body streamed when ready. */
    @GetMapping("/{symbol}")
    public Mono<Stock> getOne(@PathVariable String symbol) {
        return repository.findBySymbol(symbol.toUpperCase());
    }

    /** Flux<Stock> â€” emits each stock as it's read from DB. */
    @GetMapping
    public Flux<Stock> list() {
        return repository.findAllByOrderBySymbolAsc();
    }

    /**
     * Server-Sent Events. The connection stays open; the client receives a continuous
     * stream of ticks. ZERO threads are blocked while waiting â€” Netty handles many
     * thousands of concurrent SSE clients on a handful of event-loop threads.
     */
    @GetMapping(value = "/ticks", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PriceTickGenerator.PriceTick>> ticks(
        @RequestParam(required = false) String symbol
    ) {
        Flux<PriceTickGenerator.PriceTick> source = priceFeed.stream();
        if (symbol != null) {
            String filter = symbol.toUpperCase();
            source = source.filter(t -> t.symbol().equals(filter));
        }
        return source
            .map(tick -> ServerSentEvent.<PriceTickGenerator.PriceTick>builder()
                .event("price")
                .data(tick)
                .build())
            // heartbeat every 10s so clients can detect dropped connections
            .mergeWith(Flux.interval(Duration.ofSeconds(10))
                .map(i -> ServerSentEvent.<PriceTickGenerator.PriceTick>builder()
                    .event("heartbeat").comment("ping").build()));
    }
}
