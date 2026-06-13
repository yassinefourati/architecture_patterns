package com.application.modulith.inventory;

/**
 * Public API of the inventory module. Other modules may depend on this interface.
 * Everything in inventory.internal.* is invisible to other modules (Spring Modulith enforces this).
 */
public interface InventoryApi {
    StockStatus checkStock(String productCode);
    void reserve(String productCode, int quantity);

    record StockStatus(String productCode, int quantityOnHand, boolean available) {}
}
