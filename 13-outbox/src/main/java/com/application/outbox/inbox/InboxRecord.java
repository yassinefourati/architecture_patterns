package com.application.outbox.inbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * The inbox pattern: the consumer's counterpart to the outbox.
 *
 * When a message arrives, the consumer first checks if its ID is already in the
 * inbox. If yes â€” duplicate, discard. If no â€” process the message AND insert
 * the ID into the inbox in the same transaction.
 *
 * This makes the consumer idempotent without needing the broker to guarantee
 * exactly-once delivery (which is expensive/impossible at scale).
 */
@Entity
@Table(name = "inbox")
public class InboxRecord {

    @Id
    private UUID messageId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private Instant processedAt;

    protected InboxRecord() {}

    public InboxRecord(UUID messageId, String topic) {
        this.messageId = messageId;
        this.topic = topic;
        this.processedAt = Instant.now();
    }

    public UUID getMessageId() { return messageId; }
}
