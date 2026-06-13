# 05 — CQRS (Command Query Responsibility Segregation)

Separate write model from read model. Writes go through a JPA aggregate that protects invariants; reads pull from a denormalized view table optimized for display.

## Structure

```
com.example.cqrs
├── command/             WRITE side
│   ├── Book.java                JPA entity with invariants
│   ├── BookCommand.java         Sealed command hierarchy
│   ├── BookCommandHandler.java  Handles each command type
│   ├── BookChanged.java         Internal event
│   └── BookRepository.java
├── query/               READ side
│   ├── BookCatalogView.java     Denormalized record
│   └── BookQueryService.java    Uses JdbcTemplate against the view table
├── projection/          GLUE
│   └── BookCatalogProjector.java   Listens to BookChanged, upserts the view
└── web/
    └── BookController.java      /api/books (writes), /api/catalog (reads)
```

## Run

```bash
mvn spring-boot:run
```

Port **8085**.

## Try it

```bash
# Write: create a book
curl -X POST http://localhost:8085/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Effective Java","author":"Joshua Bloch","price":42.50,"initialStock":10}'

# Read: list (note the precomputed displayLabel and formattedPrice)
curl http://localhost:8085/api/catalog

# Read: search by author
curl "http://localhost:8085/api/catalog?author=bloch"

# Write: change price (read model updates after commit)
BOOK_ID=$(curl -s http://localhost:8085/api/catalog | jq -r '.[0].bookId')
curl -X PUT http://localhost:8085/api/books/$BOOK_ID/price \
  -H "Content-Type: application/json" \
  -d '{"newPrice":39.99}'

# Read again — formattedPrice is updated
curl http://localhost:8085/api/catalog/$BOOK_ID

# Write: adjust stock down to zero, then try to overdraw — 422
curl -X POST http://localhost:8085/api/books/$BOOK_ID/stock \
  -H "Content-Type: application/json" \
  -d '{"delta":-100}'
```

## What to notice

- **Different models for different jobs**: `Book` (write) enforces "stock can't go negative"; `BookCatalogView` (read) carries precomputed `displayLabel`, `formattedPrice`, `inStock` for fast UI rendering.
- **No JPA on the read side** — `BookQueryService` uses `JdbcTemplate` directly against `book_catalog_view`. No mapping overhead, no lazy-loading surprises.
- **`BookCatalogProjector`** is the bridge: listens to `BookChanged` events `AFTER_COMMIT`, then upserts the view row. The read side is **eventually consistent** with the write side — even at this in-process scale.
- **Sealed commands** (`BookCommand permits ...`) let the compiler enforce exhaustive handling.
- **Scaling up**: the write store could be Postgres; the read store could be Elasticsearch or Redis. The projector would consume Kafka events instead of in-process ones. The boundaries are already in place.
