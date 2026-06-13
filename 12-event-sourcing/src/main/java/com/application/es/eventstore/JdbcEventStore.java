package com.application.es.eventstore;

import com.application.es.domain.AccountEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Component
public class JdbcEventStore implements EventStore {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public JdbcEventStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void append(UUID aggregateId, long expectedVersion, List<AccountEvent> events) {
        long nextVersion = expectedVersion + 1;
        for (AccountEvent event : events) {
            try {
                jdbc.update("""
                    INSERT INTO event_store (aggregate_id, aggregate_type, event_type, aggregate_version, payload, occurred_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    aggregateId, "Account", event.getClass().getSimpleName(), nextVersion,
                    serialize(event), Timestamp.from(event.occurredAt())
                );
                nextVersion++;
            } catch (DuplicateKeyException ex) {
                // The (aggregate_id, aggregate_version) unique constraint blocked us.
                // Someone else wrote a concurrent event for this aggregate.
                throw new ConcurrencyException(
                    "Concurrent modification on aggregate " + aggregateId + " at version " + (nextVersion - 1)
                );
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountEvent> loadStream(UUID aggregateId) {
        return jdbc.query(
            "SELECT event_type, payload FROM event_store WHERE aggregate_id = ? ORDER BY aggregate_version",
            (rs, rowNum) -> deserialize(rs.getString("event_type"), rs.getString("payload")),
            aggregateId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> loadAfter(long sequenceNo, int batchSize) {
        return jdbc.query("""
            SELECT sequence_no, aggregate_id, aggregate_version, event_type, payload
            FROM event_store WHERE sequence_no > ? ORDER BY sequence_no LIMIT ?
            """,
            (rs, rowNum) -> new StoredEvent(
                rs.getLong("sequence_no"),
                (UUID) rs.getObject("aggregate_id"),
                rs.getLong("aggregate_version"),
                deserialize(rs.getString("event_type"), rs.getString("payload"))
            ),
            sequenceNo, batchSize
        );
    }

    private String serialize(AccountEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }

    private AccountEvent deserialize(String type, String payload) {
        try {
            return mapper.readValue(payload, AccountEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize event " + type, e);
        }
    }
}
