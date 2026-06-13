package com.application.hexagonal.domain.model;

import java.math.BigDecimal;

public record OrderLine(String productCode, int quantity, BigDecimal unitPrice) {
	
	public OrderLine {
		if (quantity <= 0)
			throw new IllegalArgumentException("quantity must be positive");
		if (unitPrice == null || unitPrice.signum() < 0)
			throw new IllegalArgumentException("unitPrice must be non-negative");
	}

	public BigDecimal subtotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

}
