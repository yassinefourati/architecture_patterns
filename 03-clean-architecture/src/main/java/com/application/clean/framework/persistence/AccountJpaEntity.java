package com.application.clean.framework.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

	@Id
	private String id;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal balance;

	protected AccountJpaEntity() {
		
	}

	public AccountJpaEntity(String id, BigDecimal balance) {
		this.id = id;
		this.balance = balance;
	}

	public String getId() {
		return id;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

}
