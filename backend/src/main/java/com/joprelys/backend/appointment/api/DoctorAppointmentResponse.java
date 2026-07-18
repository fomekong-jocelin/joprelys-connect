package com.joprelys.backend.appointment.api;

import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentEntity;
import java.time.Instant;
import java.util.UUID;

/** Vue minimale d'un rendez-vous dans l'agenda personnel du médecin. */
public record DoctorAppointmentResponse(
		UUID id,
		UUID patientId,
		String patientDisplayName,
		String patientLocalNumber,
		Instant startAt,
		Instant endAt,
		String status,
		String reason,
		UUID visitId
) {
	public static DoctorAppointmentResponse fromEntity(AppointmentEntity appointment) {
		return new DoctorAppointmentResponse(
				appointment.getId(),
				appointment.getPatient().getId(),
				appointment.getPatient().getDisplayName(),
				appointment.getPatient().getLocalPatientNumber(),
				appointment.getStartAt(),
				appointment.getEndAt(),
				appointment.getStatus().name(),
				appointment.getReason(),
				appointment.getVisit() == null ? null : appointment.getVisit().getId());
	}
}
