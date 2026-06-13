package com.application.ddd.order.domain;

import java.util.Optional;

/** Domain repository â€” the interface is owned by the domain. */
public interface OrderRepository {

	void save(Order order);

	Optional<Order> findById(OrderId id);

}
