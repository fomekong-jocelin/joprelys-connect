package com.joprelys.backend.patient.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientAllergyRepository extends JpaRepository<PatientAllergyEntity, UUID> {
    List<PatientAllergyEntity> findAllByPatientId(UUID patientId);
    Optional<PatientAllergyEntity> findByIdAndPatientId(UUID id, UUID patientId);
    List<PatientAllergyEntity> findAllByPatientIdAndDeletedAtIsNull(UUID patientId);
    Optional<PatientAllergyEntity> findByIdAndPatientIdAndDeletedAtIsNull(UUID id, UUID patientId);
}
