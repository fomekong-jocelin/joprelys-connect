package com.joprelys.backend.appointment.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** Requête de réservation du patient authentifié. */
public record PatientBookAppointmentRequest(
		@NotNull UUID doctorId,
		@NotNull Instant startAt,
		@Size(max = 2000) String reason
) {
}
