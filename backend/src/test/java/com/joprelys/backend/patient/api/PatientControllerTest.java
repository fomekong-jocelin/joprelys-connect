package com.joprelys.backend.patient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.LocalDate;
import java.util.UUID;
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
public class PatientControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtService jwtService;

	private OrganizationEntity orgA;
	private OrganizationEntity orgB;
	private UserAccountEntity userAgentA;
	private UserAccountEntity userAgentB;
	private UserAccountEntity userAdminJoprelys;

	private String tokenAgentA;
	private String tokenAgentB;
	private String tokenAdminJoprelys;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM medical_documents");
		jdbcTemplate.update("DELETE FROM prescription_items");
		jdbcTemplate.update("DELETE FROM prescriptions");
		jdbcTemplate.update("DELETE FROM consultations");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();

		// Create Organizations
		orgA = new OrganizationEntity("Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala");
		orgB = new OrganizationEntity("Clinique B", "contactb@joprelys.local", "456", "Street B", "Yaoundé");
		orgA = organizationRepository.save(orgA);
		orgB = organizationRepository.save(orgB);

		// Create Users
		userAgentA = new UserAccountEntity("agent.a@joprelys.local", "Agent A", "AGENT_ACCUEIL", "passhash");
		userAgentA.setOrganizationId(orgA.getId());
		userAgentA = userAccountRepository.save(userAgentA);

		userAgentB = new UserAccountEntity("agent.b@joprelys.local", "Agent B", "AGENT_ACCUEIL", "passhash");
		userAgentB.setOrganizationId(orgB.getId());
		userAgentB = userAccountRepository.save(userAgentB);

		userAdminJoprelys = new UserAccountEntity("admin.jop@joprelys.local", "Admin Joprelys", "ADMIN_JOPRELYS", "passhash");
		userAdminJoprelys = userAccountRepository.save(userAdminJoprelys);

		// Generate Tokens
		tokenAgentA = jwtService.createToken(userAgentA).value();
		tokenAgentB = jwtService.createToken(userAgentB).value();
		tokenAdminJoprelys = jwtService.createToken(userAdminJoprelys).value();
	}

	@Test
	void givenAgentA_whenCreatePatient_thenSuccessWithDPUAndLocalId() throws Exception {
		String jsonRequest = """
				{
					"fullName": "Jean Dupont",
					"gender": "MASCULIN",
					"birthDate": "1990-05-15",
					"phone": "+237699999999",
					"city": "Douala",
					"district": "Akwa",
					"allergies": "Pénicilline",
					"medicalHistory": "Hypertension"
				}
				""";

		mockMvc.perform(post("/api/patients")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fullName").value("Jean Dupont"))
				.andExpect(jsonPath("$.globalPatientNumber").value(org.hamcrest.Matchers.startsWith("DPU-JOP-")))
				.andExpect(jsonPath("$.localPatientNumber").value(org.hamcrest.Matchers.startsWith("PAT-")))
				.andExpect(jsonPath("$.allergies").value("Pénicilline"));
	}

	@Test
	void givenAgentA_whenCreateTwoPatients_thenSequencesAreSequential() throws Exception {
		String jsonRequest1 = """
				{
					"fullName": "Patient Un",
					"gender": "MASCULIN",
					"birthDate": "1980-01-01",
					"phone": "+237600000001",
					"city": "Douala"
				}
				""";

		String jsonRequest2 = """
				{
					"fullName": "Patient Deux",
					"gender": "FEMININ",
					"birthDate": "1985-02-02",
					"phone": "+237600000002",
					"city": "Douala"
				}
				""";

		mockMvc.perform(post("/api/patients")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest1))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.globalPatientNumber").value(org.hamcrest.Matchers.endsWith("000001")));

		mockMvc.perform(post("/api/patients")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest2))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.globalPatientNumber").value(org.hamcrest.Matchers.endsWith("000002")));
	}

	@Test
	void givenAgentA_whenSearchPatients_thenReturnOnlyTheirPatients() throws Exception {
		// Create patient in Org A
		String jsonRequestA = """
				{
					"fullName": "Patient Clinique A",
					"gender": "MASCULIN",
					"birthDate": "1990-05-15",
					"phone": "+237699999999",
					"city": "Douala"
				}
				""";
		mockMvc.perform(post("/api/patients")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequestA))
				.andExpect(status().isCreated());

		// Create patient in Org B
		String jsonRequestB = """
				{
					"fullName": "Patient Clinique B",
					"gender": "FEMININ",
					"birthDate": "1992-06-20",
					"phone": "+237688888888",
					"city": "Yaoundé"
				}
				""";
		mockMvc.perform(post("/api/patients")
				.header("Authorization", "Bearer " + tokenAgentB)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequestB))
				.andExpect(status().isCreated());

		// Search with Agent A: should only see Patient Clinique A
		mockMvc.perform(get("/api/patients")
				.header("Authorization", "Bearer " + tokenAgentA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].fullName").value("Patient Clinique A"));

		// Search with Agent B: should only see Patient Clinique B
		mockMvc.perform(get("/api/patients")
				.header("Authorization", "Bearer " + tokenAgentB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].fullName").value("Patient Clinique B"));
	}

	@Test
	void givenAdminJoprelys_whenAccessPatients_thenForbidden() throws Exception {
		mockMvc.perform(get("/api/patients")
				.header("Authorization", "Bearer " + tokenAdminJoprelys))
				.andExpect(status().isForbidden());
	}

	@Test
	void givenAgentA_whenCreateWithInvalidData_thenBadRequest() throws Exception {
		String jsonRequest = """
				{
					"fullName": "Jo",
					"gender": "MASCULIN",
					"birthDate": "2990-05-15",
					"phone": "",
					"city": ""
				}
				""";

		mockMvc.perform(post("/api/patients")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest());
	}
}
