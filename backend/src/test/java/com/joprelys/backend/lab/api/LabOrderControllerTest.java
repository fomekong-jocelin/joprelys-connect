package com.joprelys.backend.lab.api;

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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LabOrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private VisitRepository visitRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtService jwtService;

	private OrganizationEntity org;
	private PatientEntity patient;
	private VisitEntity visit;
	private UserAccountEntity doctor;
	private UserAccountEntity patientUser;
	private UserAccountEntity biologist;
	
	private String tokenDoctor;
	private String tokenPatient;
	private String tokenBiologist;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM lab_orders");
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM medical_documents");
		jdbcTemplate.update("DELETE FROM prescription_items");
		jdbcTemplate.update("DELETE FROM prescriptions");
		jdbcTemplate.update("DELETE FROM consultations");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();

		// 1. Créer Organisation
		org = new OrganizationEntity("Clinique Pilote", "contact@pilote.org", "123456", "Adresse", "Yaoundé");
		org = organizationRepository.save(org);

		// 2. Créer Médecin et Patient User
		doctor = new UserAccountEntity("medecin@joprelys.local", "Dr. Martin", "MEDECIN", "passhash");
		doctor.setOrganizationId(org.getId());
		doctor = userAccountRepository.save(doctor);
		tokenDoctor = jwtService.createToken(doctor).value();

		patientUser = new UserAccountEntity("patient@joprelys.local", "Jean Patient", "PATIENT", "passhash");
		patientUser.setOrganizationId(org.getId());
		patientUser = userAccountRepository.save(patientUser);
		tokenPatient = jwtService.createToken(patientUser).value();

		biologist = new UserAccountEntity("biologiste@joprelys.local", "Dr. Biologiste", "BIOLOGISTE", "passhash");
		biologist.setOrganizationId(org.getId());
		biologist = userAccountRepository.save(biologist);
		tokenBiologist = jwtService.createToken(biologist).value();

		TenantContext.setTenantId(org.getId());

		// 3. Créer Patient
		patient = new PatientEntity(
				"PAT-20260703-000001",
				"LOC-001",
				"Jean Patient",
				"MASCULIN",
				LocalDate.of(1985, 12, 25),
				"+237 600 00 00 00",
				"Yaoundé",
				"Bastos",
				"Rue 5",
				"Marie Parent",
				"+237 699 99 99 99",
				"Aucune",
				"Aucun"
		);
		patient = patientRepository.save(patient);

		// 4. Créer Visite
		visit = new VisitEntity(patient, "VIS-20260703-000001", "Consultation de tri", "GENERAL");
		visit.setOrganizationId(org.getId());
		visit = visitRepository.save(visit);
	}

	@Test
	void givenDoctor_whenCreateLabOrder_thenSuccess() throws Exception {
		String jsonRequest = """
				{
					"patientId": "%s",
					"visitId": "%s",
					"examType": "LABORATOIRE",
					"exams": ["NFS", "GLYSEMIE_A_JEUN", "CHOLESTEROL"],
					"reason": "Bilan métabolique",
					"priority": "NORMALE"
				}
				""".formatted(patient.getId(), visit.getId());

		mockMvc.perform(post("/api/lab-orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDoctor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.examRequestNumber").exists())
				.andExpect(jsonPath("$.patientId").value(patient.getId().toString()))
				.andExpect(jsonPath("$.patientName").value("Jean Patient"))
				.andExpect(jsonPath("$.visitId").value(visit.getId().toString()))
				.andExpect(jsonPath("$.requesterPractitionerId").value(doctor.getId().toString()))
				.andExpect(jsonPath("$.requesterPractitionerName").value("Dr. Martin"))
				.andExpect(jsonPath("$.examType").value("LABORATOIRE"))
				.andExpect(jsonPath("$.exams[0]").value("NFS"))
				.andExpect(jsonPath("$.exams[1]").value("GLYSEMIE_A_JEUN"))
				.andExpect(jsonPath("$.exams[2]").value("CHOLESTEROL"))
				.andExpect(jsonPath("$.status").value("REQUESTED"))
				.andExpect(jsonPath("$.priority").value("NORMALE"));
	}

	@Test
	void givenPatient_whenCreateLabOrder_thenForbidden() throws Exception {
		String jsonRequest = """
				{
					"patientId": "%s",
					"examType": "LABORATOIRE",
					"exams": ["NFS"]
				}
				""".formatted(patient.getId());

		mockMvc.perform(post("/api/lab-orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPatient)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isForbidden());
	}

	@Test
	void givenDoctor_whenGetPatientLabOrders_thenReturnsList() throws Exception {
		// Simuler une création via MockMvc d'abord
		String jsonRequest = """
				{
					"patientId": "%s",
					"examType": "LABORATOIRE",
					"exams": ["NFS"]
				}
				""".formatted(patient.getId());

		mockMvc.perform(post("/api/lab-orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDoctor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isCreated());

		// Récupérer la liste
		mockMvc.perform(get("/api/lab-orders/patient/" + patient.getId())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDoctor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].exams[0]").value("NFS"));
	}

	@Test
	void givenDoctor_whenGetPatientResults_thenReturnsList() throws Exception {
		mockMvc.perform(get("/api/lab-orders/patient/" + patient.getId() + "/results")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDoctor))
				.andExpect(status().isOk());
	}

	@Test
	void givenDoctor_whenGetLabOrdersQueue_thenForbidden() throws Exception {
		mockMvc.perform(get("/api/lab-orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDoctor))
				.andExpect(status().isForbidden());
	}

	@Test
	void givenBiologist_whenGetLabOrdersQueue_thenSuccess() throws Exception {
		mockMvc.perform(get("/api/lab-orders")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBiologist))
				.andExpect(status().isOk());
	}
}
