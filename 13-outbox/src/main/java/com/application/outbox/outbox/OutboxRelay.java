package com.application.outbox.outbox;

import com.application.outbox.inbox.MockBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The relay. Runs on a schedule, pulls a batch of PENDING messages, publishes
 * them to the broker, marks them PROCESSED.
 *
 * In production: use Debezium (Postgres logical replication â†’ Kafka) so the
 * relay reads the WAL directly. This eliminates polling and gives near-realtime
 * propagation. The polling approach here works the same way conceptually but
 * scales worse.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 20;

    private final OutboxRepository repository;
    private final MockBroker broker;

    public OutboxRelay(OutboxRepository repository, MockBroker broker) {
        this.repository = repository;
        this.broker = broker;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void relay() {
        List<OutboxMessage> batch = repository.lockBatch("PENDING", PageRequest.of(0, BATCH_SIZE));
        if (batch.isEmpty()) return;

        for (OutboxMessage message : batch) {
            try {
                broker.publish(message.getEventType(), message.getId(), message.getPayload());
                message.markProcessed();
                log.debug("[outbox-relay] published {}/{}", message.getEventType(), message.getId());
            } catch (Exception ex) {
                message.markFailed(ex.getMessage());
                log.warn("[outbox-relay] failed to publish {}/{} (attempt {}): {}",
                    message.getEventType(), message.getId(), message.getAttempts(), ex.getMessage());
            }
        }
        // The @Transactional commit here persists all the status updates atomically.
    }
}
