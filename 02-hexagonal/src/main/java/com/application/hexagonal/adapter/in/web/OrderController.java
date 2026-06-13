package com.application.hexagonal.adapter.in.web;

import com.application.hexagonal.domain.model.OrderId;
import com.application.hexagonal.domain.model.OrderLine;
import com.application.hexagonal.domain.port.in.FindOrderUseCase;
import com.application.hexagonal.domain.port.in.PlaceOrderUseCase;
import com.application.hexagonal.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Driving adapter. The controller depends on the inbound port (interface),
 * NOT on a concrete service. This is what lets us swap implementations.
 */
@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final PlaceOrderUseCase placeOrder;
    private final FindOrderUseCase findOrder;

    OrderController(PlaceOrderUseCase placeOrder, FindOrderUseCase findOrder) {
        this.placeOrder = placeOrder;
        this.findOrder = findOrder;
    }

    @PostMapping
    ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        var lines = request.lines().stream()
            .map(l -> new OrderLine(l.productCode(), l.quantity(), l.unitPrice()))
            .toList();
        OrderId id = placeOrder.placeOrder(new PlaceOrderCommand(request.customerEmail(), lines));
        return ResponseEntity.created(URI.create("/api/orders/" + id.value()))
            .body(new OrderResponse(id.value(), request.customerEmail(), "PLACED"));
    }

    @GetMapping("/{id}")
    ResponseEntity<OrderResponse> find(@PathVariable UUID id) {
        return findOrder.findById(new OrderId(id))
            .map(o -> ResponseEntity.ok(new OrderResponse(o.getId().value(), o.getCustomerEmail(), o.getStatus().name())))
            .orElse(ResponseEntity.notFound().build());
    }

	record PlaceOrderRequest(@Email String customerEmail, @NotEmpty List<@Valid LineDto> lines) {
	}

	record LineDto(@NotBlank String productCode, @Min(1) int quantity,
			@NotNull @DecimalMin("0.0") BigDecimal unitPrice) {
	}

	record OrderResponse(UUID id, String customerEmail, String status) {
	}

}
