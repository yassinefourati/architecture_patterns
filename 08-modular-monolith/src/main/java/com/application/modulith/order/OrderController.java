package com.application.modulith.order;

import com.application.modulith.order.internal.OrderApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderApplicationService service;

    public OrderController(OrderApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceRequest request) {
        UUID id = service.placeOrder(request.productCode(), request.quantity(), request.customerEmail());
        return ResponseEntity.ok(new OrderResponse(id, "CONFIRMED"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleInvariant(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleNotFound(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    public record PlaceRequest(@NotBlank String productCode, @Min(1) int quantity, @Email String customerEmail) {}
    public record OrderResponse(UUID id, String status) {}
}
