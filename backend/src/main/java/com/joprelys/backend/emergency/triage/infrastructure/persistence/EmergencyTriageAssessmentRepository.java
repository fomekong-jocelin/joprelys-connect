package com.joprelys.backend.emergency.triage.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyTriageAssessmentRepository
        extends JpaRepository<EmergencyTriageAssessmentEntity, UUID> {

    List<EmergencyTriageAssessmentEntity> findByEmergency_IdOrderBySequenceNumberAsc(UUID emergencyId);

    Optional<EmergencyTriageAssessmentEntity> findByIdAndEmergency_Id(UUID id, UUID emergencyId);

    boolean existsByEmergency_Id(UUID emergencyId);

    @Query("SELECT COALESCE(MAX(a.sequenceNumber), 0) "
            + "FROM EmergencyTriageAssessmentEntity a WHERE a.emergency.id = :emergencyId")
    int findMaxSequenceNumber(@Param("emergencyId") UUID emergencyId);
}