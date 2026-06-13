package com.application.events.order;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String status; // PLACED, CONFIRMED, REJECTED

    @Column(nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<ItemEmbeddable> items = new ArrayList<>();

    protected OrderEntity() {}

    public OrderEntity(String customerEmail, List<ItemEmbeddable> items) {
        this.id = UUID.randomUUID();
        this.customerEmail = customerEmail;
        this.items = items;
        this.status = "PLACED";
        this.createdAt = Instant.now();
    }

	public void confirm() {
		this.status = "CONFIRMED";
	}

	public void reject() {
		this.status = "REJECTED";
	}

	public UUID getId() {
		return id;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public String getStatus() {
		return status;
	}

	public List<ItemEmbeddable> getItems() {
		return items;
	}

    @Embeddable
    public static class ItemEmbeddable {
        private String productCode;
        private int quantity;

        protected ItemEmbeddable() {}

        public ItemEmbeddable(String productCode, int quantity) {
            this.productCode = productCode;
            this.quantity = quantity;
        }

        public String getProductCode() { return productCode; }
        public int getQuantity() { return quantity; }
    }
}
