package com.application.modulith.order.internal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String productCode;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private Instant createdAt;

    protected OrderEntity() {}

    OrderEntity(String productCode, int quantity, String customerEmail) {
        this.id = UUID.randomUUID();
        this.productCode = productCode;
        this.quantity = quantity;
        this.customerEmail = customerEmail;
        this.createdAt = Instant.now();
    }

    UUID getId() { return id; }
}
