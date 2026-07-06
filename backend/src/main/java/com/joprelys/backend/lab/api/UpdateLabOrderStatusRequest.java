package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLabOrderStatusRequest(
		@NotNull(message = "Le statut est obligatoire.")
		LabOrderStatus status
) {}
