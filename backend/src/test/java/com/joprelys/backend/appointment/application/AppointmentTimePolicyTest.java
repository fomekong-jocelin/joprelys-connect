package com.joprelys.backend.appointment.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AppointmentTimePolicyTest {

	private static final Instant APPOINTMENT_START = Instant.parse("2026-08-03T08:00:00Z");

	@Test
	void shouldAllowCancellationBeforeDeadline() {
		assertTrue(AppointmentTimePolicy.canPatientCancel(
				APPOINTMENT_START,
				Instant.parse("2026-08-02T07:59:59Z"),
				24));
	}

	@Test
	void shouldAllowCancellationAtExactDeadline() {
		assertTrue(AppointmentTimePolicy.canPatientCancel(
				APPOINTMENT_START,
				Instant.parse("2026-08-02T08:00:00Z"),
				24));
	}

	@Test
	void shouldRejectCancellationAfterDeadline() {
		assertFalse(AppointmentTimePolicy.canPatientCancel(
				APPOINTMENT_START,
				Instant.parse("2026-08-02T08:00:00.001Z"),
				24));
	}
}
