package com.application.ddd.order.domain;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate root. The ONLY entry point into the aggregate.
 *
 * Enforces invariants:
 *  - Lines can only be modified while in DRAFT
 *  - Cannot place an empty order
 *  - Cannot cancel a shipped order
 *  - Quantities for the same product collapse onto a single line
 *
 * Collects domain events that callers can pull after a successful operation.
 */
public class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status;
    private final List<DomainEvent> events = new ArrayList<>();

    private Order(OrderId id, CustomerId customerId, OrderStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
    }

    /** Factory: start a draft order. */
    public static Order draft(CustomerId customerId) {
        return new Order(OrderId.newId(), customerId, OrderStatus.DRAFT);
    }

    /** Rehydrate from persistence â€” no events emitted. */
    public static Order rehydrate(OrderId id, CustomerId customerId, OrderStatus status, List<OrderLine> lines) {
        Order order = new Order(id, customerId, status);
        order.lines.addAll(lines);
        return order;
    }

    public void addLine(ProductId productId, int quantity, Money unitPrice) {
        ensureDraft();
        lines.stream()
            .filter(l -> l.productId().equals(productId))
            .findFirst()
            .ifPresentOrElse(
                existing -> existing.increaseQuantity(quantity),
                () -> lines.add(new OrderLine(productId, quantity, unitPrice))
            );
    }

    public void place() {
        ensureDraft();
        if (lines.isEmpty()) throw new IllegalStateException("Cannot place an empty order");
        this.status = OrderStatus.PLACED;
        events.add(new OrderPlaced(id, customerId, total(), Instant.now()));
    }

    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED) throw new IllegalStateException("Cannot cancel a shipped order");
        if (status == OrderStatus.CANCELLED) return; // idempotent
        this.status = OrderStatus.CANCELLED;
        events.add(new OrderCancelled(id, reason, Instant.now()));
    }

    public Money total() {
        return lines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }

    private void ensureDraft() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Order is " + status + ", not modifiable");
        }
    }

    /** Drain and return pending events. Caller publishes them. */
    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(events);
        events.clear();
        return copy;
    }

	public OrderId getId() {
		return id;
	}

	public CustomerId getCustomerId() {
		return customerId;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public List<OrderLine> getLines() {
		return Collections.unmodifiableList(lines);
	}
}
