package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.application.PatientService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyPatientMergeCoordinator {

    private final LegacyPatientMergeGuard mergeGuard;
    private final PatientService patientService;

    public LegacyPatientMergeCoordinator(
            LegacyPatientMergeGuard mergeGuard,
            PatientService patientService) {
        this.mergeGuard = mergeGuard;
        this.patientService = patientService;
    }

    @Transactional
    public void merge(UUID primaryId, UUID secondaryId, UUID actorId) {
        mergeGuard.lockAndValidate(primaryId, secondaryId);
        patientService.mergePatients(primaryId, secondaryId, actorId);
    }
}
