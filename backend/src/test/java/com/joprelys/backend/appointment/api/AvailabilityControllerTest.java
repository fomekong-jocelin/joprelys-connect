package com.joprelys.backend.appointment.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.joprelys.backend.appointment.application.AppointmentSlotGenerator;
import com.joprelys.backend.appointment.application.AvailabilityService;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentRepository;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityExceptionEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityExceptionRepository;
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
import java.util.List;
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

/**
 * Tests d'intégration MockMvc des endpoints {@code /api/availabilities} (STORY-2602) :
 * authentification, autorisation par rôle et par périmètre médecin, règles métier
 * (chevauchement, RM-07), isolation multi-tenant et génération de créneaux via le service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvailabilityControllerTest {

	private static final ZoneId CLINIC_ZONE = ZoneId.of("Africa/Douala");

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
	private DoctorAvailabilityExceptionRepository exceptionRepository;

	@Autowired
	private AppointmentRepository appointmentRepository;

	@Autowired
	private AvailabilityService availabilityService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	private OrganizationEntity organization;
	private OrganizationEntity otherOrganization;
	private UserAccountEntity medecin;
	private UserAccountEntity autreMedecin;
	private UserAccountEntity admin;
	private UserAccountEntity agent;
	private UserAccountEntity medecinExterne;
	private PatientEntity patient;
	private String medecinToken;
	private String adminToken;
	private String agentToken;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		organization = organizationRepository.saveAndFlush(new OrganizationEntity(
				"Clinique Dispo " + suffix, "dispo-" + suffix + "@test.local", "600000010", "Centre", "Douala"));
		otherOrganization = organizationRepository.saveAndFlush(new OrganizationEntity(
				"Clinique Externe " + suffix, "externe-" + suffix + "@test.local", "600000011", "Centre", "Yaoundé"));

		medecin = saveUser("medecin-" + suffix + "@test.local", "Dr Meredith", "MEDECIN", organization.getId());
		autreMedecin = saveUser("autre-medecin-" + suffix + "@test.local", "Dr Derek", "MEDECIN", organization.getId());
		admin = saveUser("admin-" + suffix + "@test.local", "Admin Clinique", "ADMIN_CLINIQUE", organization.getId());
		agent = saveUser("agent-" + suffix + "@test.local", "Agent Accueil", "AGENT_ACCUEIL", organization.getId());
		medecinExterne = saveUser("medecin-externe-" + suffix + "@test.local", "Dr Externe", "MEDECIN", otherOrganization.getId());

		medecinToken = jwtService.createToken(medecin).value();
		adminToken = jwtService.createToken(admin).value();
		agentToken = jwtService.createToken(agent).value();

		TenantContext.setTenantId(organization.getId());
		try {
			patient = patientRepository.saveAndFlush(new PatientEntity(
					"DPU-DISPO-" + suffix, "PAT-DISPO-" + suffix, "Patient Dispo", "MASCULIN",
					LocalDate.of(1990, 1, 1), "+237600000012", "Douala", null, null, null, null, null, null));
		} finally {
			TenantContext.clear();
		}
	}

	@AfterEach
	void tearDown() {
		TenantContext.clear();
	}

	@Test
	void shouldReturn401WhenNoToken() throws Exception {
		mockMvc.perform(get("/api/availabilities"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturn403ForAgentAccueilRole() throws Exception {
		mockMvc.perform(get("/api/availabilities")
						.header("Authorization", "Bearer " + agentToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldReturn403WhenMedecinTargetsAnotherMedecin() throws Exception {
		// Création pour un autre médecin : refusée.
		mockMvc.perform(post("/api/availabilities")
						.header("Authorization", "Bearer " + medecinToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								ruleBody(autreMedecin.getId(), 1, "08:00", "12:00", nextMonday(), null))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));

		// Lecture des règles d'un autre médecin : refusée.
		mockMvc.perform(get("/api/availabilities")
						.param("doctorId", autreMedecin.getId().toString())
						.header("Authorization", "Bearer " + medecinToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));

		// Modification d'une règle appartenant à un autre médecin : refusée.
		DoctorAvailabilityEntity otherRule;
		TenantContext.setTenantId(organization.getId());
		try {
			otherRule = availabilityRepository.saveAndFlush(new DoctorAvailabilityEntity(
					autreMedecin, 2, LocalTime.of(8, 0), LocalTime.of(12, 0), nextMonday(), null));
		} finally {
			TenantContext.clear();
		}
		mockMvc.perform(put("/api/availabilities/" + otherRule.getId())
						.header("Authorization", "Bearer " + medecinToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								ruleBody(null, 2, "09:00", "12:00", nextMonday(), null))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
	}

	@Test
	void shouldCreateReadUpdateAndDeactivateRule() throws Exception {
		String response = mockMvc.perform(post("/api/availabilities")
						.header("Authorization", "Bearer " + medecinToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								ruleBody(null, 1, "08:00", "12:00", nextMonday(), null))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctorId").value(medecin.getId().toString()))
				.andExpect(jsonPath("$.weekday").value(1))
				.andExpect(jsonPath("$.startTime").value("08:00"))
				.andExpect(jsonPath("$.endTime").value("12:00"))
				.andExpect(jsonPath("$.active").value(true))
				.andReturn().getResponse().getContentAsString();
		UUID ruleId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

		mockMvc.perform(get("/api/availabilities")
						.header("Authorization", "Bearer " + medecinToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(ruleId.toString()));

		mockMvc.perform(put("/api/availabilities/" + ruleId)
						.header("Authorization", "Bearer " + medecinToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								ruleBody(null, 1, "09:00", "13:00", nextMonday(), null))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.startTime").value("09:00"))
				.andExpect(jsonPath("$.endTime").value("13:00"));

		mockMvc.perform(delete("/api/availabilities/" + ruleId)
						.header("Authorization", "Bearer " + medecinToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));

		TenantContext.setTenantId(organization.getId());
		try {
			DoctorAvailabilityEntity persisted = availabilityRepository.findById(ruleId)
					.orElseThrow(() -> new AssertionError("La règle doit être conservée en base"));
			assertFalse(persisted.getActive(), "La suppression doit être une désactivation logique (active = false)");
		} finally {
			TenantContext.clear();
		}
	}

	@Test
	void shouldAllowAdminCliniqueToManageAnyDoctor() throws Exception {
		mockMvc.perform(post("/api/availabilities")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								ruleBody(medecin.getId(), 3, "14:00", "18:00", nextMonday(), null))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctorId").value(medecin.getId().toString()));

		mockMvc.perform(get("/api/availabilities")
						.param("doctorId", medecin.getId().toString())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void shouldReturn409WhenRuleOverlapsActiveRule() throws Exception {
		createRuleAndGetId(medecinToken, ruleBody(null, 1, "08:00", "12:00", nextMonday(), null));

		mockMvc.perform(post("/api/availabilities")
						.header("Authorization", "Bearer " + medecinToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								ruleBody(null, 1, "09:00", "11:00", nextMonday(), null))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("AVAILABILITY_OVERLAP"));

		// Fenêtres de validité disjointes : accepté.
		createRuleAndGetId(medecinToken,
				ruleBody(null, 1, "09:00", "11:00", nextMonday().minusDays(8), nextMonday().minusDays(1)));
		// Plages adjacentes (pas de chevauchement strict) : accepté.
		createRuleAndGetId(medecinToken, ruleBody(null, 1, "12:00", "14:00", nextMonday(), null));
		// Autre jour de la semaine : accepté.
		createRuleAndGetId(medecinToken, ruleBody(null, 2, "09:00", "11:00", nextMonday(), null));

		mockMvc.perform(get("/api/availabilities")
						.header("Authorization", "Bearer " + medecinToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(4));
	}

	@Test
	void shouldReturn409WhenDeactivatingRuleWithFutureAppointments() throws Exception {
		UUID ruleId = createRuleAndGetId(medecinToken, ruleBody(null, 1, "08:00", "12:00", nextMonday(), null));

		// RM-07 : un rendez-vous confirmé futur occupe le créneau de 08:00 du lundi (heure clinique).
		Instant startAt = at(nextMonday(), 8, 0);
		TenantContext.setTenantId(organization.getId());
		try {
			appointmentRepository.saveAndFlush(new AppointmentEntity(
					medecin, patient, startAt, startAt.plus(30, ChronoUnit.MINUTES), "Consultation de suivi"));
		} finally {
			TenantContext.clear();
		}

		mockMvc.perform(delete("/api/availabilities/" + ruleId)
						.header("Authorization", "Bearer " + medecinToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("AVAILABILITY_CONFLICT"));

		TenantContext.setTenantId(organization.getId());
		try {
			DoctorAvailabilityEntity persisted = availabilityRepository.findById(ruleId)
					.orElseThrow(() -> new AssertionError("La règle doit être conservée en base"));
			assertTrue(persisted.getActive(), "La règle doit rester active après un refus de désactivation (RM-07)");
		} finally {
			TenantContext.clear();
		}
	}

	@Test
	void shouldReturn404ForRuleOfAnotherTenant() throws Exception {
		DoctorAvailabilityEntity externalRule;
		TenantContext.setTenantId(otherOrganization.getId());
		try {
			externalRule = availabilityRepository.saveAndFlush(new DoctorAvailabilityEntity(
					medecinExterne, 1, LocalTime.of(8, 0), LocalTime.of(12, 0), nextMonday(), null));
		} finally {
			TenantContext.clear();
		}

		mockMvc.perform(put("/api/availabilities/" + externalRule.getId())
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								ruleBody(null, 1, "09:00", "12:00", nextMonday(), null))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("AVAILABILITY_NOT_FOUND"));

		mockMvc.perform(delete("/api/availabilities/" + externalRule.getId())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("AVAILABILITY_NOT_FOUND"));
	}

	@Test
	void shouldCreateListAndDeleteException() throws Exception {
		Instant startAt = at(nextMonday(), 13, 0);
		Instant endAt = at(nextMonday(), 15, 0);

		String response = mockMvc.perform(post("/api/availabilities/exceptions")
						.header("Authorization", "Bearer " + medecinToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								exceptionBody(null, startAt, endAt, "Congé"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctorId").value(medecin.getId().toString()))
				.andExpect(jsonPath("$.startAt").value(startAt.toString()))
				.andExpect(jsonPath("$.endAt").value(endAt.toString()))
				.andExpect(jsonPath("$.reason").value("Congé"))
				.andReturn().getResponse().getContentAsString();
		UUID exceptionId = UUID.fromString(objectMapper.readTree(response).get("id").asText());

		mockMvc.perform(get("/api/availabilities/exceptions")
						.header("Authorization", "Bearer " + medecinToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(get("/api/availabilities/exceptions")
						.param("from", startAt.minus(1, ChronoUnit.HOURS).toString())
						.param("to", endAt.plus(1, ChronoUnit.HOURS).toString())
						.header("Authorization", "Bearer " + medecinToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(delete("/api/availabilities/exceptions/" + exceptionId)
						.header("Authorization", "Bearer " + medecinToken))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/availabilities/exceptions")
						.header("Authorization", "Bearer " + medecinToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void shouldReturn400WhenRangesAreInvalid() throws Exception {
		// endTime <= startTime.
		mockMvc.perform(post("/api/availabilities")
						.header("Authorization", "Bearer " + medecinToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								ruleBody(null, 1, "12:00", "08:00", nextMonday(), null))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		// validTo < validFrom.
		mockMvc.perform(post("/api/availabilities")
						.header("Authorization", "Bearer " + medecinToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								ruleBody(null, 1, "08:00", "12:00", nextMonday(), nextMonday().minusDays(1)))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

		// Indisponibilité : endAt <= startAt.
		Instant startAt = at(nextMonday(), 14, 0);
		mockMvc.perform(post("/api/availabilities/exceptions")
						.header("Authorization", "Bearer " + medecinToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								exceptionBody(null, startAt, startAt.minus(1, ChronoUnit.HOURS), "Plage inversée"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void shouldGenerateSlotsThroughService() {
		LocalDate monday = nextMonday();
		Instant from = monday.atTime(0, 0).atZone(CLINIC_ZONE).toInstant();
		Instant to = monday.plusDays(1).atTime(0, 0).atZone(CLINIC_ZONE).toInstant();

		TenantContext.setTenantId(organization.getId());
		try {
			availabilityRepository.saveAndFlush(new DoctorAvailabilityEntity(
					medecin, 1, LocalTime.of(8, 0), LocalTime.of(10, 0), monday, null));
			exceptionRepository.saveAndFlush(new DoctorAvailabilityExceptionEntity(
					medecin, at(monday, 8, 30), at(monday, 9, 30), "Réunion"));
			appointmentRepository.saveAndFlush(new AppointmentEntity(
					medecin, patient, at(monday, 9, 30), at(monday, 10, 0), "Suivi"));

			List<AppointmentSlotGenerator.Slot> slots = availabilityService.generateSlots(medecin.getId(), from, to);

			assertEquals(
					List.of(at(monday, 8, 0)),
					slots.stream().map(AppointmentSlotGenerator.Slot::startAt).toList(),
					"Seul 08:00 doit rester : 08:30 et 09:00 masqués par l'indisponibilité, 09:30 réservé");
		} finally {
			TenantContext.clear();
		}
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

	private UserAccountEntity saveUser(String email, String displayName, String role, UUID organizationId) {
		UserAccountEntity user = new UserAccountEntity(email, displayName, role, "password");
		user.setOrganizationId(organizationId);
		return userAccountRepository.saveAndFlush(user);
	}

	private UUID createRuleAndGetId(String token, Map<String, Object> body) throws Exception {
		String response = mockMvc.perform(post("/api/availabilities")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(objectMapper.readTree(response).get("id").asText());
	}

	private Map<String, Object> ruleBody(
			UUID doctorId, int weekday, String startTime, String endTime, LocalDate validFrom, LocalDate validTo) {
		Map<String, Object> body = new LinkedHashMap<>();
		if (doctorId != null) {
			body.put("doctorId", doctorId.toString());
		}
		body.put("weekday", weekday);
		body.put("startTime", startTime);
		body.put("endTime", endTime);
		body.put("validFrom", validFrom.toString());
		if (validTo != null) {
			body.put("validTo", validTo.toString());
		}
		return body;
	}

	private Map<String, Object> exceptionBody(UUID doctorId, Instant startAt, Instant endAt, String reason) {
		Map<String, Object> body = new LinkedHashMap<>();
		if (doctorId != null) {
			body.put("doctorId", doctorId.toString());
		}
		body.put("startAt", startAt.toString());
		body.put("endAt", endAt.toString());
		if (reason != null) {
			body.put("reason", reason);
		}
		return body;
	}

	private static LocalDate nextMonday() {
		return LocalDate.now(CLINIC_ZONE).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
	}

	private static Instant at(LocalDate date, int hour, int minute) {
		return date.atTime(LocalTime.of(hour, minute)).atZone(CLINIC_ZONE).toInstant();
	}
}
