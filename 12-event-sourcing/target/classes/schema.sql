-- The single source of truth: every state change is an append here.
CREATE TABLE event_store (
    sequence_no BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    payload CLOB NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    UNIQUE (aggregate_id, aggregate_version)   -- optimistic concurrency: two writers cannot insert v3 for the same aggregate
);

CREATE INDEX idx_event_store_aggregate ON event_store(aggregate_id, aggregate_version);
CREATE INDEX idx_event_store_sequence ON event_store(sequence_no);

-- Read model: derived from events by AccountSummaryProjection
CREATE TABLE account_summary (
    account_id UUID PRIMARY KEY,
    holder VARCHAR(255) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    closed BOOLEAN NOT NULL,
    last_event_seq BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Tracks the projection's position in the event stream
CREATE TABLE projection_offset (
    projection_name VARCHAR(100) PRIMARY KEY,
    last_processed_seq BIGINT NOT NULL
);

INSERT INTO projection_offset (projection_name, last_processed_seq) VALUES ('account_summary', 0);
