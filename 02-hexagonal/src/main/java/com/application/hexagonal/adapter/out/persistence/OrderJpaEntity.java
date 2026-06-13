package com.application.hexagonal.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String customerEmail;

	@Column(nullable = false)
	private String status;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
	private List<OrderLineEmbeddable> lines = new ArrayList<>();

    protected OrderJpaEntity() {}

	OrderJpaEntity(UUID id, String customerEmail, String status, List<OrderLineEmbeddable> lines) {
		this.id = id;
		this.customerEmail = customerEmail;
		this.status = status;
		this.lines = lines;
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

	public List<OrderLineEmbeddable> getLines() { return lines; }

    @Embeddable
	static class OrderLineEmbeddable {
		private String productCode;
		private int quantity;
		@Column(precision = 10, scale = 2)
		private BigDecimal unitPrice;

		protected OrderLineEmbeddable() {
		}

		OrderLineEmbeddable(String productCode, int quantity, BigDecimal unitPrice) {
			this.productCode = productCode;
			this.quantity = quantity;
			this.unitPrice = unitPrice;
		}

		public String getProductCode() {
			return productCode;
		}

		public int getQuantity() {
			return quantity;
		}

		public BigDecimal getUnitPrice() {
			return unitPrice;
		}
	}
}
