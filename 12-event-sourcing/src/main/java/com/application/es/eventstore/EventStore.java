package com.application.es.eventstore;

import com.application.es.domain.AccountEvent;

import java.util.List;
import java.util.UUID;

public interface EventStore {

    /** Append events; throws ConcurrencyException if expected version doesn't match. */
    void append(UUID aggregateId, long expectedVersion, List<AccountEvent> events);

    /** Load all events for an aggregate, in order. */
    List<AccountEvent> loadStream(UUID aggregateId);

    /** Load events globally for projections, starting after sequence number. */
    List<StoredEvent> loadAfter(long sequenceNo, int batchSize);

    class ConcurrencyException extends RuntimeException {
        public ConcurrencyException(String msg) { super(msg); }
    }

    /** Event + metadata as stored. */
    record StoredEvent(long sequenceNo, UUID aggregateId, long aggregateVersion, AccountEvent event) {}
}
