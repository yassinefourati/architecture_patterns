package com.application.hexagonal.adapter.out.persistence;

import com.application.hexagonal.domain.model.*;
import com.application.hexagonal.domain.port.out.OrderRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Driven adapter: implements the outbound port using JPA.
 * The domain has no idea this exists.
 */
@Component
class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final OrderSpringDataRepository repository;

    OrderPersistenceAdapter(OrderSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = toEntity(order);
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    private OrderJpaEntity toEntity(Order order) {
        var lines = order.getLines().stream()
            .map(l -> new OrderJpaEntity.OrderLineEmbeddable(l.productCode(), l.quantity(), l.unitPrice()))
            .toList();
        return new OrderJpaEntity(order.getId().value(), order.getCustomerEmail(), order.getStatus().name(), new java.util.ArrayList<>(lines));
    }

    private Order toDomain(OrderJpaEntity entity) {
        var lines = entity.getLines().stream()
            .map(l -> new OrderLine(l.getProductCode(), l.getQuantity(), l.getUnitPrice()))
            .toList();
        return Order.rehydrate(
            new OrderId(entity.getId()),
            entity.getCustomerEmail(),
            lines,
            OrderStatus.valueOf(entity.getStatus())
        );
    }
}
