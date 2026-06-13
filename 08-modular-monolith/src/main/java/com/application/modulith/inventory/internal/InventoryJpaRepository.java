package com.application.modulith.inventory.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryJpaRepository extends JpaRepository<InventoryEntity, String> {
}
