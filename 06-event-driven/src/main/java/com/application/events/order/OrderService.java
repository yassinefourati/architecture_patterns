package com.application.events.order;

import com.application.events.shared.Events;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;
    private final ApplicationEventPublisher publisher;

    public OrderService(OrderRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public UUID place(String customerEmail, List<OrderEntity.ItemEmbeddable> items) {
        OrderEntity order = new OrderEntity(customerEmail, items);
        repository.save(order);

        publisher.publishEvent(new Events.OrderPlaced(
            order.getId(), customerEmail,
            items.stream().map(i -> new Events.OrderPlaced.Item(i.getProductCode(), i.getQuantity())).toList(),
            Instant.now()
        ));
        return order.getId();
    }

    @Transactional(readOnly = true)
    public OrderEntity find(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    /** React to downstream events to update order status. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onInventoryReserved(Events.InventoryReserved event) {
        repository.findById(event.orderId()).ifPresent(o -> {
            o.confirm();
            repository.save(o);
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onInventoryRejected(Events.InventoryRejected event) {
        repository.findById(event.orderId()).ifPresent(o -> {
            o.reject();
            repository.save(o);
        });
    }

}
