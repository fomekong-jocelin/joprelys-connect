package com.joprelys.backend.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStaffRequest(
		@NotBlank @Size(min = 3) String displayName,
		@NotBlank String role
) {
}
