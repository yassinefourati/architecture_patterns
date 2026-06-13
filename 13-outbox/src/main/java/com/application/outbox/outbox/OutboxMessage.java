package com.application.outbox.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * The outbox table. Written in the SAME TRANSACTION as the business data.
 * A separate relay process publishes these to the broker, then marks them processed.
 *
 * Status field uses a tiny state machine: PENDING â†’ PROCESSED, or PENDING â†’ FAILED after retries.
 */
@Entity
@Table(name = "outbox", indexes = {
    @Index(name = "idx_outbox_pending", columnList = "status, createdAt")
})
public class OutboxMessage {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private String status; // PENDING, PROCESSED, FAILED

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt;
    private String lastError;

    protected OutboxMessage() {}

    public OutboxMessage(String aggregateType, String aggregateId, String eventType, String payload) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = "PENDING";
        this.attempts = 0;
        this.createdAt = Instant.now();
    }

    public void markProcessed() {
        this.status = "PROCESSED";
        this.processedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error;
        if (this.attempts >= 5) {
            this.status = "FAILED";  // dead-letter
        }
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
}
