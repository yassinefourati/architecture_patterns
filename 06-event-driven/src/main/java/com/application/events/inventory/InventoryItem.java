package com.application.events.inventory;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class InventoryItem {

    @Id
    private String productCode;

    @Column(nullable = false)
    private int quantityOnHand;

    protected InventoryItem() {}

    public InventoryItem(String productCode, int quantityOnHand) {
        this.productCode = productCode;
        this.quantityOnHand = quantityOnHand;
    }

    public boolean canReserve(int quantity) {
        return quantityOnHand >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient stock for " + productCode);
        }
        this.quantityOnHand -= quantity;
    }

	public String getProductCode() {
		return productCode;
	}

	public int getQuantityOnHand() {
		return quantityOnHand;
	}
}
