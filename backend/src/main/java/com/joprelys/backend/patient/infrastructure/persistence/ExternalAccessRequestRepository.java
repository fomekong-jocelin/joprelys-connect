package com.joprelys.backend.patient.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ExternalAccessRequestRepository extends JpaRepository<ExternalAccessRequestEntity, UUID> {
    boolean existsByPatientIdAndRequesterOrganizationIdAndStatusIn(UUID patientId, UUID requesterOrganizationId, Collection<String> statuses);
    List<ExternalAccessRequestEntity> findByPatientId(UUID patientId);
    List<ExternalAccessRequestEntity> findByPatientIdAndStatus(UUID patientId, String status);
}
