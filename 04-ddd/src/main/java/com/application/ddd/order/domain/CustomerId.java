package com.application.ddd.order.domain;

public record CustomerId(String value) {
	
    public CustomerId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("CustomerId required");
    }

}
