package com.joprelys.backend.appointment.application;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Générateur pur (sans Spring) des créneaux de rendez-vous d'un médecin.
 *
 * <p>Algorithme : pour chaque date de la période [{@code from}, {@code to}] exprimée dans le
 * fuseau de la clinique, les règles dont le jour ISO (1 = lundi .. 7 = dimanche) correspond et
 * dont la fenêtre de validité couvre la date sont découpées en créneaux de durée fixe. Un créneau
 * tronqué en fin de plage est exclu. Sont ensuite écartés les créneaux chevauchant strictement une
 * indisponibilité et les créneaux déjà réservés (égalité sur l'instant de début).
 *
 * <p>Un créneau est conservé dès lors qu'il recouvre la période demandée
 * ({@code startAt < to && endAt > from}).
 */
public final class AppointmentSlotGenerator {

	private AppointmentSlotGenerator() {
	}

	/** Créneau réservable, exprimé en UTC. */
	public record Slot(Instant startAt, Instant endAt) {
	}

	/** Vue immuable d'une règle de disponibilité récurrente. */
	public record RuleWindow(
			Integer weekday,
			LocalTime startTime,
			LocalTime endTime,
			LocalDate validFrom,
			LocalDate validTo) {
	}

	/** Fenêtre d'indisponibilité (congé, absence), exprimée en UTC. */
	public record UnavailabilityWindow(Instant startAt, Instant endAt) {
	}

	public static List<Slot> generate(
			List<RuleWindow> rules,
			List<UnavailabilityWindow> exceptions,
			List<Instant> reservedStarts,
			Instant from,
			Instant to,
			Duration slotDuration,
			ZoneId zone) {
		if (rules == null || rules.isEmpty()
				|| from == null || to == null || !to.isAfter(from)
				|| slotDuration == null || slotDuration.isZero() || slotDuration.isNegative()
				|| zone == null) {
			return List.of();
		}
		List<UnavailabilityWindow> safeExceptions = exceptions == null ? List.of() : exceptions;
		Set<Instant> reserved = reservedStarts == null ? Set.of() : new HashSet<>(reservedStarts);

		List<Slot> slots = new ArrayList<>();
		LocalDate firstDate = from.atZone(zone).toLocalDate();
		LocalDate lastDate = to.atZone(zone).toLocalDate();
		for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
			int weekday = date.getDayOfWeek().getValue();
			for (RuleWindow rule : rules) {
				if (!appliesOn(rule, weekday, date)) {
					continue;
				}
				collectDaySlots(rule, date, from, to, slotDuration, zone, safeExceptions, reserved, slots);
			}
		}
		slots.sort(Comparator.comparing(Slot::startAt));
		return slots;
	}

	private static boolean appliesOn(RuleWindow rule, int weekday, LocalDate date) {
		if (rule.weekday() == null || rule.weekday() != weekday) {
			return false;
		}
		if (rule.validFrom() != null && date.isBefore(rule.validFrom())) {
			return false;
		}
		return rule.validTo() == null || !date.isAfter(rule.validTo());
	}

	private static void collectDaySlots(
			RuleWindow rule,
			LocalDate date,
			Instant from,
			Instant to,
			Duration slotDuration,
			ZoneId zone,
			List<UnavailabilityWindow> exceptions,
			Set<Instant> reserved,
			List<Slot> slots) {
		LocalTime cursor = rule.startTime();
		while (true) {
			LocalTime endLocal = cursor.plus(slotDuration);
			// Arrêt sur passage à minuit (repli horaire) ou créneau tronqué en fin de plage.
			if (!endLocal.isAfter(cursor) || endLocal.isAfter(rule.endTime())) {
				return;
			}
			Instant startAt = date.atTime(cursor).atZone(zone).toInstant();
			Instant endAt = date.atTime(endLocal).atZone(zone).toInstant();
			cursor = endLocal;
			if (!startAt.isBefore(to) || !endAt.isAfter(from)) {
				continue;
			}
			if (reserved.contains(startAt)) {
				continue;
			}
			if (isMasked(startAt, endAt, exceptions)) {
				continue;
			}
			slots.add(new Slot(startAt, endAt));
		}
	}

	private static boolean isMasked(Instant startAt, Instant endAt, List<UnavailabilityWindow> exceptions) {
		for (UnavailabilityWindow exception : exceptions) {
			// Chevauchement strict : le créneau commence avant la fin de l'indisponibilité
			// et se termine après son début.
			if (startAt.isBefore(exception.endAt()) && endAt.isAfter(exception.startAt())) {
				return true;
			}
		}
		return false;
	}
}
