package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import java.time.Instant;
import java.util.UUID;

public record LabOrderItemResponse(
		UUID id,
		String examName,
		LabOrderStatus status,
		Instant sampleCollectedAt,
		Instant resultAt,
		Instant validatedAt
) {}
