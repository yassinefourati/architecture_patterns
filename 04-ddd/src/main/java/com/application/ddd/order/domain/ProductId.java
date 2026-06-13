package com.application.ddd.order.domain;

public record ProductId(String value) {
    
	public ProductId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("ProductId required");
    }

}
