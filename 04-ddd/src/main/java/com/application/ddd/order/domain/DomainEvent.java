package com.application.ddd.order.domain;

import java.time.Instant;

public sealed interface DomainEvent permits OrderPlaced, OrderCancelled {

    Instant occurredAt();

}
