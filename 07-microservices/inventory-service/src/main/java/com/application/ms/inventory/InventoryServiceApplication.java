package com.application.ms.inventory;

import jakarta.persistence.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner seed(InventoryRepository repository) {
		return args -> {
			repository.save(new InventoryItem("WIDGET-1", 10));
			repository.save(new InventoryItem("WIDGET-2", 5));
			repository.save(new InventoryItem("WIDGET-3", 0));
		};
	}
}

@RestController
@RequestMapping("/api/inventory")
class InventoryController {

	private final InventoryRepository repository;

	InventoryController(InventoryRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/{productCode}")
	ResponseEntity<InventoryResponse> get(@PathVariable String productCode) {
		return repository
				.findById(productCode).map(item -> ResponseEntity.ok(new InventoryResponse(item.getProductCode(), item.getQuantityOnHand(), item.getQuantityOnHand() > 0)))
				.orElse(ResponseEntity.notFound().build());
	}

	record InventoryResponse(String productCode, int quantityOnHand, boolean available) {
	}
}

@Entity
@Table(name = "inventory")
class InventoryItem {

	@Id
	private String productCode;

	@Column(nullable = false)
	private int quantityOnHand;

	protected InventoryItem() {
	}

	InventoryItem(String productCode, int quantityOnHand) {
		this.productCode = productCode;
		this.quantityOnHand = quantityOnHand;
	}

	String getProductCode() {
		return productCode;
	}

	int getQuantityOnHand() {
		return quantityOnHand;
	}
}

interface InventoryRepository extends JpaRepository<InventoryItem, String> {
	
}
