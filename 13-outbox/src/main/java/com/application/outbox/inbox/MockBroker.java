package com.application.outbox.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Stands in for Kafka/RabbitMQ. In-process so the demo is self-contained.
 *
 * Two demo features:
 *  - Random failures: 20% of publishes throw, exercising the retry logic.
 *  - Duplicate delivery: when "at-least-once" mode is on, every message is
 *    delivered twice. This is what exercises the inbox / idempotency consumer.
 */
@Component
public class MockBroker {

    private static final Logger log = LoggerFactory.getLogger(MockBroker.class);

    private final List<Consumer<BrokerMessage>> subscribers = new CopyOnWriteArrayList<>();
    private final AtomicBoolean atLeastOnce = new AtomicBoolean(true);
    private final AtomicBoolean simulateFailures = new AtomicBoolean(true);

    public void subscribe(Consumer<BrokerMessage> subscriber) {
        subscribers.add(subscriber);
    }

    public void publish(String topic, UUID messageId, String payload) {
        if (simulateFailures.get() && ThreadLocalRandom.current().nextInt(100) < 20) {
            throw new RuntimeException("Simulated broker failure");
        }
        BrokerMessage msg = new BrokerMessage(topic, messageId, payload);
        deliver(msg);
        if (atLeastOnce.get() && ThreadLocalRandom.current().nextInt(100) < 50) {
            log.info("[broker] redelivering {} (at-least-once simulation)", messageId);
            deliver(msg);  // duplicate
        }
    }

    private void deliver(BrokerMessage msg) {
        for (Consumer<BrokerMessage> sub : subscribers) {
            sub.accept(msg);
        }
    }

    public void setSimulateFailures(boolean v) { simulateFailures.set(v); }
    public void setAtLeastOnce(boolean v) { atLeastOnce.set(v); }

    public record BrokerMessage(String topic, UUID messageId, String payload) {}
}
