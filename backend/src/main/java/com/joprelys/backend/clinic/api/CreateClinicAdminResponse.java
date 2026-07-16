package com.joprelys.backend.clinic.api;

import java.time.Instant;
import java.util.UUID;

public record CreateClinicAdminResponse(
		UUID id,
		String email,
		String displayName,
		String role,
		boolean enabled,
		UUID organizationId,
		Instant createdAt
) {
}
