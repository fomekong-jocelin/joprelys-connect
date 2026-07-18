package com.joprelys.backend.appointment.api;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentRepository;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientAppointmentControllerTest {

	private static final ZoneId ZONE = ZoneId.of("Africa/Douala");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private DoctorAvailabilityRepository availabilityRepository;

	@Autowired
	private AppointmentRepository appointmentRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	private OrganizationEntity organization;
	private OrganizationEntity otherOrganization;
	private UserAccountEntity doctor;
	private UserAccountEntity otherDoctor;
	private UserAccountEntity disabledDoctor;
	private UserAccountEntity nurse;
	private PatientEntity patient;
	private PatientEntity secondPatient;
	private String patientToken;
	private String secondPatientToken;
	private String doctorToken;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		organization = organizationRepository.saveAndFlush(new OrganizationEntity(
				"Clinique RDV " + suffix, "rdv-" + suffix + "@test.local", "600001", "Akwa", "Douala"));
		otherOrganization = organizationRepository.saveAndFlush(new OrganizationEntity(
				"Clinique Externe " + suffix, "ext-" + suffix + "@test.local", "600002", "Centre", "Yaoundé"));

		doctor = saveUser("doctor-" + suffix + "@test.local", "Dr Alice", "MEDECIN", organization.getId(), true);
		doctor.setSpecialty("Cardiologie");
		doctor.setDepartment("Consultations externes");
		doctor = userAccountRepository.saveAndFlush(doctor);
		otherDoctor = saveUser("other-" + suffix + "@test.local", "Dr Externe", "MEDECIN", otherOrganization.getId(), true);
		disabledDoctor = saveUser("disabled-" + suffix + "@test.local", "Dr Désactivé", "MEDECIN", organization.getId(), false);
		nurse = saveUser("nurse-" + suffix + "@test.local", "Infirmier Test", "INFIRMIER", organization.getId(), true);
		doctorToken = jwtService.createToken(doctor).value();

		TenantContext.setTenantId(organization.getId());
		try {
			patient = savePatient("DPU-RDV-" + suffix, "Patient Alice");
			secondPatient = savePatient("DPU-RDV2-" + suffix, "Patient Bob");
			patientToken = jwtService.createPatientToken(patient).value();
			secondPatientToken = jwtService.createPatientToken(secondPatient).value();
			availabilityRepository.saveAndFlush(new DoctorAvailabilityEntity(
					doctor, 1, LocalTime.of(8, 0), LocalTime.of(12, 0), appointmentMonday(), null));
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
	void shouldRequirePatientRole() throws Exception {
		mockMvc.perform(get("/api/patient/appointments/doctors"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/patient/appointments/doctors")
						.header("Authorization", "Bearer " + doctorToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldListOnlyActiveDoctorsFromPatientOrganization() throws Exception {
		mockMvc.perform(get("/api/patient/appointments/doctors")
						.param("specialty", "cardio")
						.param("department", "externes")
						.header("Authorization", "Bearer " + patientToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].doctorId").value(doctor.getId().toString()))
				.andExpect(jsonPath("$[0].displayName").value("Dr Alice"));
	}

	@Test
	void shouldGenerateBookListAndCancelOwnAppointment() throws Exception {
		Instant startAt = at(appointmentMonday(), 8, 0);
		mockMvc.perform(get("/api/patient/appointments/slots")
						.param("doctorId", doctor.getId().toString())
						.param("from", at(appointmentMonday(), 0, 0).toString())
						.param("to", at(appointmentMonday().plusDays(1), 0, 0).toString())
						.header("Authorization", "Bearer " + patientToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].startAt").value(startAt.toString()));

		String booked = mockMvc.perform(post("/api/patient/appointments")
						.header("Authorization", "Bearer " + patientToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(bookBody(doctor.getId(), startAt, "Suivi"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.doctorDisplayName").value("Dr Alice"))
				.andReturn().getResponse().getContentAsString();
		UUID appointmentId = UUID.fromString(objectMapper.readTree(booked).get("id").asText());

		mockMvc.perform(get("/api/patient/appointments")
						.header("Authorization", "Bearer " + patientToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(appointmentId.toString()));

		mockMvc.perform(post("/api/patient/appointments/" + appointmentId + "/cancel")
						.header("Authorization", "Bearer " + patientToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED_BY_PATIENT"));

		TenantContext.setTenantId(organization.getId());
		try {
			AppointmentEntity persisted = appointmentRepository.findById(appointmentId).orElseThrow();
			assertNull(persisted.getActiveStartAt(), "L'annulation doit libérer le créneau");
		} finally {
			TenantContext.clear();
		}
	}

	@Test
	void shouldRejectSecondActiveAppointmentWithSameDoctorOnSameDay() throws Exception {
		book(patientToken, at(appointmentMonday(), 8, 0));
		mockMvc.perform(post("/api/patient/appointments")
						.header("Authorization", "Bearer " + patientToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								bookBody(doctor.getId(), at(appointmentMonday(), 9, 0), null))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("DUPLICATE_ACTIVE_APPOINTMENT"));
	}

	@Test
	void shouldHideAppointmentOfAnotherPatient() throws Exception {
		UUID appointmentId = book(secondPatientToken, at(appointmentMonday(), 8, 30));
		mockMvc.perform(post("/api/patient/appointments/" + appointmentId + "/cancel")
						.header("Authorization", "Bearer " + patientToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("APPOINTMENT_NOT_FOUND"));
	}

	@Test
	void shouldRejectAvailabilityExceptionCoveringFutureAppointment() throws Exception {
		Instant startAt = at(appointmentMonday(), 10, 0);
		book(patientToken, startAt);
		mockMvc.perform(post("/api/availabilities/exceptions")
						.header("Authorization", "Bearer " + doctorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"startAt", startAt.minus(15, ChronoUnit.MINUTES).toString(),
								"endAt", startAt.plus(45, ChronoUnit.MINUTES).toString(),
								"reason", "Réunion"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("AVAILABILITY_CONFLICT"));
	}

	@Test
	void shouldReturnDoctorNotFoundForAnotherTenantOrDisabledDoctor() throws Exception {
		for (UUID doctorId : new UUID[]{otherDoctor.getId(), disabledDoctor.getId(), nurse.getId()}) {
			mockMvc.perform(get("/api/patient/appointments/slots")
							.param("doctorId", doctorId.toString())
							.param("from", at(appointmentMonday(), 0, 0).toString())
							.param("to", at(appointmentMonday().plusDays(1), 0, 0).toString())
							.header("Authorization", "Bearer " + patientToken))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.error.code").value("DOCTOR_NOT_FOUND"));
		}
	}

	private UUID book(String token, Instant startAt) throws Exception {
		String response = mockMvc.perform(post("/api/patient/appointments")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(bookBody(doctor.getId(), startAt, null))))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).get("id").asText());
	}

	private UserAccountEntity saveUser(
			String email, String displayName, String role, UUID organizationId, boolean enabled) {
		UserAccountEntity user = new UserAccountEntity(email, displayName, role, "password");
		user.setOrganizationId(organizationId);
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

	private static Map<String, Object> bookBody(UUID doctorId, Instant startAt, String reason) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("doctorId", doctorId.toString());
		body.put("startAt", startAt.toString());
		if (reason != null) body.put("reason", reason);
		return body;
	}

	private static LocalDate appointmentMonday() {
		return LocalDate.now(ZONE)
				.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
				.plusWeeks(1);
	}

	private static Instant at(LocalDate date, int hour, int minute) {
		return date.atTime(LocalTime.of(hour, minute)).atZone(ZONE).toInstant();
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
