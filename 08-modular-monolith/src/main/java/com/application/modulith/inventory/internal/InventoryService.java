package com.application.modulith.inventory.internal;

import com.application.modulith.inventory.InventoryApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class InventoryService implements InventoryApi {

    private final InventoryJpaRepository repository;

    InventoryService(InventoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public StockStatus checkStock(String productCode) {
        return repository.findById(productCode)
            .map(item -> new StockStatus(item.getProductCode(), item.getQuantityOnHand(), item.getQuantityOnHand() > 0))
            .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + productCode));
    }

    @Override
    public void reserve(String productCode, int quantity) {
        InventoryEntity entity = repository.findById(productCode)
            .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + productCode));
        entity.reserve(quantity);
    }

    @Configuration
    static class Seed {
        @Bean
        CommandLineRunner seed(InventoryJpaRepository repository) {
            return args -> {
                repository.save(new InventoryEntity("WIDGET-1", 10));
                repository.save(new InventoryEntity("WIDGET-2", 5));
                repository.save(new InventoryEntity("WIDGET-3", 0));
            };
        }
    }
}
