package com.joprelys.backend.visit.api;

import java.time.Instant;

public record DocumentVerificationResponse(
        String documentNumber,
        String status,
        String documentType,
        String clinicName,
        String doctorName,
        String serviceName,
        String patientName,
        Instant issuedAt,
        String legalNotice
) {}
