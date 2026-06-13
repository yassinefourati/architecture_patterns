package com.application.cqrs.web;

import com.application.cqrs.command.BookCommand;
import com.application.cqrs.command.BookCommandHandler;
import com.application.cqrs.query.BookCatalogView;
import com.application.cqrs.query.BookQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
public class BookController {

    private final BookCommandHandler commands;
    private final BookQueryService queries;

    public BookController(BookCommandHandler commands, BookQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    // --- WRITE SIDE ---

    @PostMapping("/api/books")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateBookRequest request) {
        UUID id = commands.handle(new BookCommand.CreateBook(
            request.title(), request.author(), request.price(), request.initialStock()
        ));
        return ResponseEntity.created(URI.create("/api/books/" + id)).build();
    }

    @PutMapping("/api/books/{id}/price")
    public ResponseEntity<Void> changePrice(@PathVariable UUID id, @Valid @RequestBody ChangePriceRequest request) {
        commands.handle(new BookCommand.ChangePrice(id, request.newPrice()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/books/{id}/stock")
    public ResponseEntity<Void> adjustStock(@PathVariable UUID id, @Valid @RequestBody AdjustStockRequest request) {
        commands.handle(new BookCommand.AdjustStock(id, request.delta()));
        return ResponseEntity.noContent().build();
    }

    // --- READ SIDE ---

    @GetMapping("/api/catalog/{id}")
    public ResponseEntity<BookCatalogView> get(@PathVariable UUID id) {
        return queries.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/catalog")
    public List<BookCatalogView> list(@RequestParam(required = false) String author) {
        return author == null ? queries.listInStock() : queries.searchByAuthor(author);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleNotFound(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleInvariant(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

	public record CreateBookRequest(@NotBlank String title, 
			@NotBlank String author,
			@NotNull @DecimalMin("0.0") BigDecimal price, 
			@Min(0) int initialStock) {
		
	}

	public record ChangePriceRequest(@NotNull @DecimalMin("0.0") BigDecimal newPrice) {
		
	}

	public record AdjustStockRequest(int delta) {
		
	}
}
