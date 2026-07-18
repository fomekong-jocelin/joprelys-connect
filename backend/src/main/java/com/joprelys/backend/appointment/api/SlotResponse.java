package com.joprelys.backend.appointment.api;

import com.joprelys.backend.appointment.application.AppointmentSlotGenerator;
import java.time.Instant;
import java.util.UUID;

/** Créneau réservable retourné au portail patient. */
public record SlotResponse(UUID doctorId, Instant startAt, Instant endAt) {
	public static SlotResponse fromSlot(UUID doctorId, AppointmentSlotGenerator.Slot slot) {
		return new SlotResponse(doctorId, slot.startAt(), slot.endAt());
	}
}
