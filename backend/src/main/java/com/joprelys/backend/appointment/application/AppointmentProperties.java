package com.joprelys.backend.appointment.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration du module rendez-vous.
 *
 * @param defaultSlotDurationMinutes durée par défaut d'un créneau de rendez-vous en minutes (RM-01)
 * @param bookingHorizonDays         horizon maximal de réservation en jours (RM-03)
 * @param patientCancelDeadlineHours délai minimal avant le rendez-vous pour une annulation patient, en heures (RM-05)
 * @param reminderHoursBefore        délai d'envoi du rappel e-mail avant le rendez-vous, en heures (STORY-2605)
 */
@ConfigurationProperties(prefix = "joprelys.appointments")
public record AppointmentProperties(
        int defaultSlotDurationMinutes,
        int bookingHorizonDays,
        int patientCancelDeadlineHours,
        int reminderHoursBefore
) {
}
