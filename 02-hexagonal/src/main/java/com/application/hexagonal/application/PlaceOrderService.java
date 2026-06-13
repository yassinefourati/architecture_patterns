package com.application.hexagonal.application;

import com.application.hexagonal.domain.model.Order;
import com.application.hexagonal.domain.model.OrderId;
import com.application.hexagonal.domain.port.in.PlaceOrderUseCase;
import com.application.hexagonal.domain.port.out.OrderRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderRepositoryPort orderRepository;

    public PlaceOrderService(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderId placeOrder(PlaceOrderCommand command) {
        Order order = Order.create(command.customerEmail(), command.lines());
        return orderRepository.save(order).getId();
    }

}
