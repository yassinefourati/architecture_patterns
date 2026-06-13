package com.application.ddd.order.domain;

import java.time.Instant;

public record OrderCancelled(OrderId orderId, String reason, Instant occurredAt) implements DomainEvent {
	
}
