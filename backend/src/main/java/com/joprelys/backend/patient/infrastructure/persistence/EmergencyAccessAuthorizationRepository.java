package com.joprelys.backend.patient.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface EmergencyAccessAuthorizationRepository extends JpaRepository<EmergencyAccessAuthorizationEntity, UUID> {
    List<EmergencyAccessAuthorizationEntity> findByPatientId(UUID patientId);

    Optional<EmergencyAccessAuthorizationEntity> findByPatientIdAndOrganizationIdAndExpiresAtAfter(
        UUID patientId,
        UUID organizationId,
        Instant now
    );
}