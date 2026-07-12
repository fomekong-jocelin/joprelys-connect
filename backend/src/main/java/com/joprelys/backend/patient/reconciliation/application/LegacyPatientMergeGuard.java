package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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

    @Transactional(propagation = Propagation.MANDATORY)
    public LegacyMergeParticipants lockAndValidate(UUID primaryId, UUID secondaryId) {
        if (primaryId.equals(secondaryId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PATIENT_LEGACY_MERGE_SELF_FORBIDDEN");
        }

        UUID firstId = primaryId.compareTo(secondaryId) < 0 ? primaryId : secondaryId;
        UUID secondId = firstId.equals(primaryId) ? secondaryId : primaryId;

        PatientEntity first = requirePatientForUpdate(firstId);
        PatientEntity second = requirePatientForUpdate(secondId);
        PatientEntity primary = primaryId.equals(firstId) ? first : second;
        PatientEntity secondary = secondaryId.equals(firstId) ? first : second;

        assertEligible(primary);
        assertEligible(secondary);
        assertSameTenant(primary, secondary);
        assertNotParticipatingInCanonicalLink(primaryId);
        assertNotParticipatingInCanonicalLink(secondaryId);
        return new LegacyMergeParticipants(primary, secondary);
    }

    private PatientEntity requirePatientForUpdate(UUID patientId) {
        return patientRepository.findByIdForUpdate(patientId)
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

    private static void assertSameTenant(PatientEntity primary, PatientEntity secondary) {
        if (!primary.getOrganizationId().equals(secondary.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND");
        }
    }

    private static ResponseStatusException reconciliationWorkflowRequired() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "PATIENT_RECONCILIATION_WORKFLOW_REQUIRED");
    }

    public record LegacyMergeParticipants(PatientEntity primary, PatientEntity secondary) {
    }
}
