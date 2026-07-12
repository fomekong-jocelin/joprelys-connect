package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationQueueItemResponse;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientReconciliationQueueService {

    private static final EnumSet<PatientIdentityStatus> QUEUE_STATUSES =
            EnumSet.allOf(PatientIdentityStatus.class);

    private final PatientRepository patientRepository;
    private final PatientCanonicalLinkRepository canonicalLinkRepository;
    private final PatientReconciliationEventRepository eventRepository;

    public PatientReconciliationQueueService(
            PatientRepository patientRepository,
            PatientCanonicalLinkRepository canonicalLinkRepository,
            PatientReconciliationEventRepository eventRepository) {
        this.patientRepository = patientRepository;
        this.canonicalLinkRepository = canonicalLinkRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<PatientReconciliationQueueItemResponse> listQueue() {
        return patientRepository
                .findAllByTemporaryPatientNumberIsNotNullAndIdentityStatusInOrderByCreatedAtAsc(QUEUE_STATUSES)
                .stream()
                .map(this::toQueueItem)
                .toList();
    }

    private PatientReconciliationQueueItemResponse toQueueItem(PatientEntity patient) {
        Optional<PatientCanonicalLinkEntity> activeLink =
                canonicalLinkRepository.findBySourcePatient_Id(patient.getId());
        Optional<PatientReconciliationEventEntity> createNewDpuEvent =
                eventRepository.findFirstBySourcePatient_IdAndDecisionOrderByCreatedAtDesc(
                        patient.getId(), PatientReconciliationDecision.CREATE_NEW_DPU);
        Optional<PatientReconciliationEventEntity> latestEvent =
                eventRepository.findFirstBySourcePatient_IdOrderByCreatedAtDesc(patient.getId());

        UUID canonicalPatientId = null;
        PatientReconciliationEventEntity stateEvent = latestEvent.orElse(null);
        boolean terminal = false;

        if (activeLink.isPresent()) {
            canonicalPatientId = activeLink.get().getCanonicalPatient().getId();
            stateEvent = activeLink.get().getDecisionEvent();
            terminal = true;
        } else if (createNewDpuEvent.isPresent()) {
            canonicalPatientId = patient.getId();
            stateEvent = createNewDpuEvent.get();
            terminal = true;
        }

        return new PatientReconciliationQueueItemResponse(
                patient.getId(),
                patient.getTemporaryPatientNumber(),
                patient.getDisplayName(),
                patient.getIdentityStatus(),
                patient.getIdentityConfidenceLevel(),
                patient.getApparentGender(),
                patient.getEstimatedAgeRange(),
                patient.getFoundAt(),
                patient.getFoundLocation(),
                patient.getCreatedAt(),
                canonicalPatientId,
                stateEvent == null ? null : stateEvent.getId(),
                stateEvent == null ? null : stateEvent.getDecision(),
                terminal);
    }
}
