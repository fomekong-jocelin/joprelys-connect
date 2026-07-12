package com.joprelys.backend.patient.reconciliation.api;

import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import java.time.Instant;
import java.util.UUID;

public record PatientReconciliationQueueItemResponse(
        UUID patientId,
        String temporaryPatientNumber,
        String displayName,
        PatientIdentityStatus identityStatus,
        IdentityConfidenceLevel confidenceLevel,
        String apparentGender,
        String estimatedAgeRange,
        Instant foundAt,
        String foundLocation,
        Instant createdAt,
        UUID canonicalPatientId,
        UUID decisionEventId,
        PatientReconciliationDecision decision,
        boolean terminal) {
}
