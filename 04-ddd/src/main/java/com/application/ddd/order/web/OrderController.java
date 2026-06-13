package com.application.ddd.order.web;

import com.application.ddd.order.application.OrderApplicationService;
import com.application.ddd.order.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderApplicationService service;

    public OrderController(OrderApplicationService service) {
        this.service = service;
    }

    @PostMapping("/drafts")
    public ResponseEntity<OrderResponse> openDraft(@Valid @RequestBody OpenDraftRequest request) {
        OrderId id = service.openDraft(request.customerId());
        return ResponseEntity.created(URI.create("/api/orders/" + id.value()))
            .body(toResponse(service.find(id)));
    }

    @PostMapping("/{id}/lines")
    public OrderResponse addLine(@PathVariable UUID id, @Valid @RequestBody AddLineRequest request) {
        service.addLine(new OrderId(id), request.productId(), request.quantity(),
            new Money(request.unitPrice(), Money.USD));
        return toResponse(service.find(new OrderId(id)));
    }

    @PostMapping("/{id}/place")
    public OrderResponse place(@PathVariable UUID id) {
        service.place(new OrderId(id));
        return toResponse(service.find(new OrderId(id)));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancelRequest request) {
        service.cancel(new OrderId(id), request.reason());
        return toResponse(service.find(new OrderId(id)));
    }

    @GetMapping("/{id}")
    public OrderResponse find(@PathVariable UUID id) {
        return toResponse(service.find(new OrderId(id)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleNotFound(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleInvariant(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

	private OrderResponse toResponse(Order o) {
		return new OrderResponse(
				o.getId().value(), 
				o.getCustomerId().value(),
				o.getStatus().name(),
				o.total().amount(), 
				o.total().currency().getCurrencyCode());
	}

	public record OpenDraftRequest(@NotBlank String customerId) {
	}

	public record AddLineRequest(@NotBlank String productId, @Min(1) int quantity, @NotNull @DecimalMin("0.0") BigDecimal unitPrice) {
		
	}

	public record CancelRequest(@NotBlank String reason) {
		
	}

	public record OrderResponse(UUID id, String customerId, String status, BigDecimal total, String currency) {
		
	}
}
