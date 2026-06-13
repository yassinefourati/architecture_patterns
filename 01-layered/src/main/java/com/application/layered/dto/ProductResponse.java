package com.application.layered.dto;

import com.application.layered.domain.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, int stock) {
	
	public static ProductResponse from(Product p) {
		return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock());
	}

}
