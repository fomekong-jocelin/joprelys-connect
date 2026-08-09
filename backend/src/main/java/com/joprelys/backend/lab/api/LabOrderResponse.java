package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.infrastructure.persistence.ExamType;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
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
		ExamType examType,
		List<String> exams,
		List<LabOrderItemResponse> items,
		String reason,
		String priority,
		LabOrderStatus status,
		Instant createdAt
) {}
