package com.joprelys.backend.clinic.api;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
		UUID id,
		String name,
		String email,
		String phone,
		String address,
		String city,
		String logoPath,
		String status,
		Instant createdAt,
		String adminEmail,
		String adminDisplayName,
		String country,
		String type,
		String responsibleName,
		boolean apiEnabled) {
}
