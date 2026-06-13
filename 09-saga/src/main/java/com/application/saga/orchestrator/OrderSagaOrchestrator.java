package com.application.saga.orchestrator;

import com.application.saga.inventory.InventoryService;
import com.application.saga.payment.PaymentService;
import com.application.saga.shipping.ShippingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orchestration-based saga. The orchestrator drives the workflow and compensates
 * on failure. Each step is a separate local transaction in its service; the saga
 * ties them together with manual undo logic.
 *
 * Contrast with choreography: no central coordinator, services react to each
 * other's events. Trade-off: harder to reason about, easier to scale.
 */
@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;

    public OrderSagaOrchestrator(PaymentService paymentService,
                                 InventoryService inventoryService,
                                 ShippingService shippingService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.shippingService = shippingService;
    }

    public UUID execute(OrderSagaContext ctx) {
        UUID sagaId = UUID.randomUUID();
        log.info("[saga {}] starting", sagaId);
        try {
            // Step 1: charge payment
            ctx.recordPayment(paymentService.charge(ctx.customerId(), ctx.amount()));

            // Step 2: reserve inventory
            ctx.recordReservation(inventoryService.reserve(ctx.productCode(), ctx.quantity()));

            // Step 3: schedule shipment
            ctx.recordShipment(shippingService.schedule(ctx.shippingAddress()));

            log.info("[saga {}] completed", sagaId);
            return sagaId;

        } catch (Exception ex) {
            log.warn("[saga {}] failed at step: {}. Compensatingâ€¦", sagaId, ex.getMessage());
            compensate(ctx);
            throw new SagaFailedException("Order saga failed: " + ex.getMessage(), ex);
        }
    }

    /** Undo completed steps in reverse order. Each compensation must be idempotent. */
    private void compensate(OrderSagaContext ctx) {
        if (ctx.shipmentId() != null) safe(() -> shippingService.cancel(ctx.shipmentId()));
        if (ctx.reservationId() != null) safe(() -> inventoryService.release(ctx.reservationId()));
        if (ctx.paymentId() != null) safe(() -> paymentService.refund(ctx.paymentId()));
    }

    private void safe(Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            // In production: log to a dead-letter store; manual intervention may be needed.
            log.error("Compensation step failed", ex);
        }
    }

    public static class SagaFailedException extends RuntimeException {
        public SagaFailedException(String msg, Throwable cause) { super(msg, cause); }
    }
}
