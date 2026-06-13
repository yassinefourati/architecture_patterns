package com.application.outbox.inbox;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Subscribes to the broker and processes messages idempotently via the inbox.
 *
 * Strategy: try to INSERT into inbox; if it succeeds, process the business logic.
 * If the insert fails with a duplicate-key error, the message has already been
 * processed â€” silently discard.
 *
 * Both the inbox insert AND the side effect must run in the same transaction.
 * Otherwise a crash between them leaves you in the broken middle state.
 */
@Component
public class IdempotentOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(IdempotentOrderConsumer.class);

    private final MockBroker broker;
    private final InboxRepository inboxRepository;
    private final AtomicInteger processedCount = new AtomicInteger();
    private final AtomicInteger duplicateCount = new AtomicInteger();

    public IdempotentOrderConsumer(MockBroker broker, InboxRepository inboxRepository) {
        this.broker = broker;
        this.inboxRepository = inboxRepository;
    }

    @PostConstruct
    void subscribe() {
        broker.subscribe(this::onMessage);
    }

    private void onMessage(MockBroker.BrokerMessage msg) {
        try {
            handle(msg);
        } catch (DuplicateMessageException e) {
            duplicateCount.incrementAndGet();
            log.debug("[consumer] duplicate {}, skipped", msg.messageId());
        } catch (Exception e) {
            // In production: nack to the broker so it redelivers; or send to DLQ after N retries.
            log.error("[consumer] failed to process {}: {}", msg.messageId(), e.getMessage());
        }
    }

    @Transactional
    public void handle(MockBroker.BrokerMessage msg) {
        // Step 1: try to claim the message in the inbox
        try {
            inboxRepository.saveAndFlush(new InboxRecord(msg.messageId(), msg.topic()));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateMessageException(msg.messageId());
        }

        // Step 2: the actual side effect (send email, update read model, call external APIâ€¦)
        // Wrapped in the SAME transaction as the inbox insert.
        // In production this might be: emailService.send(...), or projection.update(...)
        processedCount.incrementAndGet();
        log.info("[consumer] processed {} (topic={}) â€” total processed={}, duplicates skipped={}",
            msg.messageId(), msg.topic(), processedCount.get(), duplicateCount.get());
    }

    public int getProcessedCount() { return processedCount.get(); }
    public int getDuplicateCount() { return duplicateCount.get(); }

    static class DuplicateMessageException extends RuntimeException {
        DuplicateMessageException(java.util.UUID id) { super("Duplicate: " + id); }
    }
}
