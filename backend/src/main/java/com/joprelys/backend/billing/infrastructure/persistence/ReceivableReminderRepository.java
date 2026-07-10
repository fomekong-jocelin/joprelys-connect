package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReceivableReminderRepository extends JpaRepository<ReceivableReminderEntity, UUID> {
    List<ReceivableReminderEntity> findByReceivableIdOrderByCreatedAtDesc(UUID receivableId);
    List<ReceivableReminderEntity> findByReceivableIdIn(List<UUID> receivableIds);
}
