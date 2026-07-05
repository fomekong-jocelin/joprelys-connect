package com.joprelys.backend.patient.api;

import java.time.Instant;
import java.util.UUID;

public record PatientDuplicateCandidateResponse(
    UUID id,
    PatientResponse sourcePatient,
    PatientResponse targetPatient,
    double similarityScore,
    String status,
    Instant createdAt
) {}