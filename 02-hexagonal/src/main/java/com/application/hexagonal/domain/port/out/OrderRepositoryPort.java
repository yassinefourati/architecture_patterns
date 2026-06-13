package com.application.hexagonal.domain.port.out;

import com.application.hexagonal.domain.model.Order;
import com.application.hexagonal.domain.model.OrderId;
import java.util.Optional;

/** Driven port: what the application needs from the outside. */
public interface OrderRepositoryPort {

	Order save(Order order);

	Optional<Order> findById(OrderId id);

}
