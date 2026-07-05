package com.joprelys.backend.patient.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientVaccinationRepository extends JpaRepository<PatientVaccinationEntity, UUID> {
    List<PatientVaccinationEntity> findAllByPatientId(UUID patientId);
    Optional<PatientVaccinationEntity> findByIdAndPatientId(UUID id, UUID patientId);
}
