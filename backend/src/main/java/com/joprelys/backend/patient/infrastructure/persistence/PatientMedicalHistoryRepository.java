package com.joprelys.backend.patient.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientMedicalHistoryRepository extends JpaRepository<PatientMedicalHistoryEntity, UUID> {
    List<PatientMedicalHistoryEntity> findAllByPatientId(UUID patientId);
    Optional<PatientMedicalHistoryEntity> findByIdAndPatientId(UUID id, UUID patientId);
}
