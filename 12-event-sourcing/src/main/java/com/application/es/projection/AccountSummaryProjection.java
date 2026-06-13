package com.application.es.projection;

import com.application.es.domain.AccountEvent;
import com.application.es.eventstore.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Polls the event store and folds events into the account_summary read model.
 *
 * Why polling? Because the event store IS the source of truth, and we want
 * exactly-once projection guarantees. The projection_offset table tracks where
 * we are; even on JVM crash, we resume from the last committed offset.
 *
 * In production: use Postgres LISTEN/NOTIFY, Debezium CDC, or Kafka instead
 * of polling â€” but the algorithm is identical.
 */
@Component
public class AccountSummaryProjection {

    private static final Logger log = LoggerFactory.getLogger(AccountSummaryProjection.class);
    private static final String NAME = "account_summary";
    private static final int BATCH_SIZE = 100;

    private final EventStore eventStore;
    private final JdbcTemplate jdbc;

    public AccountSummaryProjection(EventStore eventStore, JdbcTemplate jdbc) {
        this.eventStore = eventStore;
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelay = 200)
    @Transactional
    public void tick() {
        Long lastSeq = jdbc.queryForObject(
            "SELECT last_processed_seq FROM projection_offset WHERE projection_name = ?",
            Long.class, NAME);
        if (lastSeq == null) lastSeq = 0L;

        List<EventStore.StoredEvent> batch = eventStore.loadAfter(lastSeq, BATCH_SIZE);
        if (batch.isEmpty()) return;

        for (EventStore.StoredEvent stored : batch) {
            handle(stored.event(), stored.sequenceNo());
        }

        long newOffset = batch.get(batch.size() - 1).sequenceNo();
        jdbc.update("UPDATE projection_offset SET last_processed_seq = ? WHERE projection_name = ?", newOffset, NAME);
        log.debug("[projection] advanced to {} ({} events processed)", newOffset, batch.size());
    }

    private void handle(AccountEvent event, long seq) {
        switch (event) {
            case AccountEvent.AccountOpened e -> jdbc.update("""
                MERGE INTO account_summary (account_id, holder, balance, closed, last_event_seq, updated_at)
                KEY(account_id) VALUES (?, ?, ?, FALSE, ?, ?)
                """,
                e.accountId(), e.holder(), e.openingBalance(), seq, Timestamp.from(Instant.now()));

            case AccountEvent.MoneyDeposited e -> jdbc.update("""
                UPDATE account_summary SET balance = balance + ?, last_event_seq = ?, updated_at = ?
                WHERE account_id = ?
                """,
                e.amount(), seq, Timestamp.from(Instant.now()), e.accountId());

            case AccountEvent.MoneyWithdrawn e -> jdbc.update("""
                UPDATE account_summary SET balance = balance - ?, last_event_seq = ?, updated_at = ?
                WHERE account_id = ?
                """,
                e.amount(), seq, Timestamp.from(Instant.now()), e.accountId());

            case AccountEvent.AccountClosed e -> jdbc.update("""
                UPDATE account_summary SET closed = TRUE, last_event_seq = ?, updated_at = ?
                WHERE account_id = ?
                """,
                seq, Timestamp.from(Instant.now()), e.accountId());
        }
    }
}
