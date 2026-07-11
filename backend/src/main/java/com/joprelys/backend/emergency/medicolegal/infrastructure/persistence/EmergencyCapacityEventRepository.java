package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyCapacityEventRepository extends JpaRepository<EmergencyCapacityEventEntity, UUID> {
    List<EmergencyCapacityEventEntity> findByEmergencyIdOrderByEffectiveAtAsc(UUID emergencyId);
    Optional<EmergencyCapacityEventEntity> findFirstByEmergencyIdOrderByEffectiveAtDesc(UUID emergencyId);
}
