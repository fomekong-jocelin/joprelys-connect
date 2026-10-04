package com.joprelys.backend.lead.dto;

import com.joprelys.backend.lead.domain.DemoRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record DemoRequestResponse(
    UUID id,
    String fullName,
    String organizationName,
    DemoRequestStatus status,
    Instant createdAt,
    String message
) {}
