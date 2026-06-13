package com.application.saga.orchestrator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saga state. Tracks completed steps so we can compensate them on failure.
 */
public class OrderSagaContext {

    private final String customerId;
    private final String productCode;
    private final int quantity;
    private final BigDecimal amount;
    private final String shippingAddress;

    private UUID paymentId;
    private UUID reservationId;
    private UUID shipmentId;

    public OrderSagaContext(String customerId, String productCode, int quantity,
                            BigDecimal amount, String shippingAddress) {
        this.customerId = customerId;
        this.productCode = productCode;
        this.quantity = quantity;
        this.amount = amount;
        this.shippingAddress = shippingAddress;
    }

    public String customerId() { return customerId; }
    public String productCode() { return productCode; }
    public int quantity() { return quantity; }
    public BigDecimal amount() { return amount; }
    public String shippingAddress() { return shippingAddress; }

    public UUID paymentId() { return paymentId; }
    public UUID reservationId() { return reservationId; }
    public UUID shipmentId() { return shipmentId; }

    public void recordPayment(UUID id) { this.paymentId = id; }
    public void recordReservation(UUID id) { this.reservationId = id; }
    public void recordShipment(UUID id) { this.shipmentId = id; }
}
