package com.joprelys.backend.clinic.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationRequest(
		@NotBlank String name,
		@NotBlank @Email String email,
		String phone,
		String address,
		@NotBlank String city,
		String country,
		String type,
		String responsibleName,
		Boolean apiEnabled) {

	public CreateOrganizationRequest(String name, String email, String phone, String address, String city) {
		this(name, email, phone, address, city, "Cameroun", "CLINIC", "Responsable", true);
	}
}
