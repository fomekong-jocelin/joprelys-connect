package com.joprelys.backend.visit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VisitCareFlowControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private VisitRepository visitRepository;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtService jwtService;

	private String tokenAgent;
	private String tokenDoctorA;
	private String tokenDoctorB;
	private VisitEntity visit;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();

		OrganizationEntity org = organizationRepository.save(
				new OrganizationEntity("Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala"));

		UserAccountEntity agent = new UserAccountEntity("agent@joprelys.local", "Agent", "AGENT_ACCUEIL", "passhash");
		agent.setOrganizationId(org.getId());
		UserAccountEntity doctorA = new UserAccountEntity("doc.a@joprelys.local", "Dr Alpha", "MEDECIN", "passhash");
		doctorA.setOrganizationId(org.getId());
		UserAccountEntity doctorB = new UserAccountEntity("doc.b@joprelys.local", "Dr Beta", "MEDECIN", "passhash");
		doctorB.setOrganizationId(org.getId());
		tokenAgent = jwtService.createToken(userAccountRepository.save(agent)).value();
		tokenDoctorA = jwtService.createToken(userAccountRepository.save(doctorA)).value();
		tokenDoctorB = jwtService.createToken(userAccountRepository.save(doctorB)).value();

		TenantContext.setTenantId(org.getId());
		PatientEntity patient = patientRepository.save(new PatientEntity(
				"DPU-A", "PAT-A", "Patient A", "MASCULIN", LocalDate.of(1990, 1, 1), "+123", "Douala", "", "", "", "", "", ""));
		visit = visitRepository.save(new VisitEntity(patient, "VIS-A", "Fièvre", "CONSULTATION"));
		TenantContext.clear();
	}

	@Test
	void vitalsMoveVisitToReadyAndAreKeptAsHistoryWithAlerts() throws Exception {
		postVitals("{\"temperature\": 39.2, \"systolic\": 120, \"diastolic\": 80}");
		postVitals("{\"temperature\": 37.4, \"spo2\": 88}");

		mockMvc.perform(get("/api/visits/" + visit.getId())
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.careStage").value("PRET_MEDECIN"))
				.andExpect(jsonPath("$.vitals.alerts[0].code").value("SPO2_LOW"))
				.andExpect(jsonPath("$.vitals.alerts[0].severity").value("CRITICAL"));

		mockMvc.perform(get("/api/visits/" + visit.getId() + "/vitals/history")
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].spo2").value(88))
				.andExpect(jsonPath("$[0].recordedByName").value("Agent"))
				.andExpect(jsonPath("$[1].temperature").value(39.2))
				.andExpect(jsonPath("$[1].alerts[0].code").value("TEMPERATURE_HIGH"));
	}

	@Test
	void onlyOnePractitionerHoldsTheConsultationUnlessExplicitTakeOver() throws Exception {
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.careStage").value("EN_CONSULTATION"))
				.andExpect(jsonPath("$.consultingPractitionerName").value("Dr Alpha"));

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Dr Alpha")));

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenDoctorB)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symptoms\": \"Fièvre\", \"diagnosis\": \"Paludisme\"}"))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
				.param("takeOver", "true")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.consultingPractitionerName").value("Dr Beta"));
	}

	@Test
	void practitionerCanPutPatientBackInQueue() throws Exception {
		postVitals("{\"temperature\": 37.0}");
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/release")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/release")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.careStage").value("PRET_MEDECIN"))
				.andExpect(jsonPath("$.consultingPractitionerId").doesNotExist());
	}

	@Test
	void savingConsultationWithoutTakeChargeImplicitlyAssignsTheDoctor() throws Exception {
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenDoctorA)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symptoms\": \"Fièvre\", \"diagnosis\": \"Paludisme\"}"))
				.andExpect(status().is2xxSuccessful());

		mockMvc.perform(get("/api/visits/" + visit.getId())
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(jsonPath("$.careStage").value("EN_CONSULTATION"))
				.andExpect(jsonPath("$.consultingPractitionerName").value("Dr Alpha"));
	}

	@Test
	void mineScopeListsOnlyPatientsHeldByTheConnectedPractitioner() throws Exception {
		mockMvc.perform(get("/api/visits/active").param("scope", "MINE")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/visits/active").param("scope", "MINE")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/visits/active").param("scope", "MINE")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/visits/active")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void receptionAgentGetsServicesAndCliniciansWithoutStaffManagementRights() throws Exception {
		mockMvc.perform(get("/api/visits/admission-options")
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.services").isNotEmpty())
				.andExpect(jsonPath("$.practitioners.length()").value(2))
				.andExpect(jsonPath("$.practitioners[*].displayName",
						org.hamcrest.Matchers.containsInAnyOrder("Dr Alpha", "Dr Beta")));

		mockMvc.perform(get("/api/staff")
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(status().isForbidden());
	}

	private void postVitals(String json) throws Exception {
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/vitals")
				.header("Authorization", "Bearer " + tokenAgent)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isOk());
	}
}
