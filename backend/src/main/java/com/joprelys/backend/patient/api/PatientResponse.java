package com.joprelys.backend.patient.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
		UUID id,
		UUID organizationId,
		String globalPatientNumber,
		String localPatientNumber,
		String fullName,
		String gender,
		LocalDate birthDate,
		String phone,
		String city,
		String district,
		String address,
		String emergencyContactName,
		String emergencyContactPhone,
		String allergies,
		String medicalHistory,
		String status,
		String bloodGroup,
		String email,
		Instant createdAt,
		Instant updatedAt,
		Boolean emergencyAccessActive
) {
}
