package com.application.cqrs.query;

import java.time.Instant;
import java.util.UUID;

/** Read model - shaped for UI display, not for invariants. */
public record BookCatalogView(
    UUID bookId,
    String displayLabel,
    String formattedPrice,
    int stock,
    boolean inStock,
    Instant updatedAt) {
	
}
