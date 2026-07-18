package com.joprelys.backend.appointment.api;

import jakarta.validation.constraints.Size;

/** Motif optionnel d'annulation par le patient. */
public record CancelAppointmentRequest(@Size(max = 255) String cancellationReason) {
}
