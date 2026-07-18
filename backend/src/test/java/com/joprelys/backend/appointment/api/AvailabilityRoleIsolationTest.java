package com.joprelys.backend.appointment.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.LocalDate;
import java.util.List;
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
class AvailabilityRoleIsolationTest {

	private static final String VALID_RULE = """
			{"weekday":1,"startTime":"08:00","endTime":"12:00","validFrom":"2099-01-05"}
			""";
	private static final String VALID_EXCEPTION = """
			{"startAt":"2099-01-05T08:00:00Z","endAt":"2099-01-05T09:00:00Z","reason":"Indisponibilité"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String patientToken;
	private String mixedPatientToken;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		OrganizationEntity organization = organizationRepository.saveAndFlush(new OrganizationEntity(
				"Clinique isolation " + suffix,
				"isolation-" + suffix + "@test.local",
				"600000020",
				"Centre",
				"Douala"));

		UserAccountEntity mixedPatient = new UserAccountEntity(
				"mixed-patient-" + suffix + "@test.local",
				"Patient Mixte",
				"PATIENT,MEDECIN",
				"password");
		mixedPatient.setOrganizationId(organization.getId());
		mixedPatient = userAccountRepository.saveAndFlush(mixedPatient);
		mixedPatientToken = jwtService.createToken(mixedPatient).value();

		TenantContext.setTenantId(organization.getId());
		try {
			PatientEntity patient = patientRepository.saveAndFlush(new PatientEntity(
					"DPU-ISOL-" + suffix,
					"PAT-ISOL-" + suffix,
					"Patient Isolation",
					"MASCULIN",
					LocalDate.of(1990, 1, 1),
					"+237600000020",
					"Douala",
					null, null, null, null, null, null));
			patientToken = jwtService.createPatientToken(patient).value();
		} finally {
			TenantContext.clear();
		}
	}

	@AfterEach
	void tearDown() {
		TenantContext.clear();
	}

	@Test
	void shouldReturn403ForPatientAndMixedPatientTokensOnEveryAvailabilityEndpoint() throws Exception {
		for (String token : List.of(patientToken, mixedPatientToken)) {
			assertAvailabilityEndpointsForbidden(token);
		}
	}

	private void assertAvailabilityEndpointsForbidden(String token) throws Exception {
		UUID resourceId = UUID.randomUUID();

		mockMvc.perform(get("/api/availabilities").header("Authorization", bearer(token)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/availabilities").header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON).content(VALID_RULE))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/availabilities/" + resourceId).header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON).content(VALID_RULE))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/availabilities/" + resourceId).header("Authorization", bearer(token)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/availabilities/exceptions").header("Authorization", bearer(token)))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/availabilities/exceptions").header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON).content(VALID_EXCEPTION))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/availabilities/exceptions/" + resourceId)
						.header("Authorization", bearer(token)))
				.andExpect(status().isForbidden());
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

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}
