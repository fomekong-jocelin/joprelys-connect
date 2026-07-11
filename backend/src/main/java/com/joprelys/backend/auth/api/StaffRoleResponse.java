package com.joprelys.backend.auth.api;

public record StaffRoleResponse(
		String code,
		String labelKey,
		String descriptionKey,
		String category,
		boolean sensitive) {
}
