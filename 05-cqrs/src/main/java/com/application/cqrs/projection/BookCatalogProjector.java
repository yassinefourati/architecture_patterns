package com.application.cqrs.projection;

import com.application.cqrs.command.Book;
import com.application.cqrs.command.BookChanged;
import com.application.cqrs.command.BookRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Projector: listens to writes and updates the denormalized read model.
 * Runs AFTER_COMMIT so the read model never lags behind a rolled-back transaction.
 *
 * In a distributed setup, this would consume events from Kafka and update a separate store.
 */
@Component
public class BookCatalogProjector {

    private static final NumberFormat USD = NumberFormat.getCurrencyInstance(Locale.US);

    private final BookRepository bookRepository;
    private final JdbcTemplate jdbc;

    public BookCatalogProjector(BookRepository bookRepository, JdbcTemplate jdbc) {
        this.bookRepository = bookRepository;
        this.jdbc = jdbc;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookChanged event) {
        Book book = bookRepository.findById(event.bookId()).orElse(null);
        if (book == null) return;

        String label = book.getTitle() + " â€” " + book.getAuthor();
        String formattedPrice = USD.format(book.getPrice());

        // Upsert (MERGE in H2)
        jdbc.update("""
            MERGE INTO book_catalog_view (book_id, title, author, display_label, price, formatted_price, stock, in_stock, updated_at)
            KEY(book_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            book.getId(), book.getTitle(), book.getAuthor(), label,
            book.getPrice(), formattedPrice, book.getStock(),
            book.getStock() > 0, java.sql.Timestamp.from(book.getUpdatedAt())
        );
    }
}
