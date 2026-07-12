package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PatientPairLockService {

    private final PatientRepository patientRepository;

    public PatientPairLockService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public LockedPatientPair lock(UUID leftPatientId, UUID rightPatientId) {
        if (leftPatientId.equals(rightPatientId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PATIENT_PAIR_MUST_BE_DISTINCT");
        }

        UUID firstLockId = leftPatientId.compareTo(rightPatientId) < 0
                ? leftPatientId
                : rightPatientId;
        UUID secondLockId = firstLockId.equals(leftPatientId)
                ? rightPatientId
                : leftPatientId;

        PatientEntity firstLocked = requireForUpdate(firstLockId);
        PatientEntity secondLocked = requireForUpdate(secondLockId);
        PatientEntity left = leftPatientId.equals(firstLockId) ? firstLocked : secondLocked;
        PatientEntity right = rightPatientId.equals(firstLockId) ? firstLocked : secondLocked;
        return new LockedPatientPair(left, right);
    }

    private PatientEntity requireForUpdate(UUID patientId) {
        return patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND"));
    }

    public record LockedPatientPair(PatientEntity left, PatientEntity right) {
    }
}
