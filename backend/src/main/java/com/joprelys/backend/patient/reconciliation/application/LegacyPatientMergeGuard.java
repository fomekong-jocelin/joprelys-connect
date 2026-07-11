package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LegacyPatientMergeGuard {

    private final PatientRepository patientRepository;

    public LegacyPatientMergeGuard(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public void assertLegacyMergeAllowed(UUID primaryId, UUID secondaryId) {
        assertEligible(requirePatient(primaryId));
        assertEligible(requirePatient(secondaryId));
    }

    private PatientEntity requirePatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND"));
    }

    private static void assertEligible(PatientEntity patient) {
        boolean provisionalOrigin = patient.getTemporaryPatientNumber() != null;
        boolean reconciliationState = patient.getIdentityStatus() == PatientIdentityStatus.PROVISIONAL_URGENCY
                || patient.getIdentityStatus() == PatientIdentityStatus.DECLARED
                || patient.getIdentityStatus() == PatientIdentityStatus.MERGED;
        if (provisionalOrigin || reconciliationState) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "PATIENT_RECONCILIATION_WORKFLOW_REQUIRED");
        }
    }
}
