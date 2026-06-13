package com.application.cqrs.command;

import java.math.BigDecimal;
import java.util.UUID;

public sealed interface BookCommand {

	record CreateBook(String title, String author, BigDecimal price, int initialStock) implements BookCommand {
		
	}

	record ChangePrice(UUID bookId, BigDecimal newPrice) implements BookCommand {
		
	}

	record AdjustStock(UUID bookId, int delta) implements BookCommand {
		
	}

}
