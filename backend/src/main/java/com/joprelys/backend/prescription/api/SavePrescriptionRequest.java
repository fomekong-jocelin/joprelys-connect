package com.joprelys.backend.prescription.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SavePrescriptionRequest(
		@NotNull
		@Size(min = 1, message = "La prescription doit contenir au moins un médicament.")
		List<@Valid PrescriptionItemRequest> items
) {}
