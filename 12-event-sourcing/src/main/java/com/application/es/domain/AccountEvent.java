package com.application.es.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain events. These are the SOURCE OF TRUTH â€” the aggregate's current state
 * is derived by replaying them. Events are immutable, append-only.
 *
 * Jackson polymorphism config: each event serializes with a "type" discriminator
 * so we can round-trip it through the event_store JSONB column.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AccountEvent.AccountOpened.class, name = "AccountOpened"),
    @JsonSubTypes.Type(value = AccountEvent.MoneyDeposited.class, name = "MoneyDeposited"),
    @JsonSubTypes.Type(value = AccountEvent.MoneyWithdrawn.class, name = "MoneyWithdrawn"),
    @JsonSubTypes.Type(value = AccountEvent.AccountClosed.class, name = "AccountClosed")
})
public sealed interface AccountEvent {

    UUID accountId();
    Instant occurredAt();

    record AccountOpened(UUID accountId, String holder, BigDecimal openingBalance, Instant occurredAt) implements AccountEvent {}
    record MoneyDeposited(UUID accountId, BigDecimal amount, Instant occurredAt) implements AccountEvent {}
    record MoneyWithdrawn(UUID accountId, BigDecimal amount, Instant occurredAt) implements AccountEvent {}
    record AccountClosed(UUID accountId, String reason, Instant occurredAt) implements AccountEvent {}
}
