package com.joprelys.backend.appointment.api;

import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentEntity;
import java.time.Instant;
import java.util.UUID;

/** Vue API d'un rendez-vous, sans colonne technique ni entité JPA exposée. */
public record AppointmentResponse(
		UUID id,
		UUID doctorId,
		String doctorDisplayName,
		UUID patientId,
		Instant startAt,
		Instant endAt,
		String status,
		String reason,
		Instant cancelledAt,
		String cancellationReason,
		UUID visitId,
		Instant reminderSentAt,
		Instant createdAt,
		Instant updatedAt
) {
	public static AppointmentResponse fromEntity(AppointmentEntity appointment) {
		return new AppointmentResponse(
				appointment.getId(),
				appointment.getDoctor().getId(),
				appointment.getDoctor().getDisplayName(),
				appointment.getPatient().getId(),
				appointment.getStartAt(),
				appointment.getEndAt(),
				appointment.getStatus().name(),
				appointment.getReason(),
				appointment.getCancelledAt(),
				appointment.getCancellationReason(),
				appointment.getVisit() == null ? null : appointment.getVisit().getId(),
				appointment.getReminderSentAt(),
				appointment.getCreatedAt(),
				appointment.getUpdatedAt());
	}
}
