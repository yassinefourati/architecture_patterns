package com.application.outbox.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InboxRepository extends JpaRepository<InboxRecord, UUID> {
}
