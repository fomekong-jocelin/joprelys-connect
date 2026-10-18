package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLabOrderStatusRequest(
		@NotNull(message = "Le statut est obligatoire.")
		LabOrderStatus status,
		@jakarta.validation.constraints.Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères.")
		String reason
) {
	public UpdateLabOrderStatusRequest(LabOrderStatus status) {
		this(status, null);
	}
}
