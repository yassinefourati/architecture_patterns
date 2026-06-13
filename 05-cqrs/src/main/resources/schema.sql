-- Read-side denormalized view (manually maintained in this demo).
-- In production this could be a materialized view, a separate DB, Elasticsearch, etc.
CREATE TABLE book_catalog_view (
    book_id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    display_label VARCHAR(512) NOT NULL,   -- precomputed: "Title — Author"
    price DECIMAL(10, 2) NOT NULL,
    formatted_price VARCHAR(32) NOT NULL,  -- precomputed: "$19.99"
    stock INT NOT NULL,
    in_stock BOOLEAN NOT NULL,             -- precomputed: stock > 0
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_book_catalog_author ON book_catalog_view(author);
CREATE INDEX idx_book_catalog_in_stock ON book_catalog_view(in_stock);
