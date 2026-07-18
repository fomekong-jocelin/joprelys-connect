package com.joprelys.backend.appointment.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration du module rendez-vous.
 *
 * @param defaultSlotDurationMinutes durée par défaut d'un créneau de rendez-vous en minutes (RM-01)
 * @param bookingHorizonDays         horizon maximal de réservation en jours (RM-03)
 * @param patientCancelDeadlineHours délai minimal avant le rendez-vous pour une annulation patient, en heures (RM-05)
 * @param reminderHoursBefore        délai d'envoi du rappel e-mail avant le rendez-vous, en heures (STORY-2605)
 * @param timezone                   fuseau horaire de la clinique (identifiant IANA) utilisé pour découper les créneaux de disponibilité
 */
@ConfigurationProperties(prefix = "joprelys.appointments")
public record AppointmentProperties(
		int defaultSlotDurationMinutes,
		int bookingHorizonDays,
		int patientCancelDeadlineHours,
		int reminderHoursBefore,
		String timezone
) {

	public static final int DEFAULT_SLOT_DURATION_MINUTES = 30;
	public static final int DEFAULT_BOOKING_HORIZON_DAYS = 30;
	public static final int DEFAULT_PATIENT_CANCEL_DEADLINE_HOURS = 24;
	public static final int DEFAULT_REMINDER_HOURS_BEFORE = 24;
	public static final String DEFAULT_TIMEZONE = "Africa/Douala";

	/**
	 * Les primitives d'un record de configuration valent {@code 0} lorsqu'une source de propriétés
	 * ne renseigne pas la clé. Les valeurs métier documentées restent donc appliquées de manière
	 * défensive, y compris dans les profils de test ou les déploiements partiellement configurés.
	 */
	public AppointmentProperties {
		if (defaultSlotDurationMinutes <= 0) {
			defaultSlotDurationMinutes = DEFAULT_SLOT_DURATION_MINUTES;
		}
		if (bookingHorizonDays <= 0) {
			bookingHorizonDays = DEFAULT_BOOKING_HORIZON_DAYS;
		}
		if (patientCancelDeadlineHours <= 0) {
			patientCancelDeadlineHours = DEFAULT_PATIENT_CANCEL_DEADLINE_HOURS;
		}
		if (reminderHoursBefore <= 0) {
			reminderHoursBefore = DEFAULT_REMINDER_HOURS_BEFORE;
		}
		if (timezone == null || timezone.isBlank()) {
			timezone = DEFAULT_TIMEZONE;
		}
	}
}
