package com.application.clean.entity;

import java.math.BigDecimal;

/**
 * Enterprise-wide business rule: money arithmetic. Pure Java, innermost layer.
 */
public record Money(BigDecimal amount) {
	public static final Money ZERO = new Money(BigDecimal.ZERO);

	public Money {
		if (amount == null)
			throw new IllegalArgumentException("amount required");
		if (amount.signum() < 0)
			throw new IllegalArgumentException("amount must be non-negative");
	}

	public Money add(Money other) {
		return new Money(amount.add(other.amount));
	}

	public Money subtract(Money other) {
		return new Money(amount.subtract(other.amount));
	}

	public boolean isLessThan(Money other) {
		return amount.compareTo(other.amount) < 0;
	}
}
