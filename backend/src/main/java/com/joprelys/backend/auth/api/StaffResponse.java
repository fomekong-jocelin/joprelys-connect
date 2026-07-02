package com.joprelys.backend.auth.api;

import java.time.Instant;
import java.util.UUID;

public record StaffResponse(
		UUID id,
		String email,
		String displayName,
		String role,
		boolean enabled,
		Instant createdAt
) {
}
