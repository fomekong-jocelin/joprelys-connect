package com.joprelys.backend.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
