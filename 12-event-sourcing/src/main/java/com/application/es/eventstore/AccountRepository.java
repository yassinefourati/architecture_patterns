package com.application.es.eventstore;

import com.application.es.domain.Account;
import com.application.es.domain.AccountEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AccountRepository {

    private final EventStore eventStore;

    public AccountRepository(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public Optional<Account> findById(UUID id) {
        List<AccountEvent> history = eventStore.loadStream(id);
        if (history.isEmpty()) return Optional.empty();
        return Optional.of(Account.rehydrate(history));
    }

    /** Append uncommitted events and clear them from the aggregate. */
    public void save(Account account) {
        List<AccountEvent> newEvents = account.pullUncommitted();
        if (newEvents.isEmpty()) return;
        long expectedVersion = account.getVersion() - newEvents.size();
        eventStore.append(account.getId(), expectedVersion, newEvents);
    }
}
