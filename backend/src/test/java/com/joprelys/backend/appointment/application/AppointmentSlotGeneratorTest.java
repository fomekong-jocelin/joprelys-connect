package com.joprelys.backend.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.appointment.application.AppointmentSlotGenerator.RuleWindow;
import com.joprelys.backend.appointment.application.AppointmentSlotGenerator.Slot;
import com.joprelys.backend.appointment.application.AppointmentSlotGenerator.UnavailabilityWindow;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires purs (sans Spring) du générateur de créneaux.
 * Fuseau de référence : Africa/Douala (UTC+1, sans heure d'été).
 */
class AppointmentSlotGeneratorTest {

	private static final ZoneId ZONE = ZoneId.of("Africa/Douala");
	private static final Duration SLOT_DURATION = Duration.ofMinutes(30);
	private static final LocalDate MONDAY = LocalDate.of(2026, 8, 3);
	private static final LocalDate TUESDAY = LocalDate.of(2026, 8, 4);
	private static final LocalDate SUNDAY = LocalDate.of(2026, 8, 9);

	@Test
	void shouldGenerateSlotsForSimpleRule() {
		RuleWindow rule = rule(1, "08:00", "12:00", MONDAY, null);

		List<Slot> slots = generate(List.of(rule), List.of(), List.of(), at(MONDAY, "00:00"), at(TUESDAY, "00:00"));

		assertEquals(8, slots.size(), "Une plage de 4 h en pas de 30 min doit produire 8 créneaux");
		assertEquals(at(MONDAY, "08:00"), slots.get(0).startAt(), "Le premier créneau démarre à l'ouverture de la plage");
		assertEquals(at(MONDAY, "08:30"), slots.get(0).endAt());
		assertEquals(at(MONDAY, "11:30"), slots.get(7).startAt());
		assertEquals(at(MONDAY, "12:00"), slots.get(7).endAt(), "Le dernier créneau se termine à la fermeture de la plage");
	}

	@Test
	void shouldMaskAllSlotsWhenExceptionCoversWholeRange() {
		RuleWindow rule = rule(1, "08:00", "12:00", MONDAY, null);
		UnavailabilityWindow exception = new UnavailabilityWindow(at(MONDAY, "07:00"), at(MONDAY, "13:00"));

		List<Slot> slots = generate(List.of(rule), List.of(exception), List.of(), at(MONDAY, "00:00"), at(TUESDAY, "00:00"));

		assertTrue(slots.isEmpty(), "Une indisponibilité couvrant toute la plage doit masquer tous les créneaux");
	}

	@Test
	void shouldMaskOnlyOverlappingSlots() {
		RuleWindow rule = rule(1, "08:00", "12:00", MONDAY, null);
		UnavailabilityWindow exception = new UnavailabilityWindow(at(MONDAY, "09:00"), at(MONDAY, "10:00"));

		List<Slot> slots = generate(List.of(rule), List.of(exception), List.of(), at(MONDAY, "00:00"), at(TUESDAY, "00:00"));

		assertEquals(
				List.of(
						at(MONDAY, "08:00"),
						at(MONDAY, "08:30"),
						at(MONDAY, "10:00"),
						at(MONDAY, "10:30"),
						at(MONDAY, "11:00"),
						at(MONDAY, "11:30")),
				startsOf(slots),
				"Seuls les créneaux de 09:00 et 09:30 doivent être masqués ; 10:00 touche la fin sans chevauchement");
	}

	@Test
	void shouldMaskSlotsAcrossMidnight() {
		RuleWindow mondayEvening = rule(1, "21:00", "23:00", MONDAY, null);
		RuleWindow tuesdayNight = rule(2, "00:00", "02:00", MONDAY, null);
		// Indisponibilité du lundi 22:30 au mardi 01:00 (heure clinique) : à cheval sur minuit.
		UnavailabilityWindow exception = new UnavailabilityWindow(at(MONDAY, "22:30"), at(TUESDAY, "01:00"));

		List<Slot> slots = generate(
				List.of(mondayEvening, tuesdayNight), List.of(exception), List.of(),
				at(MONDAY, "20:00"), at(TUESDAY, "03:00"));

		assertEquals(
				List.of(
						at(MONDAY, "21:00"),
						at(MONDAY, "21:30"),
						at(MONDAY, "22:00"),
						at(TUESDAY, "01:00"),
						at(TUESDAY, "01:30")),
				startsOf(slots),
				"Le créneau de 22:30 et ceux de 00:00/00:30 doivent être masqués ; 01:00 touche la fin sans chevauchement");
	}

	@Test
	void shouldRespectValidityWindow() {
		LocalDate validFrom = LocalDate.of(2026, 8, 10);
		LocalDate validTo = LocalDate.of(2026, 8, 16);
		RuleWindow rule = rule(1, "08:00", "10:00", validFrom, validTo);

		List<Slot> slots = generate(List.of(rule), List.of(), List.of(), at(MONDAY, "00:00"), at(LocalDate.of(2026, 8, 31), "00:00"));

		assertEquals(
				List.of(
						at(validFrom, "08:00"),
						at(validFrom, "08:30"),
						at(validFrom, "09:00"),
						at(validFrom, "09:30")),
				startsOf(slots),
				"Seul le lundi 10 août est couvert : le 3 août précède validFrom et le 17 août dépasse validTo");
	}

	@Test
	void shouldExcludeTruncatedSlotWhenDurationDoesNotDivide() {
		RuleWindow rule = rule(1, "08:00", "09:15", MONDAY, null);

		List<Slot> slots = generate(List.of(rule), List.of(), List.of(), at(MONDAY, "00:00"), at(TUESDAY, "00:00"));

		assertEquals(
				List.of(at(MONDAY, "08:00"), at(MONDAY, "08:30")),
				startsOf(slots),
				"Le créneau de 09:00, tronqué à 09:15, doit être exclu");
	}

	@Test
	void shouldExcludeReservedSlot() {
		RuleWindow rule = rule(1, "08:00", "10:00", MONDAY, null);

		List<Slot> slots = generate(
				List.of(rule), List.of(), List.of(at(MONDAY, "08:30")), at(MONDAY, "00:00"), at(TUESDAY, "00:00"));

		assertEquals(
				List.of(at(MONDAY, "08:00"), at(MONDAY, "09:00"), at(MONDAY, "09:30")),
				startsOf(slots),
				"Le créneau réservé de 08:30 doit être exclu");
	}

	@Test
	void shouldReturnEmptyWhenNoRule() {
		List<Slot> slots = generate(List.of(), List.of(), List.of(), at(MONDAY, "00:00"), at(TUESDAY, "00:00"));

		assertTrue(slots.isEmpty(), "Sans règle de disponibilité, aucun créneau ne doit être généré");
	}

	@Test
	void shouldApplyIsoWeekdays() {
		RuleWindow mondayRule = rule(1, "08:00", "09:00", MONDAY, null);
		RuleWindow sundayRule = rule(7, "08:00", "09:00", MONDAY, null);
		Instant weekEnd = at(LocalDate.of(2026, 8, 10), "00:00");

		List<Slot> mondaySlots = generate(List.of(mondayRule), List.of(), List.of(), at(MONDAY, "00:00"), weekEnd);
		List<Slot> sundaySlots = generate(List.of(sundayRule), List.of(), List.of(), at(MONDAY, "00:00"), weekEnd);

		assertEquals(
				List.of(at(MONDAY, "08:00"), at(MONDAY, "08:30")),
				startsOf(mondaySlots),
				"weekday = 1 doit produire des créneaux le lundi uniquement");
		assertEquals(
				List.of(at(SUNDAY, "08:00"), at(SUNDAY, "08:30")),
				startsOf(sundaySlots),
				"weekday = 7 doit produire des créneaux le dimanche uniquement");
	}

	private static RuleWindow rule(int weekday, String startTime, String endTime, LocalDate validFrom, LocalDate validTo) {
		return new RuleWindow(weekday, LocalTime.parse(startTime), LocalTime.parse(endTime), validFrom, validTo);
	}

	private static Instant at(LocalDate date, String localTime) {
		return date.atTime(LocalTime.parse(localTime)).atZone(ZONE).toInstant();
	}

	private static List<Slot> generate(
			List<RuleWindow> rules,
			List<UnavailabilityWindow> exceptions,
			List<Instant> reserved,
			Instant from,
			Instant to) {
		return AppointmentSlotGenerator.generate(rules, exceptions, reserved, from, to, SLOT_DURATION, ZONE);
	}

	private static List<Instant> startsOf(List<Slot> slots) {
		return slots.stream().map(Slot::startAt).toList();
	}
}
