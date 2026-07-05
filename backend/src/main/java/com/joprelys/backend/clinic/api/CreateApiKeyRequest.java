package com.joprelys.backend.clinic.api;

import jakarta.validation.constraints.NotBlank;

public record CreateApiKeyRequest(
		@NotBlank String name) {
}
