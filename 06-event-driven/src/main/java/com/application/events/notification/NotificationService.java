package com.application.events.notification;

import com.application.events.shared.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notification module: a third consumer demonstrating that multiple
 * independent listeners can react to the same event.
 *
 * @Async runs on a separate thread pool slow IO won't block the request thread.
 */
@Component
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(Events.OrderPlaced event) {
        log.info("[notification] (async) sending order-received email to {}", event.customerEmail());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReserved(Events.InventoryReserved event) {
        log.info("[notification] (async) sending order-confirmed email for order={}", event.orderId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRejected(Events.InventoryRejected event) {
        log.info("[notification] (async) sending order-rejected email for order={}: {}",
            event.orderId(), event.reason());
    }
}
