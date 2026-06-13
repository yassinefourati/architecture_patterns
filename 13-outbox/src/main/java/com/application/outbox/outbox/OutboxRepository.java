package com.application.outbox.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    /**
     * Lock pending rows so only one relay instance picks them up.
     * In Postgres this would use SKIP LOCKED for true concurrent relay processing.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM OutboxMessage m WHERE m.status = :status ORDER BY m.createdAt ASC")
    List<OutboxMessage> lockBatch(@Param("status") String status, org.springframework.data.domain.Pageable pageable);
}
