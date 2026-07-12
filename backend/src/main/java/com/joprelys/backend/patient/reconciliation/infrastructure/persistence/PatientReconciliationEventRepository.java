package com.joprelys.backend.patient.reconciliation.infrastructure.persistence;

import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Append-only access to reconciliation evidence.
 *
 * <p>Delete and update operations are deliberately not exposed. A wrong decision is
 * corrected by recording a new event and updating only the active canonical link.</p>
 */
public interface PatientReconciliationEventRepository
        extends Repository<PatientReconciliationEventEntity, UUID> {

    <S extends PatientReconciliationEventEntity> S saveAndFlush(S entity);

    Optional<PatientReconciliationEventEntity> findByIdempotencyKey(String idempotencyKey);

    List<PatientReconciliationEventEntity> findAllBySourcePatient_IdOrderByCreatedAtDesc(UUID sourcePatientId);

    boolean existsBySourcePatient_IdAndDecision(
            UUID sourcePatientId,
            PatientReconciliationDecision decision);
}