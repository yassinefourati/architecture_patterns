package com.application.hexagonal.domain.port.in;

import com.application.hexagonal.domain.model.OrderId;
import com.application.hexagonal.domain.model.OrderLine;
import java.util.List;

/** Driving port: what the application offers to the outside world. */
public interface PlaceOrderUseCase {

    OrderId placeOrder(PlaceOrderCommand command);

	record PlaceOrderCommand(String customerEmail, List<OrderLine> lines) {
		
	}

}
