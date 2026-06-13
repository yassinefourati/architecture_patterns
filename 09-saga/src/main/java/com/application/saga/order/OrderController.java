package com.application.saga.order;

import com.application.saga.orchestrator.OrderSagaContext;
import com.application.saga.orchestrator.OrderSagaOrchestrator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderSagaOrchestrator orchestrator;

    public OrderController(OrderSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody OrderRequest request) {
        OrderSagaContext ctx = new OrderSagaContext(
            request.customerId(),
            request.productCode(),
            request.quantity(),
            request.amount(),
            request.shippingAddress()
        );
        UUID sagaId = orchestrator.execute(ctx);
        return ResponseEntity.ok(new OrderResponse(sagaId, ctx.paymentId(), ctx.reservationId(), ctx.shipmentId()));
    }

    @ExceptionHandler(OrderSagaOrchestrator.SagaFailedException.class)
    public ProblemDetail handleSagaFailed(OrderSagaOrchestrator.SagaFailedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    public record OrderRequest(
        @NotBlank String customerId,
        @NotBlank String productCode,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String shippingAddress
    ) {
    	
    }

    public record OrderResponse(UUID sagaId, UUID paymentId, UUID reservationId, UUID shipmentId) {
    	
    }
}
