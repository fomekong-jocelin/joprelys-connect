package com.joprelys.backend.appointment.application;

import com.joprelys.backend.appointment.api.AvailabilityExceptionResponse;
import com.joprelys.backend.appointment.api.AvailabilityRuleResponse;
import com.joprelys.backend.appointment.api.CreateAvailabilityExceptionRequest;
import com.joprelys.backend.appointment.api.UpsertAvailabilityRuleRequest;
import com.joprelys.backend.appointment.application.AppointmentSlotGenerator.RuleWindow;
import com.joprelys.backend.appointment.application.AppointmentSlotGenerator.Slot;
import com.joprelys.backend.appointment.application.AppointmentSlotGenerator.UnavailabilityWindow;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentRepository;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentStatus;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityExceptionEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityExceptionRepository;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service de gestion des disponibilités médecin (STORY-2602).
 *
 * <p>Règles métier : validations de plages (400), chevauchement de règles actives (409
 * {@code AVAILABILITY_OVERLAP}), jamais de retrait de disponibilité écrasant des rendez-vous
 * actifs futurs (409 {@code AVAILABILITY_CONFLICT}, RM-07), ressources introuvables ou hors
 * tenant (404 {@code AVAILABILITY_NOT_FOUND}), contrôle d'accès par périmètre médecin (403
 * {@code ACCESS_DENIED}).
 */
@Service
public class DefaultAvailabilityService implements AvailabilityService {

