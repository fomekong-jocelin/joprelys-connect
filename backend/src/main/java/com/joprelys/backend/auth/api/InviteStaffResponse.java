package com.joprelys.backend.auth.api;

import java.time.Instant;
import java.util.UUID;

public record InviteStaffResponse(
		UUID id,
		String email,
		String displayName,
		String role,
		boolean enabled,
		String temporaryPassword,
		Instant createdAt
) {
}
