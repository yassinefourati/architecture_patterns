package com.application.es.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Event-sourced aggregate. Two key methods:
 *
 *  1. Decision functions (open, deposit, withdraw, close): take the current state,
 *     produce a NEW EVENT. They do NOT mutate state directly.
 *  2. apply(event): the only place state changes â€” folds the event into state.
 *
 * To load: replay all events through apply().
 * To save: persist only the newly produced events.
 *
 * This separation is what makes event sourcing testable: given a list of past
 * events and a command, assert which event was produced.
 */
public class Account {

    private UUID id;
    private String holder;
    private BigDecimal balance = BigDecimal.ZERO;
    private boolean closed = false;
    private long version = 0;  // number of events applied; used for optimistic concurrency

    private final List<AccountEvent> uncommittedEvents = new ArrayList<>();

    // === Decision functions: produce events from commands ===

    public static Account open(UUID accountId, String holder, BigDecimal openingBalance) {
        if (openingBalance.signum() < 0) throw new IllegalArgumentException("Opening balance must be non-negative");
        Account account = new Account();
        account.raise(new AccountEvent.AccountOpened(accountId, holder, openingBalance, Instant.now()));
        return account;
    }

    public void deposit(BigDecimal amount) {
        ensureOpen();
        if (amount.signum() <= 0) throw new IllegalArgumentException("Deposit must be positive");
        raise(new AccountEvent.MoneyDeposited(id, amount, Instant.now()));
    }

    public void withdraw(BigDecimal amount) {
        ensureOpen();
        if (amount.signum() <= 0) throw new IllegalArgumentException("Withdrawal must be positive");
        if (balance.compareTo(amount) < 0) throw new IllegalStateException("Insufficient funds");
        raise(new AccountEvent.MoneyWithdrawn(id, amount, Instant.now()));
    }

    public void close(String reason) {
        ensureOpen();
        if (balance.signum() != 0) throw new IllegalStateException("Cannot close account with non-zero balance");
        raise(new AccountEvent.AccountClosed(id, reason, Instant.now()));
    }

    // === Rehydration: replay events ===

    public static Account rehydrate(List<AccountEvent> history) {
        if (history.isEmpty()) throw new IllegalArgumentException("Cannot rehydrate empty history");
        Account account = new Account();
        history.forEach(account::apply);
        return account;
    }

    // === The fold function ===

    private void apply(AccountEvent event) {
        switch (event) {
            case AccountEvent.AccountOpened e -> {
                this.id = e.accountId();
                this.holder = e.holder();
                this.balance = e.openingBalance();
            }
            case AccountEvent.MoneyDeposited e -> this.balance = balance.add(e.amount());
            case AccountEvent.MoneyWithdrawn e -> this.balance = balance.subtract(e.amount());
            case AccountEvent.AccountClosed e -> this.closed = true;
        }
        this.version++;
    }

    private void raise(AccountEvent event) {
        apply(event);
        uncommittedEvents.add(event);
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Account is closed");
    }

    /** Drained by the repository after a successful persist. */
    public List<AccountEvent> pullUncommitted() {
        var copy = List.copyOf(uncommittedEvents);
        uncommittedEvents.clear();
        return copy;
    }

    public UUID getId() { return id; }
    public String getHolder() { return holder; }
    public BigDecimal getBalance() { return balance; }
    public boolean isClosed() { return closed; }
    public long getVersion() { return version; }
}
