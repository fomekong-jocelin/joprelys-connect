package com.joprelys.backend.patient.reconciliation.api;

import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PatientReconciliationDecisionResponse(
        UUID eventId,
        PatientReconciliationDecision decision,
        UUID sourcePatientId,
        PatientIdentityStatus sourceIdentityStatus,
        UUID canonicalPatientId,
        String temporaryPatientNumber,
        Set<UUID> contributingPatientIds,
        Instant decidedAt,
        boolean replayed) {
}
