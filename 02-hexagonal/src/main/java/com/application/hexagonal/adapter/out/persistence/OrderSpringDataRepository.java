package com.application.hexagonal.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

interface OrderSpringDataRepository extends JpaRepository<OrderJpaEntity, UUID> {
	
}
