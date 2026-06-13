package com.application.events.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceRequest request) {
        var items = request.items().stream()
            .map(i -> new OrderEntity.ItemEmbeddable(i.productCode(), i.quantity()))
            .toList();
        UUID id = service.place(request.customerEmail(), items);
        var order = service.find(id);
        return ResponseEntity.ok(new OrderResponse(order.getId(), order.getStatus()));
    }

    @GetMapping("/{id}")
    public OrderResponse find(@PathVariable UUID id) {
        var order = service.find(id);
        return new OrderResponse(order.getId(), order.getStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleNotFound(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

	public record PlaceRequest(@Email String customerEmail, @NotEmpty List<@Valid ItemDto> items) {
		
	}

	public record ItemDto(@NotBlank String productCode, @Min(1) int quantity) {
	}

	public record OrderResponse(UUID id, String status) {
	}
}
