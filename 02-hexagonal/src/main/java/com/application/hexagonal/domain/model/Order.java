package com.application.hexagonal.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Pure domain entity. No Spring, no JPA annotations. The framework lives at the
 * edge; the core is plain Java.
 */
public class Order {

	private final OrderId id;
	private final String customerEmail;
	private final List<OrderLine> lines;
	private OrderStatus status;

	private Order(OrderId id, String customerEmail, List<OrderLine> lines, OrderStatus status) {
		this.id = id;
		this.customerEmail = customerEmail;
		this.lines = lines;
		this.status = status;
	}

	public static Order create(String customerEmail, List<OrderLine> lines) {
		if (lines == null || lines.isEmpty()) {
			throw new IllegalArgumentException("Order must contain at least one line");
		}
		return new Order(new OrderId(UUID.randomUUID()), customerEmail, new ArrayList<>(lines), OrderStatus.PLACED);
	}

	public static Order rehydrate(OrderId id, String email, List<OrderLine> lines, OrderStatus status) {
		return new Order(id, email, new ArrayList<>(lines), status);
	}

	public void cancel() {
		if (status == OrderStatus.SHIPPED) {
			throw new IllegalStateException("Cannot cancel a shipped order");
		}
		this.status = OrderStatus.CANCELLED;
	}

	public BigDecimal total() {
		return lines.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public OrderId getId() {
		return id;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public List<OrderLine> getLines() {
		return Collections.unmodifiableList(lines);
	}

	public OrderStatus getStatus() {
		return status;
	}
}
