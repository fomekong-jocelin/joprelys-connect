package com.joprelys.backend.appointment.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DoctorAppointmentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private AppointmentRepository appointmentRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private OrganizationEntity organization;
	private UserAccountEntity doctor;
	private UserAccountEntity otherDoctor;
	private UserAccountEntity disabledDoctor;
	private UserAccountEntity receptionAgent;
	private String doctorToken;
	private String disabledDoctorToken;
	private String receptionToken;
	private Instant from;
	private Instant to;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		organization = organizationRepository.saveAndFlush(new OrganizationEntity(
				"Clinique Agenda " + suffix,
				"agenda-" + suffix + "@test.local",
				"600010",
				"Akwa",
				"Douala"));
		doctor = saveUser("doctor-" + suffix + "@test.local", "Dr Alice", "MEDECIN", true);
		otherDoctor = saveUser("other-" + suffix + "@test.local", "Dr Bob", "MEDECIN", true);
		disabledDoctor = saveUser("disabled-" + suffix + "@test.local", "Dr Désactivé", "MEDECIN", false);
		receptionAgent = saveUser("reception-" + suffix + "@test.local", "Accueil", "AGENT_ACCUEIL", true);
		doctorToken = jwtService.createToken(doctor).value();
		disabledDoctorToken = jwtService.createToken(disabledDoctor).value();
		receptionToken = jwtService.createToken(receptionAgent).value();

		from = Instant.parse("2026-07-20T00:00:00Z");
		to = Instant.parse("2026-07-27T00:00:00Z");
		TenantContext.setTenantId(organization.getId());
		try {
			PatientEntity alice = savePatient("DPU-AGENDA-" + suffix, "Patient Alice");
			PatientEntity bob = savePatient("DPU-AGENDA2-" + suffix, "Patient Bob");
			appointmentRepository.saveAndFlush(new AppointmentEntity(
					doctor,
					alice,
					from,
					from.plusSeconds(1800),
					"Suivi"));
			appointmentRepository.saveAndFlush(new AppointmentEntity(
					otherDoctor,
					bob,
					from.plusSeconds(3600),
					from.plusSeconds(5400),
					"Consultation"));
			appointmentRepository.saveAndFlush(new AppointmentEntity(
					doctor,
					bob,
					to,
					to.plusSeconds(1800),
					"Borne exclusive"));
		} finally {
			TenantContext.clear();
		}
	}

	@AfterEach
	void tearDown() {
		TenantContext.clear();
		cleanDatabase();
	}

	@Test
	void shouldRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/doctor/appointments")
					.param("from", from.toString())
					.param("to", to.toString()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldListOnlyOwnAppointmentsInsideHalfOpenRange() throws Exception {
		mockMvc.perform(get("/api/doctor/appointments")
					.param("from", from.toString())
					.param("to", to.toString())
					.header("Authorization", "Bearer " + doctorToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].patientDisplayName").value("Patient Alice"))
				.andExpect(jsonPath("$[0].patientLocalNumber").isNotEmpty())
				.andExpect(jsonPath("$[0].startAt").value(from.toString()))
				.andExpect(jsonPath("$[0].status").value("CONFIRMED"))
				.andExpect(jsonPath("$[0].reason").value("Suivi"))
				.andExpect(jsonPath("$[0].phone").doesNotExist())
				.andExpect(jsonPath("$[0].medicalHistory").doesNotExist());
	}

	@Test
	void shouldRejectReceptionAgentEvenWithAppointmentReadPermission() throws Exception {
		mockMvc.perform(get("/api/doctor/appointments")
					.param("from", from.toString())
					.param("to", to.toString())
					.header("Authorization", "Bearer " + receptionToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("DOCTOR_APPOINTMENT_ACCESS_DENIED"));
	}

	@Test
	void shouldRejectDisabledDoctor() throws Exception {
		mockMvc.perform(get("/api/doctor/appointments")
					.param("from", from.toString())
					.param("to", to.toString())
					.header("Authorization", "Bearer " + disabledDoctorToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("DOCTOR_APPOINTMENT_ACCESS_DENIED"));
	}

	@Test
	void shouldRejectInvalidOrExcessiveRange() throws Exception {
		mockMvc.perform(get("/api/doctor/appointments")
					.param("from", from.toString())
					.param("to", from.toString())
					.header("Authorization", "Bearer " + doctorToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		mockMvc.perform(get("/api/doctor/appointments")
					.param("from", from.toString())
					.param("to", from.plusSeconds(93L * 24 * 3600).toString())
					.header("Authorization", "Bearer " + doctorToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	private UserAccountEntity saveUser(String email, String displayName, String role, boolean enabled) {
		UserAccountEntity user = new UserAccountEntity(email, displayName, role, "password");
		user.setOrganizationId(organization.getId());
		user.setEnabled(enabled);
		return userAccountRepository.saveAndFlush(user);
	}

	private PatientEntity savePatient(String globalNumber, String fullName) {
		return patientRepository.saveAndFlush(new PatientEntity(
				globalNumber,
				"LOCAL-" + UUID.randomUUID().toString().substring(0, 6),
				fullName,
				"MASCULIN",
				LocalDate.of(1990, 1, 1),
				"+237600000000",
				"Douala",
				null, null, null, null, null, null));
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM appointments");
		jdbcTemplate.update("DELETE FROM doctor_availability_exceptions");
		jdbcTemplate.update("DELETE FROM doctor_availabilities");
		jdbcTemplate.update("DELETE FROM emergency_admission_requests");
		jdbcTemplate.update("DELETE FROM resuscitation_logs");
		jdbcTemplate.update("DELETE FROM emergencies");
		jdbcTemplate.update("DELETE FROM patient_identity_declarations");
		jdbcTemplate.update("DELETE FROM patient_identity_status_history");
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM medical_documents");
		jdbcTemplate.update("DELETE FROM prescription_items");
		jdbcTemplate.update("DELETE FROM prescriptions");
		jdbcTemplate.update("DELETE FROM consultations");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();
	}
}
