package com.application.ddd.order.application;

import com.application.ddd.order.domain.OrderPlaced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Demo handler: in a real system this might send a confirmation email. */
@Component
public class OrderPlacedHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedHandler.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(OrderPlaced event) {
        log.info("[event] Order {} placed by {} for {} {}",
            event.orderId().value(),
            event.customerId().value(),
            event.total().amount(),
            event.total().currency());
    }
}
