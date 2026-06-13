package com.application.layered.exception;

public class ProductNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -8635284880884677938L;

	public ProductNotFoundException(Long id) {
		super("Product not found: " + id);
	}
}
