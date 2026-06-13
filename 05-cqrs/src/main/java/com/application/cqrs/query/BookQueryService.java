package com.application.cqrs.query;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookQueryService {

    private static final RowMapper<BookCatalogView> ROW_MAPPER = (rs, rowNum) -> new BookCatalogView(
        (UUID) rs.getObject("book_id"),
        rs.getString("display_label"),
        rs.getString("formatted_price"),
        rs.getInt("stock"),
        rs.getBoolean("in_stock"),
        rs.getTimestamp("updated_at").toInstant()
    );

    private final JdbcTemplate jdbc;

    public BookQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<BookCatalogView> findById(UUID bookId) {
        return jdbc.query("SELECT * FROM book_catalog_view WHERE book_id = ?", ROW_MAPPER, bookId)
            .stream().findFirst();
    }

    public List<BookCatalogView> listInStock() {
        return jdbc.query("SELECT * FROM book_catalog_view WHERE in_stock = TRUE ORDER BY updated_at DESC", ROW_MAPPER);
    }

    public List<BookCatalogView> searchByAuthor(String author) {
        return jdbc.query("SELECT * FROM book_catalog_view WHERE LOWER(author) LIKE LOWER(?) ORDER BY display_label",  ROW_MAPPER, "%" + author + "%");
    }
}
