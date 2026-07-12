package com.joprelys.backend.patient.reconciliation.api;

import com.joprelys.backend.patient.domain.IdentitySourceType;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PatientReconciliationDecisionRequest(
        @NotNull PatientReconciliationDecision decision,
        UUID candidatePatientId,
        @NotNull IdentitySourceType evidenceSourceType,
        @Size(max = 255) String evidenceReference,
        @NotBlank @Size(max = 1500) String justification) {
}
