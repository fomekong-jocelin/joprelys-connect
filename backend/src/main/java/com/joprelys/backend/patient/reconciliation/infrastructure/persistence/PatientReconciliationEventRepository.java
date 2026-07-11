package com.joprelys.backend.patient.reconciliation.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientReconciliationEventRepository
        extends JpaRepository<PatientReconciliationEventEntity, UUID> {

    Optional<PatientReconciliationEventEntity> findByIdempotencyKey(String idempotencyKey);

    List<PatientReconciliationEventEntity> findAllBySourcePatient_IdOrderByCreatedAtDesc(UUID sourcePatientId);
}
