package com.joprelys.backend.lab.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LabOrderResponse(
		UUID id,
		String examRequestNumber,
		UUID patientId,
		String patientName,
		UUID visitId,
		UUID requesterPractitionerId,
		String requesterPractitionerName,
		UUID sourceOrganizationId,
		UUID targetOrganizationId,
		String examType,
		List<String> exams,
		String reason,
		String priority,
		String status,
		Instant createdAt
) {}
