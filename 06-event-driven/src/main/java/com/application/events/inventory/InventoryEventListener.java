package com.application.events.inventory;

import com.application.events.shared.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryRepository repository;
    private final ApplicationEventPublisher publisher;

    public InventoryEventListener(InventoryRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * Subscribe to OrderPlaced. Run AFTER_COMMIT of the publisher's transaction â€”
     * if the order persistence rolls back, we never see this event.
     * Then run in our own transaction so the reservation is its own atomic step.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void on(Events.OrderPlaced event) {
        log.info("[inventory] received OrderPlaced for order={}", event.orderId());
        try {
            for (var item : event.items()) {
                InventoryItem stock = repository.findById(item.productCode())
                    .orElseThrow(() -> new IllegalStateException("Unknown product: " + item.productCode()));
                stock.reserve(item.quantity());
            }
            publisher.publishEvent(new Events.InventoryReserved(event.orderId(), Instant.now()));
            log.info("[inventory] reserved for order={}", event.orderId());
        } catch (IllegalStateException ex) {
            log.warn("[inventory] rejected order={}: {}", event.orderId(), ex.getMessage());
            publisher.publishEvent(new Events.InventoryRejected(event.orderId(), ex.getMessage(), Instant.now()));
        }
    }

    @Configuration
    static class InventorySeed {
        @Bean
        CommandLineRunner seed(InventoryRepository repository) {
            return args -> {
                repository.save(new InventoryItem("WIDGET-1", 10));
                repository.save(new InventoryItem("WIDGET-2", 5));
                repository.save(new InventoryItem("WIDGET-3", 0));
            };
        }
    }
}
