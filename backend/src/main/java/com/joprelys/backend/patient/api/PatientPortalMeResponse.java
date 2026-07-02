package com.joprelys.backend.patient.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientPortalMeResponse(
        UUID id,
        String globalPatientNumber,
        String localPatientNumber,
        String fullName,
        String gender,
        LocalDate birthDate,
        String phone,
        String city,
        String district,
        String address,
        String emergencyContactName,
        String emergencyContactPhone,
        String allergies,
        String medicalHistory,
        List<PatientPortalConsultation> consultations
) {
    public record PatientPortalConsultation(
            UUID visitId,
            String visitNumber,
            LocalDate visitDate,
            String doctorName,
            String clinicName,
            String diagnosis,
            UUID documentId,
            String documentStatus
    ) {
    }
}
