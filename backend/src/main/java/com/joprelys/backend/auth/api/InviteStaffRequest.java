package com.joprelys.backend.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteStaffRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 3) String displayName,
		@NotBlank String role
) {
}
