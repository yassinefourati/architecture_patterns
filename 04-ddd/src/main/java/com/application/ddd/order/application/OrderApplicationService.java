package com.application.ddd.order.application;

import com.application.ddd.order.domain.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderApplicationService {

	private final OrderRepository orderRepository;
	private final ApplicationEventPublisher publisher;

	public OrderApplicationService(OrderRepository orderRepository, ApplicationEventPublisher publisher) {
		this.orderRepository = orderRepository;
		this.publisher = publisher;
	}

	public OrderId openDraft(String customerId) {
		Order order = Order.draft(new CustomerId(customerId));
		orderRepository.save(order);
		return order.getId();
	}

	public void addLine(OrderId id, String productId, int quantity, Money unitPrice) {
		Order order = loadOrFail(id);
		order.addLine(new ProductId(productId), quantity, unitPrice);
		orderRepository.save(order);
	}

	public void place(OrderId id) {
		Order order = loadOrFail(id);
		order.place();
		orderRepository.save(order);
		publishEvents(order.pullEvents());
	}

	public void cancel(OrderId id, String reason) {
		Order order = loadOrFail(id);
		order.cancel(reason);
		orderRepository.save(order);
		publishEvents(order.pullEvents());
	}

	@Transactional(readOnly = true)
	public Order find(OrderId id) {
		return loadOrFail(id);
	}

	private Order loadOrFail(OrderId id) {
		return orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id.value()));
	}

	private void publishEvents(List<DomainEvent> events) {
		events.forEach(publisher::publishEvent);
	}
}
