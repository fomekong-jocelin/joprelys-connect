package com.joprelys.backend.appointment.api;

import com.joprelys.backend.appointment.application.DoctorAppointmentService;
import com.joprelys.backend.auth.security.JwtClaims;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint de lecture de l'agenda personnel du médecin connecté. */
@RestController
@RequestMapping("/api/doctor/appointments")
@PreAuthorize("hasAuthority('APPOINTMENT_READ')")
public class DoctorAppointmentController {

	private final DoctorAppointmentService doctorAppointmentService;

	public DoctorAppointmentController(DoctorAppointmentService doctorAppointmentService) {
		this.doctorAppointmentService = doctorAppointmentService;
	}

	@GetMapping
	public List<DoctorAppointmentResponse> listOwn(
			@RequestParam Instant from,
			@RequestParam Instant to,
			Authentication authentication) {
		JwtClaims claims = (JwtClaims) authentication.getDetails();
		UUID doctorId = UUID.fromString(claims.subject());
		UUID organizationId = claims.organizationId() == null || claims.organizationId().isBlank()
				? null
				: UUID.fromString(claims.organizationId());
		return doctorAppointmentService.listOwn(doctorId, organizationId, from, to);
	}
}
