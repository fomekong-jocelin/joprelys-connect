package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyBelongingTransferRepository extends JpaRepository<EmergencyBelongingTransferEntity, UUID> {
    List<EmergencyBelongingTransferEntity> findByBelongingEmergencyIdOrderByOccurredAtAsc(UUID emergencyId);
}
