package com.application.ddd.order.domain;

/** Entity within the Order aggregate. Not accessed directly from outside. */
public class OrderLine {

	private final ProductId productId;
	private int quantity;
	private final Money unitPrice;

	public OrderLine(ProductId productId, int quantity, Money unitPrice) {
		if (quantity <= 0)
			throw new IllegalArgumentException("quantity must be positive");
		this.productId = productId;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
	}

	void increaseQuantity(int additional) {
		if (additional <= 0)
			throw new IllegalArgumentException("additional must be positive");
		this.quantity += additional;
	}

	public Money subtotal() {
		return unitPrice.multiply(quantity);
	}

	public ProductId productId() {
		return productId;
	}

	public int quantity() {
		return quantity;
	}

	public Money unitPrice() {
		return unitPrice;
	}
}
