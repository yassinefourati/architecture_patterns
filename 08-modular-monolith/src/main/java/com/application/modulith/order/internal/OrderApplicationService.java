package com.application.modulith.order.internal;

import com.application.modulith.inventory.InventoryApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class OrderApplicationService {

    private final OrderJpaRepository repository;
    private final InventoryApi inventoryApi;     // depends on the PUBLIC interface, not internal classes

    OrderApplicationService(OrderJpaRepository repository, InventoryApi inventoryApi) {
        this.repository = repository;
        this.inventoryApi = inventoryApi;
    }

    public UUID placeOrder(String productCode, int quantity, String customerEmail) {
        // Reserve stock via the inventory module's public API.
        // We cannot reach into inventory's internal entities or repository.
        inventoryApi.reserve(productCode, quantity);

        OrderEntity order = new OrderEntity(productCode, quantity, customerEmail);
        repository.save(order);
        return order.getId();
    }
}
