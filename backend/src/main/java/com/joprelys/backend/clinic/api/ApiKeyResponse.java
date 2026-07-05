package com.joprelys.backend.clinic.api;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
		UUID id,
		String name,
		String prefix,
		String rawKey,
		String status,
		Instant createdAt,
		Instant revokedAt) {
}
