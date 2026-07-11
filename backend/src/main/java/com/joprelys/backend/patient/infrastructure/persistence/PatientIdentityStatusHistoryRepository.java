package com.joprelys.backend.patient.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientIdentityStatusHistoryRepository
        extends JpaRepository<PatientIdentityStatusHistoryEntity, UUID> {

    List<PatientIdentityStatusHistoryEntity> findAllByPatientIdOrderByChangedAtAsc(UUID patientId);
}
