package com.application.ddd.order.infrastructure;

import com.application.ddd.order.domain.*;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.Optional;

@Component
class JpaOrderRepository implements OrderRepository {

    private final OrderJpaRepository jpa;

    JpaOrderRepository(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Order order) {
        OrderJpaEntity entity = jpa.findById(order.getId().value()).orElseGet(OrderJpaEntity::new);
        entity.id = order.getId().value();
        entity.customerId = order.getCustomerId().value();
        entity.status = order.getStatus().name();
        entity.currency = order.total().currency().getCurrencyCode();
        entity.lines.clear();
        order.getLines().forEach(l -> entity.lines.add(
            new OrderJpaEntity.LineEmbeddable(l.productId().value(), l.quantity(), l.unitPrice().amount())
        ));
        jpa.save(entity);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    private Order toDomain(OrderJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        var lines = entity.lines.stream()
            .map(l -> new OrderLine(new ProductId(l.productId), l.quantity, new Money(l.unitPrice, currency)))
            .toList();
        return Order.rehydrate(
            new OrderId(entity.id),
            new CustomerId(entity.customerId),
            OrderStatus.valueOf(entity.status),
            lines
        );
    }
}
