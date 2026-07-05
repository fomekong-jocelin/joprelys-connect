package com.joprelys.backend.common.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<WebhookEntity, UUID> {
    List<WebhookEntity> findByOrganizationIdAndStatus(UUID organizationId, String status);
    List<WebhookEntity> findByStatus(String status);
}
