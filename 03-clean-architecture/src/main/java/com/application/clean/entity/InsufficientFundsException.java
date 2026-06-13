package com.application.clean.entity;

public class InsufficientFundsException extends RuntimeException {

	private static final long serialVersionUID = -9028111907857935576L;

	public InsufficientFundsException(String accountId) {
		super("Insufficient funds in account " + accountId);
	}
}
