package com.joprelys.backend.appointment.api;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import java.util.UUID;

/** Médecin visible dans l'annuaire du portail patient. */
public record DoctorDirectoryEntry(
		UUID doctorId,
		String displayName,
		String specialty,
		String department
) {
	public static DoctorDirectoryEntry fromEntity(UserAccountEntity doctor) {
		return new DoctorDirectoryEntry(
				doctor.getId(),
				doctor.getDisplayName(),
				doctor.getSpecialty(),
				doctor.getDepartment());
	}
}
