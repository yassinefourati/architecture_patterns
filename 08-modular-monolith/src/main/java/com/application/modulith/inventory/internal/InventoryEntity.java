package com.application.modulith.inventory.internal;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
class InventoryEntity {

    @Id
    private String productCode;

    @Column(nullable = false)
    private int quantityOnHand;

    protected InventoryEntity() {}

    InventoryEntity(String productCode, int quantityOnHand) {
        this.productCode = productCode;
        this.quantityOnHand = quantityOnHand;
    }

    void reserve(int quantity) {
        if (quantity > quantityOnHand) {
            throw new IllegalStateException("Insufficient stock for " + productCode);
        }
        this.quantityOnHand -= quantity;
    }

    String getProductCode() { return productCode; }
    int getQuantityOnHand() { return quantityOnHand; }
}
