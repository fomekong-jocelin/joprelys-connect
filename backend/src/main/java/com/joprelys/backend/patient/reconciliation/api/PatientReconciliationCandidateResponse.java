package com.joprelys.backend.patient.reconciliation.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientReconciliationCandidateResponse(
        UUID patientId,
        String globalPatientNumber,
        String localPatientNumber,
        String displayName,
        String gender,
        LocalDate birthDate,
        String phone,
        String city,
        BigDecimal score,
        List<String> reasons) {
}
