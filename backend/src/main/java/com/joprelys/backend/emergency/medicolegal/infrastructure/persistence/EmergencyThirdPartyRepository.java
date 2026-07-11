package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyThirdPartyRepository extends JpaRepository<EmergencyThirdPartyEntity, UUID> {

    @EntityGraph(attributePaths = "qualities")
    List<EmergencyThirdPartyEntity> findByEmergencyIdOrderByCreatedAtAsc(UUID emergencyId);
}
