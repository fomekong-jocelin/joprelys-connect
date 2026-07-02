package com.joprelys.backend.visit.api;

import java.time.Instant;

public record DocumentVerificationResponse(
        String documentNumber,
        String status,
        String clinicName,
        String doctorName,
        String patientName,
        Instant issuedAt
) {}
