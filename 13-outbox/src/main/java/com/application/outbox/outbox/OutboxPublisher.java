package com.application.outbox.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application code calls this to enqueue messages. The save() runs in the CURRENT
 * transaction (REQUIRED), so the outbox row commits atomically with the business data.
 *
 * If the business transaction rolls back, no message is emitted. If it commits,
 * the message is durable until the relay publishes it.
 */
@Service
public class OutboxPublisher {

    private final OutboxRepository repository;
    private final ObjectMapper mapper;

    public OutboxPublisher(OutboxRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)  // must be inside an existing transaction
    public void enqueue(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String json = mapper.writeValueAsString(payload);
            repository.save(new OutboxMessage(aggregateType, aggregateId, eventType, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
