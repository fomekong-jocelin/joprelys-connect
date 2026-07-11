package com.joprelys.backend.patient.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientIdentityDeclarationRepository
        extends JpaRepository<PatientIdentityDeclarationEntity, UUID> {

    List<PatientIdentityDeclarationEntity> findAllByPatientIdOrderByDeclaredAtAsc(UUID patientId);
}
