package com.application.clean.entity;

public class AccountNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -2174227905340245979L;

	public AccountNotFoundException(String accountId) {
		super("Account not found: " + accountId);
	}
}
