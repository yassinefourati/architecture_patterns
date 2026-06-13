package com.application.ddd.order.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderJpaEntity {

    @Id
    UUID id;

    @Column(nullable = false)
    String customerId;

    @Column(nullable = false)
    String status;

    @Column(nullable = false, length = 3)
    String currency;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
    List<LineEmbeddable> lines = new ArrayList<>();

    @Embeddable
    static class LineEmbeddable {
        String productId;
        int quantity;
        @Column(precision = 19, scale = 2)
        BigDecimal unitPrice;

        protected LineEmbeddable() {}
        LineEmbeddable(String productId, int quantity, BigDecimal unitPrice) {
            this.productId = productId; this.quantity = quantity; this.unitPrice = unitPrice;
        }
    }
}
