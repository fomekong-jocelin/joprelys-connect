package com.joprelys.backend.patient.reconciliation.api;

import com.joprelys.backend.patient.domain.IdentitySourceType;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PatientReconciliationEventResponse(
        UUID eventId,
        PatientReconciliationDecision decision,
        UUID sourcePatientId,
        UUID candidatePatientId,
        PatientIdentityStatus previousIdentityStatus,
        PatientIdentityStatus resultingIdentityStatus,
        BigDecimal similarityScore,
        List<String> matchReasons,
        IdentitySourceType evidenceSourceType,
        String evidenceReference,
        String justification,
        UUID correctedEventId,
        UUID createdByUserId,
        Instant createdAt) {
}
