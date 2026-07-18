package com.joprelys.backend.appointment.application;

import com.joprelys.backend.appointment.api.DoctorAppointmentResponse;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service de lecture de l'agenda personnel du médecin authentifié. */
@Service
public class DefaultDoctorAppointmentService implements DoctorAppointmentService {

	private static final Duration MAX_RANGE = Duration.ofDays(92);

	private final UserAccountRepository userAccountRepository;
	private final AppointmentRepository appointmentRepository;

	public DefaultDoctorAppointmentService(
			UserAccountRepository userAccountRepository,
			AppointmentRepository appointmentRepository) {
		this.userAccountRepository = userAccountRepository;
		this.appointmentRepository = appointmentRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DoctorAppointmentResponse> listOwn(
			UUID doctorId,
			UUID organizationId,
			Instant from,
			Instant to) {
		UserAccountEntity doctor = loadDoctor(doctorId, organizationId);
		validateRange(from, to);
		return appointmentRepository
				.findByDoctorIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
						doctor.getId(), from, to)
				.stream()
				.map(DoctorAppointmentResponse::fromEntity)
				.toList();
	}

	private UserAccountEntity loadDoctor(UUID doctorId, UUID organizationId) {
		if (doctorId == null || organizationId == null) {
			throw accessDenied();
		}
		UserAccountEntity doctor = userAccountRepository
				.findByIdAndOrganizationId(doctorId, organizationId)
				.orElseThrow(DefaultDoctorAppointmentService::accessDenied);
		if (!doctor.isEnabled() || !doctor.hasRole("MEDECIN")) {
			throw accessDenied();
		}
		return doctor;
	}

	private static void validateRange(Instant from, Instant to) {
		if (from == null || to == null || !to.isAfter(from)) {
			throw validation("La période demandée est invalide.");
		}
		if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
			throw validation("La période demandée ne peut pas dépasser 92 jours.");
		}
	}

	private static DoctorAppointmentApiException accessDenied() {
		return new DoctorAppointmentApiException(
				HttpStatus.FORBIDDEN,
				"DOCTOR_APPOINTMENT_ACCESS_DENIED",
				"L'accès à cet agenda médecin est refusé.");
	}

	private static DoctorAppointmentApiException validation(String message) {
		return new DoctorAppointmentApiException(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				message);
	}
}
