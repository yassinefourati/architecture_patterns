package com.application.outbox.order;

import com.application.outbox.inbox.IdempotentOrderConsumer;
import com.application.outbox.inbox.MockBroker;
import com.application.outbox.outbox.OutboxRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
class AdminController {

    private final OutboxRepository outboxRepository;
    private final IdempotentOrderConsumer consumer;
    private final MockBroker broker;

    AdminController(OutboxRepository outboxRepository, IdempotentOrderConsumer consumer, MockBroker broker) {
        this.outboxRepository = outboxRepository;
        this.consumer = consumer;
        this.broker = broker;
    }

    @GetMapping("/status")
    Map<String, Object> status() {
        return Map.of(
            "outboxTotal", outboxRepository.count(),
            "consumerProcessed", consumer.getProcessedCount(),
            "consumerDuplicatesSkipped", consumer.getDuplicateCount()
        );
    }

    @PostMapping("/broker/failures/{state}")
    Map<String, String> failures(@PathVariable String state) {
        broker.setSimulateFailures("on".equalsIgnoreCase(state));
        return Map.of("simulateFailures", state);
    }

    @PostMapping("/broker/at-least-once/{state}")
    Map<String, String> atLeastOnce(@PathVariable String state) {
        broker.setAtLeastOnce("on".equalsIgnoreCase(state));
        return Map.of("atLeastOnce", state);
    }
}
