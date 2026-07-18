package com.joprelys.backend.appointment.application;

import java.time.Duration;
import java.time.Instant;

/** Règles temporelles pures et testables du parcours de rendez-vous. */
public final class AppointmentTimePolicy {

	private AppointmentTimePolicy() {
	}

	/** La limite exacte reste autorisée ; seule une date courante strictement postérieure est refusée. */
	public static boolean canPatientCancel(
			Instant appointmentStart,
			Instant now,
			int deadlineHours) {
		if (appointmentStart == null || now == null || deadlineHours <= 0) {
			return false;
		}
		Instant deadline = appointmentStart.minus(Duration.ofHours(deadlineHours));
		return !now.isAfter(deadline);
	}
}
