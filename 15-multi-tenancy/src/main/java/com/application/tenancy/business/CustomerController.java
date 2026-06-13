package com.application.tenancy.business;

import com.application.tenancy.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * The business code is completely UNAWARE of multi-tenancy.
 * No tenant_id columns, no filters, no special repository methods.
 *
 * Each request runs against the tenant's schema because the routing DataSource
 * silently delivers the right physical connection.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository repository;

    public CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateRequest request) {
        Customer saved = repository.save(new Customer(request.name(), request.email()));
        return ResponseEntity.ok(Map.of(
            "id", saved.getId(),
            "tenant", TenantContext.require()  // echo back so it's visible in the response
        ));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Map<String, Object> list() {
        List<Customer> customers = repository.findAll();
        return Map.of(
            "tenant", TenantContext.require(),
            "customers", customers.stream()
                .map(c -> Map.of("id", c.getId(), "name", c.getName(), "email", c.getEmail()))
                .toList()
        );
    }

    public record CreateRequest(@NotBlank String name, @Email String email) {}
}
