package com.joprelys.backend.auth.api;

public record CurrentSessionResponse(
		String email,
		String name,
		String role) {
}
