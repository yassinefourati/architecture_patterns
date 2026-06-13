package com.application.clean.entity;

/**
 * Enterprise business rule: an account's invariants (no overdraft, identity).
 * Innermost layer. Knows nothing about Spring, JPA, web, or use cases.
 */
public class Account {

    private final String id;
    private Money balance;

    public Account(String id, Money balance) {
		if (id == null || id.isBlank())
			throw new IllegalArgumentException("id required");
        this.id = id;
        this.balance = balance;
    }

	public void withdraw(Money amount) {
		if (balance.isLessThan(amount)) {
			throw new InsufficientFundsException(id);
		}
		this.balance = balance.subtract(amount);
	}

	public void deposit(Money amount) {
		this.balance = balance.add(amount);
	}

	public String getId() {
		return id;
	}

	public Money getBalance() {
		return balance;
	}
}
