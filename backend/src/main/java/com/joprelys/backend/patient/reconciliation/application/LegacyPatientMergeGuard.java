package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LegacyPatientMergeGuard {

    private final PatientRepository patientRepository;
    private final PatientCanonicalLinkRepository canonicalLinkRepository;

    public LegacyPatientMergeGuard(
            PatientRepository patientRepository,
            PatientCanonicalLinkRepository canonicalLinkRepository) {
        this.patientRepository = patientRepository;
        this.canonicalLinkRepository = canonicalLinkRepository;
    }

    @Transactional(readOnly = true)
    public void assertLegacyMergeAllowed(UUID primaryId, UUID secondaryId) {
        assertEligible(requirePatient(primaryId));
        assertEligible(requirePatient(secondaryId));
        assertNotParticipatingInCanonicalLink(primaryId);
        assertNotParticipatingInCanonicalLink(secondaryId);
    }

    private PatientEntity requirePatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND"));
    }

    private void assertNotParticipatingInCanonicalLink(UUID patientId) {
        boolean isSource = canonicalLinkRepository.findBySourcePatient_Id(patientId).isPresent();
        boolean isCanonicalTarget = !canonicalLinkRepository.findAllByCanonicalPatient_Id(patientId).isEmpty();
        if (isSource || isCanonicalTarget) {
            throw reconciliationWorkflowRequired();
        }
    }

    private static void assertEligible(PatientEntity patient) {
        boolean provisionalOrigin = patient.getTemporaryPatientNumber() != null;
        boolean reconciliationState = patient.getIdentityStatus() == PatientIdentityStatus.PROVISIONAL_URGENCY
                || patient.getIdentityStatus() == PatientIdentityStatus.DECLARED
                || patient.getIdentityStatus() == PatientIdentityStatus.MERGED;
        if (provisionalOrigin || reconciliationState) {
            throw reconciliationWorkflowRequired();
        }
    }

    private static ResponseStatusException reconciliationWorkflowRequired() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "PATIENT_RECONCILIATION_WORKFLOW_REQUIRED");
    }
}
