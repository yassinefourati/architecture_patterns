package com.application.es.web;

import com.application.es.domain.Account;
import com.application.es.domain.AccountEvent;
import com.application.es.eventstore.AccountRepository;
import com.application.es.eventstore.EventStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository repository;
    private final EventStore eventStore;
    private final JdbcTemplate jdbc;

    public AccountController(AccountRepository repository, EventStore eventStore, JdbcTemplate jdbc) {
        this.repository = repository;
        this.eventStore = eventStore;
        this.jdbc = jdbc;
    }

    // === COMMANDS ===

    @PostMapping
    @Transactional
    public ResponseEntity<AccountIdResponse> open(@Valid @RequestBody OpenRequest request) {
        UUID id = UUID.randomUUID();
        Account account = Account.open(id, request.holder(), request.openingBalance());
        repository.save(account);
        return ResponseEntity.ok(new AccountIdResponse(id));
    }

    @PostMapping("/{id}/deposits")
    @Transactional
    public ResponseEntity<Void> deposit(@PathVariable UUID id, @Valid @RequestBody AmountRequest request) {
        Account account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.deposit(request.amount());
        repository.save(account);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/withdrawals")
    @Transactional
    public ResponseEntity<Void> withdraw(@PathVariable UUID id, @Valid @RequestBody AmountRequest request) {
        Account account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.withdraw(request.amount());
        repository.save(account);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    @Transactional
    public ResponseEntity<Void> close(@PathVariable UUID id, @Valid @RequestBody CloseRequest request) {
        Account account = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.close(request.reason());
        repository.save(account);
        return ResponseEntity.noContent().build();
    }

    // === QUERIES ===

    /** Read from the projection (eventually consistent). */
    @GetMapping("/{id}")
    public AccountSummaryView find(@PathVariable UUID id) {
        return jdbc.queryForObject("""
            SELECT account_id, holder, balance, closed, updated_at
            FROM account_summary WHERE account_id = ?
            """,
            (rs, rowNum) -> new AccountSummaryView(
                (UUID) rs.getObject("account_id"),
                rs.getString("holder"),
                rs.getBigDecimal("balance"),
                rs.getBoolean("closed"),
                rs.getTimestamp("updated_at").toInstant()
            ),
            id
        );
    }

    /** Replay the event stream â€” the entire history of changes. */
    @GetMapping("/{id}/history")
    public List<AccountEvent> history(@PathVariable UUID id) {
        return eventStore.loadStream(id);
    }

    // === ERRORS ===

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleNotFound(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleInvariant(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(EventStore.ConcurrencyException.class)
    public ProblemDetail handleConcurrency(EventStore.ConcurrencyException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    public record OpenRequest(@NotBlank String holder, @NotNull @DecimalMin("0.0") BigDecimal openingBalance) {}
    public record AmountRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {}
    public record CloseRequest(@NotBlank String reason) {}
    public record AccountIdResponse(UUID id) {}
    public record AccountSummaryView(UUID id, String holder, BigDecimal balance, boolean closed, Instant updatedAt) {}
}
