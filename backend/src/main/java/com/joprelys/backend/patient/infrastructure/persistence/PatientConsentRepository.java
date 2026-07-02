package com.joprelys.backend.patient.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientConsentRepository extends JpaRepository<PatientConsentEntity, UUID> {
    Optional<PatientConsentEntity> findByPatientIdAndOrganizationId(UUID patientId, UUID organizationId);
    List<PatientConsentEntity> findByPatientId(UUID patientId);
}