	// Statuts occupant réellement un créneau (aligné sur la colonne active_start_at, DATA-MODEL §5).
	private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
			AppointmentStatus.CONFIRMED,
			AppointmentStatus.COMPLETED,
			AppointmentStatus.NO_SHOW);

	private final DoctorAvailabilityRepository availabilityRepository;
	private final DoctorAvailabilityExceptionRepository exceptionRepository;
	private final AppointmentRepository appointmentRepository;
	private final UserAccountRepository userAccountRepository;
	private final AppointmentProperties properties;

	public DefaultAvailabilityService(
			DoctorAvailabilityRepository availabilityRepository,
			DoctorAvailabilityExceptionRepository exceptionRepository,
			AppointmentRepository appointmentRepository,
			UserAccountRepository userAccountRepository,
			AppointmentProperties properties) {
		this.availabilityRepository = availabilityRepository;
		this.exceptionRepository = exceptionRepository;
		this.appointmentRepository = appointmentRepository;
		this.userAccountRepository = userAccountRepository;
		this.properties = properties;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AvailabilityRuleResponse> listRules(UUID requestedDoctorId, UUID callerId, boolean clinicAdmin) {
		UUID doctorId = resolveScope(requestedDoctorId, callerId, clinicAdmin);
		return availabilityRepository.findByDoctorIdOrderByWeekdayAscStartTimeAsc(doctorId)
				.stream()
				.map(AvailabilityRuleResponse::fromEntity)
				.toList();
	}

	@Override
	@Transactional
	public AvailabilityRuleResponse createRule(
			UpsertAvailabilityRuleRequest request, UUID callerId, UUID organizationId, boolean clinicAdmin) {
		UUID doctorId = resolveScope(request.doctorId(), callerId, clinicAdmin);
		UserAccountEntity doctor = loadDoctor(doctorId, organizationId);
		validateRule(request);
		assertNoOverlap(doctorId, toWindow(request), null);
		DoctorAvailabilityEntity entity = new DoctorAvailabilityEntity(
				doctor,
				request.weekday(),
				request.startTime(),
				request.endTime(),
				request.validFrom(),
				request.validTo());
		return AvailabilityRuleResponse.fromEntity(availabilityRepository.save(entity));
	}

	@Override
	@Transactional
	public AvailabilityRuleResponse updateRule(
			UUID ruleId, UpsertAvailabilityRuleRequest request, UUID callerId, UUID organizationId, boolean clinicAdmin) {
		DoctorAvailabilityEntity entity = availabilityRepository.findById(ruleId)
				.orElseThrow(() -> notFound("Règle de disponibilité introuvable."));
		assertCanManage(entity.getDoctor().getId(), callerId, clinicAdmin);
		validateRule(request);
		if (request.doctorId() != null
				&& !resolveScope(request.doctorId(), callerId, clinicAdmin).equals(entity.getDoctor().getId())) {
			// La réaffectation n'est pas offerte par le contrat : la règle conserve son médecin.
			throw validation("La réaffectation d'une règle à un autre médecin n'est pas supportée : créez une nouvelle règle.");
		}

		if (Boolean.TRUE.equals(entity.getActive())) {
			UUID doctorId = entity.getDoctor().getId();
			List<DoctorAvailabilityEntity> activeRules = availabilityRepository.findByDoctorIdAndActiveTrue(doctorId);
			// L'état persisté est capturé sous forme immuable avant toute mutation de l'entité managée.
			List<RuleWindow> rulesBefore = activeRules.stream()
					.map(DefaultAvailabilityService::toWindow)
					.toList();
			RuleWindow newWindow = toWindow(request);
			List<RuleWindow> rulesAfter = activeRules.stream()
					.map(rule -> rule.getId().equals(ruleId) ? newWindow : toWindow(rule))
					.toList();
			assertNoOverlap(doctorId, newWindow, ruleId);
			assertNoAppointmentConflict(doctorId, rulesBefore, rulesAfter);
		}

		entity.setWeekday(request.weekday());
		entity.setStartTime(request.startTime());
		entity.setEndTime(request.endTime());
		entity.setValidFrom(request.validFrom());
		entity.setValidTo(request.validTo());
		return AvailabilityRuleResponse.fromEntity(availabilityRepository.save(entity));
	}

	@Override
	@Transactional
	public AvailabilityRuleResponse deactivateRule(UUID ruleId, UUID callerId, boolean clinicAdmin) {
		DoctorAvailabilityEntity entity = availabilityRepository.findById(ruleId)
				.orElseThrow(() -> notFound("Règle de disponibilité introuvable."));
		assertCanManage(entity.getDoctor().getId(), callerId, clinicAdmin);

		if (Boolean.TRUE.equals(entity.getActive())) {
			UUID doctorId = entity.getDoctor().getId();
			List<DoctorAvailabilityEntity> activeRules = availabilityRepository.findByDoctorIdAndActiveTrue(doctorId);
			List<RuleWindow> rulesBefore = activeRules.stream()
					.map(DefaultAvailabilityService::toWindow)
					.toList();
			List<RuleWindow> rulesAfter = activeRules.stream()
					.filter(rule -> !rule.getId().equals(ruleId))
					.map(DefaultAvailabilityService::toWindow)
					.toList();
			assertNoAppointmentConflict(doctorId, rulesBefore, rulesAfter);
			entity.setActive(false);
			availabilityRepository.save(entity);
		}
		return AvailabilityRuleResponse.fromEntity(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AvailabilityExceptionResponse> listExceptions(
			UUID requestedDoctorId, Instant from, Instant to, UUID callerId, boolean clinicAdmin) {
		UUID doctorId = resolveScope(requestedDoctorId, callerId, clinicAdmin);
		if (from != null && to != null && !to.isAfter(from)) {
			throw validation("La borne de fin de période doit être postérieure à la borne de début.");
		}
		List<DoctorAvailabilityExceptionEntity> entities;
		if (from != null && to != null) {
			// Indisponibilités recouvrant la période : startAt < to && endAt > from.
			entities = exceptionRepository.findByDoctorIdAndStartAtLessThanAndEndAtGreaterThan(doctorId, to, from);
		} else {
			entities = exceptionRepository.findByDoctorIdOrderByStartAtAsc(doctorId).stream()
					.filter(exception -> from == null || exception.getEndAt().isAfter(from))
					.filter(exception -> to == null || exception.getStartAt().isBefore(to))
					.toList();
		}
		return entities.stream().map(AvailabilityExceptionResponse::fromEntity).toList();
	}

	@Override
	@Transactional
	public AvailabilityExceptionResponse createException(
			CreateAvailabilityExceptionRequest request, UUID callerId, UUID organizationId, boolean clinicAdmin) {
		UUID doctorId = resolveScope(request.doctorId(), callerId, clinicAdmin);
		UserAccountEntity doctor = loadDoctor(doctorId, organizationId);
		if (!request.endAt().isAfter(request.startAt())) {
			throw validation("La fin de l'indisponibilité doit être postérieure à son début.");
		}
		Instant now = Instant.now();
		boolean conflict = appointmentRepository
				.findByDoctorIdAndStartAtGreaterThanEqualAndStatusIn(doctorId, now, ACTIVE_STATUSES)
				.stream()
				.anyMatch(appointment -> appointment.getStartAt().isBefore(request.endAt())
						&& appointment.getEndAt().isAfter(request.startAt()));
		if (conflict) {
			throw new AvailabilityApiException(HttpStatus.CONFLICT, "AVAILABILITY_CONFLICT",
					"Des rendez-vous actifs existent sur cette période. Veuillez les traiter avant de créer l'indisponibilité.");
		}
		DoctorAvailabilityExceptionEntity entity = new DoctorAvailabilityExceptionEntity(
				doctor, request.startAt(), request.endAt(), request.reason());
		return AvailabilityExceptionResponse.fromEntity(exceptionRepository.save(entity));
	}

	@Override
	@Transactional
	public void deleteException(UUID exceptionId, UUID callerId, boolean clinicAdmin) {
		DoctorAvailabilityExceptionEntity entity = exceptionRepository.findById(exceptionId)
				.orElseThrow(() -> notFound("Indisponibilité introuvable."));
		UUID doctorId = entity.getDoctor().getId();
		assertCanManage(doctorId, callerId, clinicAdmin);

		// RM-07 : jamais de suppression silencieuse si des rendez-vous actifs futurs recouvrent la plage.
		Instant now = Instant.now();
		boolean conflict = appointmentRepository
				.findByDoctorIdAndStartAtGreaterThanEqualAndStatusIn(doctorId, now, ACTIVE_STATUSES)
				.stream()
				.anyMatch(appointment -> appointment.getStartAt().isBefore(entity.getEndAt())
						&& appointment.getEndAt().isAfter(entity.getStartAt()));
		if (conflict) {
			throw new AvailabilityApiException(HttpStatus.CONFLICT, "AVAILABILITY_CONFLICT",
					"Des rendez-vous actifs existent sur la plage de cette indisponibilité. Veuillez les traiter avant de la supprimer.");
		}
		exceptionRepository.delete(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Slot> generateSlots(UUID doctorId, Instant from, Instant to) {
		if (doctorId == null || from == null || to == null || !to.isAfter(from)) {
			throw validation("La période demandée est invalide.");
		}
		List<RuleWindow> rules = availabilityRepository.findByDoctorIdAndActiveTrue(doctorId).stream()
				.map(DefaultAvailabilityService::toWindow)
				.toList();
		List<UnavailabilityWindow> exceptions = exceptionRepository
				.findByDoctorIdAndStartAtLessThanAndEndAtGreaterThan(doctorId, to, from)
				.stream()
				.map(DefaultAvailabilityService::toWindow)
				.toList();
		List<Instant> reservedStarts = appointmentRepository
				.findByDoctorIdAndStartAtBetweenAndStatusIn(doctorId, from, to, ACTIVE_STATUSES)
				.stream()
				.map(AppointmentEntity::getStartAt)
				.toList();
		return AppointmentSlotGenerator.generate(
				rules,
				exceptions,
				reservedStarts,
				from,
				to,
				Duration.ofMinutes(properties.defaultSlotDurationMinutes()),
				ZoneId.of(properties.timezone()));
	}

	/**
	 * RM-07 : vérifie qu'aucun rendez-vous actif futur du médecin n'occupe un créneau qui
	 * disparaîtrait entre l'état courant des règles ({@code rulesBefore}) et leur état après
	 * modification ({@code rulesAfter}).
	 */
	private void assertNoAppointmentConflict(UUID doctorId, List<RuleWindow> rulesBefore, List<RuleWindow> rulesAfter) {
		Instant now = Instant.now();
		List<AppointmentEntity> futureAppointments = appointmentRepository
				.findByDoctorIdAndStartAtGreaterThanEqualAndStatusIn(doctorId, now, ACTIVE_STATUSES);
		if (futureAppointments.isEmpty()) {
			return;
		}
		Instant horizon = futureAppointments.stream()
				.map(AppointmentEntity::getEndAt)
				.max(Instant::compareTo)
				.orElse(now);
		List<UnavailabilityWindow> exceptions = exceptionRepository
				.findByDoctorIdAndStartAtLessThanAndEndAtGreaterThan(doctorId, horizon, now)
				.stream()
				.map(DefaultAvailabilityService::toWindow)
				.toList();
		Duration slotDuration = Duration.ofMinutes(properties.defaultSlotDurationMinutes());
		ZoneId zone = ZoneId.of(properties.timezone());
		Set<Instant> slotsBefore = startsOf(AppointmentSlotGenerator.generate(
				rulesBefore, exceptions, List.of(), now, horizon, slotDuration, zone));
		Set<Instant> slotsAfter = startsOf(AppointmentSlotGenerator.generate(
				rulesAfter, exceptions, List.of(), now, horizon, slotDuration, zone));
		boolean conflict = futureAppointments.stream()
				.anyMatch(appointment -> slotsBefore.contains(appointment.getStartAt())
						&& !slotsAfter.contains(appointment.getStartAt()));
		if (conflict) {
			throw new AvailabilityApiException(HttpStatus.CONFLICT, "AVAILABILITY_CONFLICT",
					"Des rendez-vous actifs existent sur les créneaux retirés. Veuillez les traiter avant de modifier la disponibilité.");
		}
	}

	/**
	 * Une règle active ne doit chevaucher aucune autre règle active du même médecin sur le même
	 * jour, avec intersection des fenêtres de validité ({@code validTo} nul = sans fin) et
	 * chevauchement strict des plages horaires (plages adjacentes autorisées).
	 */
	private void assertNoOverlap(UUID doctorId, RuleWindow candidate, UUID excludedRuleId) {
		List<DoctorAvailabilityEntity> actives = availabilityRepository
				.findByDoctorIdAndWeekdayAndActiveTrue(doctorId, candidate.weekday());
		for (DoctorAvailabilityEntity existing : actives) {
			if (existing.getId().equals(excludedRuleId)) {
				continue;
			}
			boolean timeOverlap = candidate.startTime().isBefore(existing.getEndTime())
					&& candidate.endTime().isAfter(existing.getStartTime());
			boolean validityOverlap = (existing.getValidTo() == null || !candidate.validFrom().isAfter(existing.getValidTo()))
					&& (candidate.validTo() == null || !existing.getValidFrom().isAfter(candidate.validTo()));
			if (timeOverlap && validityOverlap) {
				throw new AvailabilityApiException(HttpStatus.CONFLICT, "AVAILABILITY_OVERLAP",
						"Cette plage chevauche une règle de disponibilité active existante.");
			}
		}
	}

	private void validateRule(UpsertAvailabilityRuleRequest request) {
		if (!request.endTime().isAfter(request.startTime())) {
			throw validation("L'heure de fin doit être postérieure à l'heure de début.");
		}
		if (request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
			throw validation("La date de fin de validité doit être postérieure ou égale à la date de début de validité.");
		}
	}

	/**
	 * Résout le médecin concerné : {@code doctorId} omis = appelant. Un médecin ne peut viser que
	 * lui-même ; seul un administrateur de clinique peut viser un autre médecin.
	 */
	private UUID resolveScope(UUID requestedDoctorId, UUID callerId, boolean clinicAdmin) {
		if (requestedDoctorId == null || requestedDoctorId.equals(callerId)) {
			return callerId;
		}
		if (!clinicAdmin) {
			throw accessDenied();
		}
		return requestedDoctorId;
	}

	private void assertCanManage(UUID resourceDoctorId, UUID callerId, boolean clinicAdmin) {
		if (!clinicAdmin && !resourceDoctorId.equals(callerId)) {
			throw accessDenied();
		}
	}

	/**
	 * Charge le médecin en vérifiant son appartenance au tenant courant et son rôle : la table
	 * {@code users} n'est pas filtrée par {@code @TenantId}, le contrôle est donc explicite, et
	 * une disponibilité n'a de sens que pour un porteur du rôle MEDECIN (CSV multi-rôles).
	 */
	private UserAccountEntity loadDoctor(UUID doctorId, UUID organizationId) {
		return userAccountRepository.findById(doctorId)
				.filter(doctor -> organizationId == null || organizationId.equals(doctor.getOrganizationId()))
				.filter(doctor -> doctor.hasRole("MEDECIN")
						|| doctor.hasRole("ADMIN_CLINIQUE")
						|| doctor.hasRole("ADMIN_JOPRELYS")
						|| doctor.hasRole("SUPER_ADMIN"))
				.orElseThrow(() -> notFound("Médecin ou administrateur introuvable dans cette clinique."));
	}

	private static RuleWindow toWindow(UpsertAvailabilityRuleRequest request) {
		return new RuleWindow(request.weekday(), request.startTime(), request.endTime(), request.validFrom(), request.validTo());
	}

	private static RuleWindow toWindow(DoctorAvailabilityEntity entity) {
		return new RuleWindow(entity.getWeekday(), entity.getStartTime(), entity.getEndTime(), entity.getValidFrom(), entity.getValidTo());
	}

	private static UnavailabilityWindow toWindow(DoctorAvailabilityExceptionEntity entity) {
		return new UnavailabilityWindow(entity.getStartAt(), entity.getEndAt());
	}

	private static Set<Instant> startsOf(List<Slot> slots) {
		return slots.stream().map(Slot::startAt).collect(Collectors.toSet());
	}

	private static AvailabilityApiException notFound(String message) {
		return new AvailabilityApiException(HttpStatus.NOT_FOUND, "AVAILABILITY_NOT_FOUND", message);
	}

	private static AvailabilityApiException validation(String message) {
		return new AvailabilityApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}

	private static AvailabilityApiException accessDenied() {
		return new AvailabilityApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
				"Un médecin ne peut gérer que ses propres disponibilités.");
	}
}
