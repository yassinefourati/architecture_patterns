package com.application.hexagonal.domain.port.in;

import com.application.hexagonal.domain.model.Order;
import com.application.hexagonal.domain.model.OrderId;
import java.util.Optional;

public interface FindOrderUseCase {

    Optional<Order> findById(OrderId id);

}
