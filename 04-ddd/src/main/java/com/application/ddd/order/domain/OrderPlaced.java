package com.application.ddd.order.domain;

import java.time.Instant;

public record OrderPlaced(OrderId orderId, CustomerId customerId, Money total, Instant occurredAt) implements DomainEvent {
	
}
