package com.application.cqrs.command;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "books")
public class Book {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String author;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private int stock;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private Long version;

	protected Book() {
	}

	public Book(String title, String author, BigDecimal price, int stock) {
		if (price.signum() < 0)
			throw new IllegalArgumentException("price must be non-negative");
		if (stock < 0)
			throw new IllegalArgumentException("stock must be non-negative");
		this.id = UUID.randomUUID();
		this.title = title;
		this.author = author;
		this.price = price;
		this.stock = stock;
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void changePrice(BigDecimal newPrice) {
		if (newPrice.signum() < 0)
			throw new IllegalArgumentException("price must be non-negative");
		this.price = newPrice;
		this.updatedAt = Instant.now();
	}

	public void adjustStock(int delta) {
		int updated = this.stock + delta;
		if (updated < 0)
			throw new IllegalStateException("stock cannot go negative");
		this.stock = updated;
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getStock() {
		return stock;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
