package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyLegalBasisRepository extends JpaRepository<EmergencyLegalBasisEntity, UUID> {

    @EntityGraph(attributePaths = "coveredActs")
    List<EmergencyLegalBasisEntity> findByEmergencyIdOrderByStartsAtAsc(UUID emergencyId);
}
