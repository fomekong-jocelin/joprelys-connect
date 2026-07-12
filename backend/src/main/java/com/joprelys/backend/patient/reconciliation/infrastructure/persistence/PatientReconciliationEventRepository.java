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

    Optional<PatientReconciliationEventEntity> findById(UUID id);

    Optional<PatientReconciliationEventEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<PatientReconciliationEventEntity> findFirstBySourcePatient_IdOrderByCreatedAtDesc(
            UUID sourcePatientId);

    Optional<PatientReconciliationEventEntity> findFirstBySourcePatient_IdAndDecisionOrderByCreatedAtDesc(
            UUID sourcePatientId,
            PatientReconciliationDecision decision);

    List<PatientReconciliationEventEntity> findAllBySourcePatient_IdOrderByCreatedAtDesc(UUID sourcePatientId);

    boolean existsBySourcePatient_IdAndDecision(
            UUID sourcePatientId,
            PatientReconciliationDecision decision);
}
