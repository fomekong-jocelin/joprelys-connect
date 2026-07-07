package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.infrastructure.persistence.PreRegistrationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientPreRegistrationResponse(
        UUID id,
        UUID organizationId,
        String firstName,
        String lastName,
        String gender,
        LocalDate birthDate,
        String bloodGroup,
        String phone,
        String email,
        String address,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelation,
        PreRegistrationStatus status,
        Instant createdAt,
        Double similarityScore,
        UUID similarPatientId,
        String similarPatientName
) {}
