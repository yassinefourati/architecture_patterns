CREATE TABLE IF NOT EXISTS stocks (
    symbol VARCHAR(10) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    last_price DECIMAL(10, 2) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO stocks (symbol, name, last_price, updated_at) VALUES
    ('AAPL', 'Apple Inc.', 175.00, CURRENT_TIMESTAMP),
    ('GOOG', 'Alphabet Inc.', 140.00, CURRENT_TIMESTAMP),
    ('MSFT', 'Microsoft Corp.', 410.00, CURRENT_TIMESTAMP),
    ('NVDA', 'NVIDIA Corp.', 880.00, CURRENT_TIMESTAMP),
    ('TSLA', 'Tesla Inc.', 175.00, CURRENT_TIMESTAMP);
