package com.joprelys.backend.patient.reconciliation.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.joprelys.backend.patient.domain.IdentitySourceType;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PatientReconciliationEventEntityTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Test
    void shouldRejectLinkDecisionWithoutCanonicalCandidate() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> event(null, PatientReconciliationDecision.LINK_EXISTING_DPU, null));

        assertEquals("PATIENT_RECONCILIATION_DECISION_SHAPE_INVALID", exception.getMessage());
    }

    @Test
    void shouldRejectNewDpuDecisionWithCandidate() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> event(patient("DPU-TARGET"), PatientReconciliationDecision.CREATE_NEW_DPU, null));

        assertEquals("PATIENT_RECONCILIATION_DECISION_SHAPE_INVALID", exception.getMessage());
    }

    @Test
    void shouldRejectCorrectionWithoutOriginalEventReference() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> event(patient("DPU-TARGET"), PatientReconciliationDecision.CORRECT_LINK, null));

        assertEquals("PATIENT_RECONCILIATION_DECISION_SHAPE_INVALID", exception.getMessage());
    }

    @Test
    void shouldAcceptValidLinkDecision() {
        assertDoesNotThrow(() -> event(
                patient("DPU-TARGET"),
                PatientReconciliationDecision.LINK_EXISTING_DPU,
                null));
    }

    private static PatientReconciliationEventEntity event(
            PatientEntity candidate,
            PatientReconciliationDecision decision,
            UUID correctedEventId) {
        return new PatientReconciliationEventEntity(
                ORGANIZATION_ID,
                patient("DPU-SOURCE"),
                candidate,
                decision,
                PatientIdentityStatus.VERIFIED,
                decision == PatientReconciliationDecision.LINK_EXISTING_DPU
                        ? PatientIdentityStatus.MERGED
                        : PatientIdentityStatus.VERIFIED,
                null,
                null,
                IdentitySourceType.DOCUMENT,
                "CNI-100001",
                "Décision vérifiée par un agent habilité.",
                correctedEventId,
                UUID.randomUUID().toString(),
                ACTOR_ID);
    }

    private static PatientEntity patient(String number) {
        return new PatientEntity(
                number,
                number.replace("DPU", "PAT"),
                "Nadège Maffock",
                "FEMININ",
                LocalDate.of(1994, 5, 10),
                "+237699000111",
                "Douala",
                "Akwa",
                "Rue 10",
                "Paul Maffock",
                "+237699000222",
                "",
                "");
    }
}