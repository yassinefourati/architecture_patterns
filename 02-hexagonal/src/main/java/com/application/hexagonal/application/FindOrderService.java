package com.application.hexagonal.application;

import com.application.hexagonal.domain.model.Order;
import com.application.hexagonal.domain.model.OrderId;
import com.application.hexagonal.domain.port.in.FindOrderUseCase;
import com.application.hexagonal.domain.port.out.OrderRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class FindOrderService implements FindOrderUseCase {

    private final OrderRepositoryPort orderRepository;

    public FindOrderService(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return orderRepository.findById(id);
    }
}
