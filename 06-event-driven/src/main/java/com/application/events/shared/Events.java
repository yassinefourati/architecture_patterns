package com.application.events.shared;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Events published across modules. Each module depends only on this contract. */
public final class Events {
	private Events() {
		
	}

	public record OrderPlaced(UUID orderId, String customerEmail, List<Item> items, Instant occurredAt) {
		public record Item(String productCode, int quantity) {
		}
	}

	public record InventoryReserved(UUID orderId, Instant occurredAt) {
	}

	public record InventoryRejected(UUID orderId, String reason, Instant occurredAt) {
	}
}
