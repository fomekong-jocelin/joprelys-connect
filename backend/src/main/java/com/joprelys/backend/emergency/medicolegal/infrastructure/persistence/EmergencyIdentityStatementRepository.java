package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyIdentityStatementRepository extends JpaRepository<EmergencyIdentityStatementEntity, UUID> {
    List<EmergencyIdentityStatementEntity> findByEmergencyIdOrderByDeclaredAtAsc(UUID emergencyId);
}
