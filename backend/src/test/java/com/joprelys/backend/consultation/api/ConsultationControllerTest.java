package com.joprelys.backend.consultation.api;

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
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ConsultationControllerTest {

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
	private ConsultationRepository consultationRepository;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtService jwtService;

	private OrganizationEntity orgA;
	private OrganizationEntity orgB;
	private UserAccountEntity userMedecinA;
	private UserAccountEntity userMedecinB;
	private UserAccountEntity userAgentA;

	private String tokenMedecinA;
	private String tokenMedecinB;
	private String tokenAgentA;

	private PatientEntity patientA;
	private VisitEntity visitA;
	private VisitEntity visitB;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM consultations");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();

		orgA = new OrganizationEntity("Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala");
		orgB = new OrganizationEntity("Clinique B", "contactb@joprelys.local", "456", "Street B", "Yaoundé");
		orgA = organizationRepository.save(orgA);
		orgB = organizationRepository.save(orgB);

		userMedecinA = new UserAccountEntity("medecin.a@joprelys.local", "Dr. Alpha", "MEDECIN", "passhash");
		userMedecinA.setOrganizationId(orgA.getId());
		userMedecinA = userAccountRepository.save(userMedecinA);

		userMedecinB = new UserAccountEntity("medecin.b@joprelys.local", "Dr. Beta", "MEDECIN", "passhash");
		userMedecinB.setOrganizationId(orgB.getId());
		userMedecinB = userAccountRepository.save(userMedecinB);

		userAgentA = new UserAccountEntity("agent.a@joprelys.local", "Agent A", "AGENT_ACCUEIL", "passhash");
		userAgentA.setOrganizationId(orgA.getId());
		userAgentA = userAccountRepository.save(userAgentA);

		tokenMedecinA = jwtService.createToken(userMedecinA).value();
		tokenMedecinB = jwtService.createToken(userMedecinB).value();
		tokenAgentA = jwtService.createToken(userAgentA).value();

		TenantContext.setTenantId(orgA.getId());
		patientA = new PatientEntity("DPU-A", "PAT-A", "Patient Alpha", "MASCULIN",
				LocalDate.of(1985, 3, 15), "+237690000000", "Douala", "", "", "", "", "", "");
		patientA = patientRepository.save(patientA);

		visitA = new VisitEntity(patientA, "VIS-A001", "Fièvre persistante", "Médecine générale");
		visitA = visitRepository.save(visitA);
		TenantContext.clear();

		TenantContext.setTenantId(orgB.getId());
		PatientEntity patientB = new PatientEntity("DPU-B", "PAT-B", "Patient Beta", "FEMININ",
				LocalDate.of(1990, 6, 20), "+237699000000", "Yaoundé", "", "", "", "", "", "");
		patientB = patientRepository.save(patientB);
		visitB = new VisitEntity(patientB, "VIS-B001", "Consultation pré-natale", "Gynécologie");
		visitB = visitRepository.save(visitB);
		TenantContext.clear();
	}

	@Test
	void givenMedecinA_whenSaveConsultation_thenSuccess() throws Exception {
		String json = """
				{
					"symptoms": "Fièvre à 39°C, frissons, maux de tête",
					"clinicalExam": "Gorge rouge, amygdales hypertrophiées",
					"diagnosis": "Angine bactérienne",
					"advice": "Repos, hydratation, éviter les contacts",
					"followUp": "Contrôle dans 7 jours"
				}
				""";

		mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenMedecinA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.documentNumber").value(org.hamcrest.Matchers.startsWith("DOC-CONS-")))
				.andExpect(jsonPath("$.diagnosis").value("Angine bactérienne"))
				.andExpect(jsonPath("$.symptoms").value("Fièvre à 39°C, frissons, maux de tête"))
				.andExpect(jsonPath("$.doctorName").value("Dr. Alpha"))
				.andExpect(jsonPath("$.status").value("BROUILLON"))
				.andExpect(jsonPath("$.visitNumber").value("VIS-A001"));
	}

	@Test
	void givenExistingConsultation_whenSaveAgain_thenUpsertSuccess() throws Exception {
		String json1 = """
				{
					"symptoms": "Douleur thoracique",
					"diagnosis": "Suspicion angine de poitrine"
				}
				""";
		mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenMedecinA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json1))
				.andExpect(status().isOk());

		String json2 = """
				{
					"symptoms": "Douleur thoracique irradiant dans le bras gauche",
					"clinicalExam": "ECG normal, auscultation normale",
					"diagnosis": "Douleur musculo-squelettique",
					"advice": "Repos, antalgiques",
					"followUp": "Bilan cardiologique si récidive"
				}
				""";
		mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenMedecinA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json2))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diagnosis").value("Douleur musculo-squelettique"))
				.andExpect(jsonPath("$.clinicalExam").value("ECG normal, auscultation normale"));
	}

	@Test
	void givenMedecinA_whenGetConsultation_thenSuccess() throws Exception {
		TenantContext.setTenantId(orgA.getId());
		ConsultationEntity entity = new ConsultationEntity(
				visitA, userMedecinA, "DOC-CONS-20260702-000001",
				"Toux chronique", "Poumons sains à l'auscultation",
				"Bronchite virale", "Sirop, repos", "Contrôle si aggravation");
		consultationRepository.save(entity);
		TenantContext.clear();

		mockMvc.perform(get("/api/visits/" + visitA.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenMedecinA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diagnosis").value("Bronchite virale"))
				.andExpect(jsonPath("$.doctorName").value("Dr. Alpha"))
				.andExpect(jsonPath("$.visitNumber").value("VIS-A001"));
	}

	@Test
	void givenAgentAccueil_whenSaveConsultation_thenForbidden() throws Exception {
		String json = """
				{
					"symptoms": "Tentative non autorisée",
					"diagnosis": "Interdit"
				}
				""";

		mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isForbidden());
	}

	@Test
	void givenMissingSymptoms_whenSaveConsultation_thenBadRequest() throws Exception {
		String json = """
				{
					"diagnosis": "Diagnostic sans symptômes"
				}
				""";

		mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenMedecinA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isBadRequest());
	}

	@Test
	void givenMissingDiagnosis_whenSaveConsultation_thenBadRequest() throws Exception {
		String json = """
				{
					"symptoms": "Symptômes sans diagnostic"
				}
				""";

		mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenMedecinA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isBadRequest());
	}

	@Test
	void givenUnknownVisit_whenSaveConsultation_thenNotFound() throws Exception {
		UUID unknownId = UUID.randomUUID();
		String json = """
				{
					"symptoms": "Symptômes valides",
					"diagnosis": "Diagnostic valide"
				}
				""";

		mockMvc.perform(post("/api/visits/" + unknownId + "/consultation")
				.header("Authorization", "Bearer " + tokenMedecinA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isNotFound());
	}

	@Test
	void givenNoConsultation_whenGetConsultation_thenNoContent() throws Exception {
		mockMvc.perform(get("/api/visits/" + visitA.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenMedecinA))
				.andExpect(status().isNoContent());
	}

	@Test
	void givenNoToken_whenSaveConsultation_thenUnauthorized() throws Exception {
		String json = """
				{
					"symptoms": "Symptômes",
					"diagnosis": "Diagnostic"
				}
				""";

		mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void givenMedecinB_whenSaveConsultationOnVisitA_thenNotFound() throws Exception {
		String json = """
				{
					"symptoms": "Tentative cross-tenant",
					"diagnosis": "Accès interdit"
				}
				""";

		mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenMedecinB)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isNotFound());
	}

	@Test
	void givenClosedVisit_whenSaveConsultation_thenBadRequest() throws Exception {
		TenantContext.setTenantId(orgA.getId());
		visitA.setStatus("TERMINEE");
		visitRepository.save(visitA);
		TenantContext.clear();

		String json = """
				{
					"symptoms": "Symptômes sur visite clôturée",
					"diagnosis": "Ne devrait pas passer"
				}
				""";

		mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenMedecinA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(
						"Une consultation ne peut être saisie que sur une visite active (EN_COURS)."));
	}
}
