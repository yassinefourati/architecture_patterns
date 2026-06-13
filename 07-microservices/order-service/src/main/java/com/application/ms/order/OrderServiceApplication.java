package com.application.ms.order;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}
}

/**
 * Inter-service call via Feign. Configured by URL â€” in production, use
 * service discovery (Eureka/K8s DNS).
 */
@FeignClient(name = "inventory-service", url = "${services.inventory.url}")
interface InventoryClient {

	@GetMapping("/api/inventory/{productCode}")
	InventoryResponse check(@PathVariable("productCode") String productCode);

	record InventoryResponse(String productCode, int quantityOnHand, boolean available) {
	}
}

@RestController
@RequestMapping("/api/orders")
class OrderController {

	private final OrderService service;

	OrderController(OrderService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<OrderResponse> place(@Valid @RequestBody OrderRequest request) {
		OrderEntity order = service.placeOrder(request.productCode(), request.quantity(), request.customerEmail());
		return ResponseEntity.ok(new OrderResponse(order.getId(), order.getStatus()));
	}

	@ExceptionHandler(OutOfStockException.class)
	ProblemDetail handleOutOfStock(OutOfStockException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
	}

	record OrderRequest(@NotBlank String productCode, @Min(1) int quantity, @Email String customerEmail) {
	}

	record OrderResponse(UUID id, String status) {
	}
}

@Service
@Transactional
class OrderService {

	private final OrderRepository repository;
	private final InventoryClient inventoryClient;

	OrderService(OrderRepository repository, InventoryClient inventoryClient) {
		this.repository = repository;
		this.inventoryClient = inventoryClient;
	}

	OrderEntity placeOrder(String productCode, int quantity, String customerEmail) {
		var stock = inventoryClient.check(productCode);
		if (stock.quantityOnHand() < quantity) {
			throw new OutOfStockException(productCode, stock.quantityOnHand(), quantity);
		}
		return repository.save(new OrderEntity(productCode, quantity, customerEmail));
	}
}

@Entity
@Table(name = "orders")
class OrderEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String productCode;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false)
	private String customerEmail;

	@Column(nullable = false)
	private String status;

	@Column(nullable = false)
	private Instant createdAt;

	protected OrderEntity() {
	}

	OrderEntity(String productCode, int quantity, String customerEmail) {
		this.id = UUID.randomUUID();
		this.productCode = productCode;
		this.quantity = quantity;
		this.customerEmail = customerEmail;
		this.status = "PLACED";
		this.createdAt = Instant.now();
	}

	UUID getId() {
		return id;
	}

	String getStatus() {
		return status;
	}
}

interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
}

class OutOfStockException extends RuntimeException {
	OutOfStockException(String productCode, int available, int requested) {
		super("Out of stock for " + productCode + ": available=" + available + ", requested=" + requested);
	}
}
