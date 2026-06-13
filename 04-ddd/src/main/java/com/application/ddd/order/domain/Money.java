package com.application.ddd.order.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/** Value object: immutable, equality by value, owns its invariants. */
public record Money(BigDecimal amount, Currency currency) {

    public static final Currency USD = Currency.getInstance("USD");
    public static final Money ZERO = new Money(BigDecimal.ZERO, USD);

    public Money {
        Objects.requireNonNull(amount, "amount required");
        Objects.requireNonNull(currency, "currency required");
        if (amount.signum() < 0) throw new IllegalArgumentException("Money cannot be negative");
    }

    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    private void ensureSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
