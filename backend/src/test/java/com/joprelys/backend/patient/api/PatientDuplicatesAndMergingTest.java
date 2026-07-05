package com.joprelys.backend.patient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientDuplicateCandidateRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientMergedHistoryRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
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
public class PatientDuplicatesAndMergingTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private PatientDuplicateCandidateRepository duplicateCandidateRepository;

	@Autowired
	private PatientMergedHistoryRepository mergedHistoryRepository;

	@Autowired
	private VisitRepository visitRepository;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtService jwtService;

	private OrganizationEntity orgA;
	private UserAccountEntity userAgentA;
	private UserAccountEntity userAdminCliniqueA;

	private String tokenAgentA;
	private String tokenAdminCliniqueA;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM patient_duplicate_candidates");
		jdbcTemplate.update("DELETE FROM patient_merged_history");
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM medical_documents");
		jdbcTemplate.update("DELETE FROM prescription_items");
		jdbcTemplate.update("DELETE FROM prescriptions");
		jdbcTemplate.update("DELETE FROM consultations");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();

		// Create Organization
		orgA = new OrganizationEntity("Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala");
		orgA = organizationRepository.save(orgA);

		// Create Users
		userAgentA = new UserAccountEntity("agent.a@joprelys.local", "Agent A", "AGENT_ACCUEIL", "passhash");
		userAgentA.setOrganizationId(orgA.getId());
		userAgentA = userAccountRepository.save(userAgentA);

		userAdminCliniqueA = new UserAccountEntity("admin.a@joprelys.local", "Admin Clinique A", "ADMIN_CLINIQUE", "passhash");
		userAdminCliniqueA.setOrganizationId(orgA.getId());
		userAdminCliniqueA = userAccountRepository.save(userAdminCliniqueA);

		// Generate Tokens
		tokenAgentA = jwtService.createToken(userAgentA).value();
		tokenAdminCliniqueA = jwtService.createToken(userAdminCliniqueA).value();
	}

	private PatientEntity createPatientViaMockMvc(String fullName, String birthDate, String phone, String token) throws Exception {
		String jsonRequest = String.format("""
				{
					"fullName": "%s",
					"gender": "MASCULIN",
					"birthDate": "%s",
					"phone": %s,
					"city": "Douala"
				}
				""", fullName, birthDate, phone == null ? "null" : "\"" + phone + "\"");

		String response = mockMvc.perform(post("/api/patients")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String idStr = response.split("\"id\":\"")[1].split("\"")[0];
		return patientRepository.findByIdGlobally(UUID.fromString(idStr)).orElseThrow();
	}

	@Test
	void whenCreatePatientWithoutPhone_thenSuccess() throws Exception {
		String jsonRequest = """
				{
					"fullName": "Jean Dupont",
					"gender": "MASCULIN",
					"birthDate": "1990-05-15",
					"phone": null,
					"city": "Douala",
					"district": "Akwa"
				}
				""";

		mockMvc.perform(post("/api/patients")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fullName").value("Jean Dupont"))
				.andExpect(jsonPath("$.phone").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void whenCreateSimilarPatient_thenCandidateDuplicateIsCreated() throws Exception {
		// 1. Create first patient
		createPatientViaMockMvc("Jean Dupont", "1990-05-15", "+237699999999", tokenAgentA);

		// 2. Create second patient (similar name, same birthdate)
		createPatientViaMockMvc("Jean Dupond", "1990-05-15", "+237699999998", tokenAgentA);

		// 3. Admin Clinique lists duplicates
		mockMvc.perform(get("/api/patients/duplicates")
				.header("Authorization", "Bearer " + tokenAdminCliniqueA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].similarityScore").value(org.hamcrest.Matchers.greaterThanOrEqualTo(85.0)))
				.andExpect(jsonPath("$[0].status").value("PENDING"));
	}

	@Test
	void whenMergeSimilarPatients_thenReassignVisitsAndDisableSecondary() throws Exception {
		// 1. Create two patients via MockMvc
		var p1 = createPatientViaMockMvc("Jean Dupont", "1990-05-15", "+237699999999", tokenAgentA);
		var p2 = createPatientViaMockMvc("Jean Dupond", "1990-05-15", "+237699999998", tokenAgentA);

		// 2. Fetch the automatically created duplicate candidate
		var candidate = duplicateCandidateRepository.findByPatientPair(p1.getId(), p2.getId())
				.orElseThrow(() -> new AssertionError("Le doublon candidat aurait dû être créé automatiquement"));

		// 3. Create a visit for the secondary patient (p2)
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		var visitObj = new VisitEntity(p2, "VIS-000001", "Consulter", "Medecine Generale");
		visitObj.setOrganizationId(orgA.getId());
		var visit = visitRepository.save(visitObj);

		// 4. Merge patients: p1 is primary, p2 is secondary
		String mergeRequest = String.format("""
				{
					"primaryId": "%s",
					"secondaryId": "%s"
				}
				""", p1.getId(), p2.getId());

		mockMvc.perform(post("/api/patients/merge")
				.header("Authorization", "Bearer " + tokenAdminCliniqueA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mergeRequest))
				.andExpect(status().isNoContent());

		// 5. Verify secondary status is MERGED
		var updatedP2 = patientRepository.findByIdGlobally(p2.getId()).orElseThrow();
		assertEquals("MERGED", updatedP2.getStatus());

		// 6. Verify visit is reassigned to primary
		var updatedVisit = visitRepository.findByIdGlobally(visit.getId()).orElseThrow();
		assertEquals(p1.getId(), updatedVisit.getPatient().getId());

		// 7. Verify duplicate candidate status is RESOLVED
		var updatedCandidate = duplicateCandidateRepository.findById(candidate.getId()).orElseThrow();
		assertEquals("RESOLVED", updatedCandidate.getStatus());

		// 8. Verify merged history is populated
		var history = mergedHistoryRepository.findAll();
		assertEquals(1, history.size());
		assertEquals(p1.getId(), history.get(0).getPrimaryPatient().getId());
		assertEquals(p2.getId(), history.get(0).getMergedPatientId());
		assertEquals(p2.getGlobalPatientNumber(), history.get(0).getMergedPatientDpu());
		assertEquals(userAdminCliniqueA.getId(), history.get(0).getMergedBy());
	}
}