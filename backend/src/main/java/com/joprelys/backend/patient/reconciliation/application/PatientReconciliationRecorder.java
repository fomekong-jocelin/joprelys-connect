package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.patient.domain.IdentitySourceType;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientIdentityStatusHistoryEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientIdentityStatusHistoryRepository;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PatientReconciliationRecorder {

    private final PatientReconciliationEventRepository eventRepository;
    private final PatientIdentityStatusHistoryRepository statusHistoryRepository;
    private final AuditService auditService;

    public PatientReconciliationRecorder(
            PatientReconciliationEventRepository eventRepository,
            PatientIdentityStatusHistoryRepository statusHistoryRepository,
            AuditService auditService) {
        this.eventRepository = eventRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.auditService = auditService;
    }

    public PatientReconciliationEventEntity record(
            UserAccountEntity actor,
            PatientEntity source,
            PatientEntity candidate,
            PatientReconciliationDecision decision,
            PatientIdentityStatus previousStatus,
            PatientIdentityStatus resultingStatus,
            BigDecimal similarityScore,
            String matchReasons,
            IdentitySourceType evidenceSourceType,
            String evidenceReference,
            String justification,
            UUID correctedEventId,
            String idempotencyKey,
            String auditAction) {
        PatientReconciliationEventEntity event = eventRepository.saveAndFlush(
                new PatientReconciliationEventEntity(
                        actor.getOrganizationId(),
                        source,
                        candidate,
                        decision,
                        previousStatus,
                        resultingStatus,
                        similarityScore,
                        matchReasons,
                        evidenceSourceType,
                        evidenceReference,
                        justification,
                        correctedEventId,
                        idempotencyKey,
                        actor.getId()));

        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                source.getId(),
                "PATIENT_RECONCILIATION",
                event.getId(),
                auditAction,
                "Patient reconciliation decision recorded");
        return event;
    }

    public void recordIdentityStatusChange(
            UserAccountEntity actor,
            PatientEntity patient,
            PatientIdentityStatus previousStatus,
            PatientIdentityStatus newStatus,
            String reason) {
        statusHistoryRepository.save(new PatientIdentityStatusHistoryEntity(
                actor.getOrganizationId(),
                patient.getId(),
                previousStatus,
                newStatus,
                reason.trim(),
                actor.getId()));
    }
}
