package com.application.outbox.order;

import com.application.outbox.outbox.OutboxPublisher;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService service;

    OrderController(OrderService service) { this.service = service; }

    @PostMapping
    ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceRequest request) {
        OrderEntity order = service.placeOrder(request.customerEmail(), request.amount(), request.failAfterOutbox());
        return ResponseEntity.ok(new OrderResponse(order.getId(), order.getStatus()));
    }

    record PlaceRequest(
        @Email String customerEmail,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        boolean failAfterOutbox  // demo: force a rollback after the outbox row is inserted
    ) {}
    record OrderResponse(UUID id, String status) {}
}

@Service
class OrderService {

    private final OrderRepository repository;
    private final OutboxPublisher outboxPublisher;

    OrderService(OrderRepository repository, OutboxPublisher outboxPublisher) {
        this.repository = repository;
        this.outboxPublisher = outboxPublisher;
    }

    /**
     * The whole method is one transaction. The order INSERT and the outbox INSERT
     * commit together or roll back together â€” no possibility of an order without
     * its event, or an event without its order.
     */
    @Transactional
    public OrderEntity placeOrder(String customerEmail, BigDecimal amount, boolean failAfterOutbox) {
        OrderEntity order = new OrderEntity(customerEmail, amount);
        repository.save(order);

        outboxPublisher.enqueue(
            "Order", order.getId().toString(), "OrderPlaced",
            new OrderPlacedPayload(order.getId(), customerEmail, amount, Instant.now())
        );

        // Demo trigger: simulate a failure between the writes and commit.
        // Both the order AND the outbox row get rolled back together â€” no leak.
        if (failAfterOutbox) {
            throw new RuntimeException("Simulated failure AFTER writing order + outbox row, BEFORE commit");
        }

        return order;
    }

    record OrderPlacedPayload(UUID orderId, String customerEmail, BigDecimal amount, Instant placedAt) {}
}

interface OrderRepository extends JpaRepository<OrderEntity, UUID> {}

@Entity
@Table(name = "orders")
class OrderEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    protected OrderEntity() {}

    OrderEntity(String customerEmail, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.status = "PLACED";
        this.createdAt = Instant.now();
    }

    UUID getId() { return id; }
    String getStatus() { return status; }
}
