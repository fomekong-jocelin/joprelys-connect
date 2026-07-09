package com.joprelys.backend.lab.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository;
import com.joprelys.backend.lab.infrastructure.persistence.ExamType;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
public class LabIntegrationControllerTest {

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
	private LabOrderRepository labOrderRepository;

	@Autowired
	private LabResultRepository labResultRepository;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	private OrganizationEntity org;
	private PatientEntity patient;
	private VisitEntity visit;
	private UserAccountEntity doctor;
	private LabOrderEntity labOrder;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM lab_results");
		jdbcTemplate.update("DELETE FROM lab_orders");
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();

		// 1. Créer Organisation
		org = new OrganizationEntity("Clinique Test Lab", "contact@testlab.org", "123456", "Adresse", "Yaoundé");
		org = organizationRepository.save(org);

		// 2. Créer Médecin
		doctor = new UserAccountEntity("doctor@testlab.org", "Dr. Martin", "MEDECIN", "passhash");
		doctor.setOrganizationId(org.getId());
		doctor = userAccountRepository.save(doctor);

		TenantContext.setTenantId(org.getId());

		// 3. Créer Patient
		patient = new PatientEntity(
				"PAT-20260703-999999",
				"LOC-999",
				"Jean Patient Test",
				"MASCULIN",
				LocalDate.of(1990, 1, 1),
				"+237 699 99 99 99",
				"Douala",
				"Littoral",
				"Bonapriso",
				"Marie Patient",
				"+237 688 88 88 88",
				null,
				null
		);
		patient.setOrganizationId(org.getId());
		patient = patientRepository.save(patient);

		// 4. Créer Visite
		visit = new VisitEntity(patient, "VIS-20260703-999999", "Bilan sanguin général", "Médecine Générale");
		visit.setOrganizationId(org.getId());
		visit = visitRepository.save(visit);

		// 5. Créer Demande d'examen (LabOrder)
		labOrder = new LabOrderEntity(
				"EXAM-REQ-20260703-000042",
				patient,
				visit,
				doctor,
				null,
				ExamType.LABORATOIRE,
				List.of("GLYSEMIE_A_JEUN"),
				"Suspicion de diabète",
				"NORMALE",
				org.getId()
		);
		labOrder.setOrganizationId(org.getId());
		labOrder = labOrderRepository.save(labOrder);
	}

	@Test
	void givenNoApiKey_whenUpload_thenUnauthorized() throws Exception {
		String payload = """
				{
				  "examRequestNumber": "%s",
				  "validatorName": "Dr. Biologiste",
				  "results": [],
				  "pdfBase64": ""
				}
				""".formatted(labOrder.getExamRequestNumber());

		mockMvc.perform(post("/api/public/lab-integration/upload")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void givenBadApiKey_whenUpload_thenUnauthorized() throws Exception {
		String payload = """
				{
				  "examRequestNumber": "%s",
				  "validatorName": "Dr. Biologiste",
				  "results": [],
				  "pdfBase64": ""
				}
				""".formatted(labOrder.getExamRequestNumber());

		mockMvc.perform(post("/api/public/lab-integration/upload")
						.header("X-API-KEY", "mauvaise-cle")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void givenValidApiKeyButInexistentOrder_whenUpload_thenNotFound() throws Exception {
		String payload = """
				{
				  "examRequestNumber": "EXAM-REQ-INEXISTANT",
				  "validatorName": "Dr. Biologiste",
				  "results": [],
				  "pdfBase64": ""
				}
				""";

		mockMvc.perform(post("/api/public/lab-integration/upload")
						.header("X-API-KEY", "lab-partner-secret-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isNotFound());
	}

	@Test
	void givenValidRequest_whenUpload_thenCreatedAndStateUpdated() throws Exception {
		// Mock PDF Base64 simple ("%PDF-1.4\nHello PDF\n" en base64)
		String mockPdfBase64 = "JVBERi0xLjQKSGVsbG8gUERGCg==";

		String payload = """
				{
				  "examRequestNumber": "%s",
				  "validatorName": "Dr. Jean Biologiste",
				  "sampleCollectedAt": "2026-07-03T07:30:00Z",
				  "resultAt": "2026-07-03T10:00:00Z",
				  "validatedAt": "2026-07-03T10:15:00Z",
				  "conclusion": "Glycémie élevée",
				  "results": [
				    {
				      "analyteName": "Glucose à jeun",
				      "value": "1.45",
				      "unit": "g/L",
				      "referenceRange": "0.70 - 1.10",
				      "interpretation": "ELEVE",
				      "comment": "Patient à jeun"
				    }
				  ],
				  "pdfBase64": "%s"
				}
				""".formatted(labOrder.getExamRequestNumber(), mockPdfBase64);

		mockMvc.perform(post("/api/public/lab-integration/upload")
						.header("X-API-KEY", "lab-partner-secret-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated());

		// Restaurer le TenantContext après l'appel MockMvc car il a été nettoyé par le filtre JWT
		TenantContext.setTenantId(org.getId());

		// Vérifications :
		// 1. Statut du LabOrder passé à VALIDATED
		LabOrderEntity updatedOrder = labOrderRepository.findById(labOrder.getId()).orElseThrow();
		assertEquals(LabOrderStatus.VALIDATED, updatedOrder.getStatus());

		// 2. Création du résultat d'analyse
		List<LabResultEntity> results = labResultRepository.findAll();
		assertEquals(1, results.size());
		LabResultEntity res = results.getFirst();
		assertEquals("Glucose à jeun", res.getAnalyteName());
		assertEquals("1.45", res.getValue());
		assertEquals("g/L", res.getUnit());
		assertEquals("ELEVE", res.getInterpretation());
		assertEquals("Dr. Jean Biologiste", res.getValidatorName());
		assertNotNull(res.getPdfFilePath());

		// 3. Fichier physique écrit sur le disque
		assertTrue(Files.exists(Paths.get(res.getPdfFilePath())));
		byte[] fileBytes = Files.readAllBytes(Paths.get(res.getPdfFilePath()));
		assertEquals("%PDF-1.4\nHello PDF\n", new String(fileBytes));

		// Nettoyage du fichier créé
		Files.deleteIfExists(Paths.get(res.getPdfFilePath()));

		// 4. Trace d'audit présente
		Integer auditCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM audit_logs WHERE action = 'UPLOAD_LAB_RESULTS' AND patient_id = ?",
				Integer.class,
				patient.getId()
		);
		assertEquals(1, auditCount);
	}

	@Test
	void givenInvalidPdfHeader_whenUpload_thenBadRequest() throws Exception {
		// Mock PDF Base64 simple but without %PDF header ("Hello PDF" in base64)
		String mockPdfBase64 = "SGVsbG8gUERGCg==";

		String payload = """
				{
				  "examRequestNumber": "%s",
				  "validatorName": "Dr. Jean Biologiste",
				  "results": [],
				  "pdfBase64": "%s"
				}
				""".formatted(labOrder.getExamRequestNumber(), mockPdfBase64);

		mockMvc.perform(post("/api/public/lab-integration/upload")
						.header("X-API-KEY", "lab-partner-secret-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest());
	}

	@Test
	void givenInvalidMimeTypePrefix_whenUpload_thenBadRequest() throws Exception {
		// Mock PDF Base64 simple but with wrong metadata prefix ("data:image/png;base64,...")
		String mockPdfBase64 = "data:image/png;base64,JVBERi0xLjQKSGVsbG8gUERGCg==";

		String payload = """
				{
				  "examRequestNumber": "%s",
				  "validatorName": "Dr. Jean Biologiste",
				  "results": [],
				  "pdfBase64": "%s"
				}
				""".formatted(labOrder.getExamRequestNumber(), mockPdfBase64);

		mockMvc.perform(post("/api/public/lab-integration/upload")
						.header("X-API-KEY", "lab-partner-secret-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testVersioningAndImmutability() throws Exception {
		String mockPdfBase64 = "JVBERi0xLjQKSGVsbG8gUERGCg==";

		// 1. Upload initial (Version 1)
		String payloadV1 = """
				{
				  "examRequestNumber": "%s",
				  "validatorName": "Dr. Jean Biologiste",
				  "sampleCollectedAt": "2026-07-03T07:30:00Z",
				  "resultAt": "2026-07-03T10:00:00Z",
				  "validatedAt": "2026-07-03T10:15:00Z",
				  "conclusion": "Glycémie élevée",
				  "status": "VALIDATED",
				  "results": [
				    {
				      "analyteName": "Glucose à jeun",
				      "value": "1.45",
				      "unit": "g/L",
				      "referenceRange": "0.70 - 1.10",
				      "interpretation": "ELEVE",
				      "comment": "Patient à jeun"
				    }
				  ],
				  "pdfBase64": "%s"
				}
				""".formatted(labOrder.getExamRequestNumber(), mockPdfBase64);

		mockMvc.perform(post("/api/public/lab-integration/upload")
						.header("X-API-KEY", "lab-partner-secret-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payloadV1))
				.andExpect(status().isCreated());

		TenantContext.setTenantId(org.getId());
		List<LabResultEntity> resultsV1 = labResultRepository.findAll();
		assertEquals(1, resultsV1.size());
		LabResultEntity firstResult = resultsV1.getFirst();
		assertEquals(1, firstResult.getVersion());
		assertEquals("VALIDATED", firstResult.getStatus().name());

		// 2. Upload correctif (Création d'une Version 2 à cause du statut VALIDATED)
		String payloadV2 = """
				{
				  "examRequestNumber": "%s",
				  "validatorName": "Dr. Jean Biologiste",
				  "sampleCollectedAt": "2026-07-03T07:30:00Z",
				  "resultAt": "2026-07-03T10:00:00Z",
				  "validatedAt": "2026-07-03T10:15:00Z",
				  "conclusion": "Glycémie rectifiée",
				  "status": "VALIDATED",
				  "results": [
				    {
				      "analyteName": "Glucose à jeun",
				      "value": "1.35",
				      "unit": "g/L",
				      "referenceRange": "0.70 - 1.10",
				      "interpretation": "ELEVE",
				      "comment": "Patient à jeun"
				    }
				  ],
				  "pdfBase64": "%s"
				}
				""".formatted(labOrder.getExamRequestNumber(), mockPdfBase64);

		mockMvc.perform(post("/api/public/lab-integration/upload")
						.header("X-API-KEY", "lab-partner-secret-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payloadV2))
				.andExpect(status().isCreated());

		TenantContext.setTenantId(org.getId());
		List<LabResultEntity> allResults = labResultRepository.findAll();
		assertEquals(2, allResults.size()); // Version 1 et Version 2 coexistent

		LabResultEntity v2Result = allResults.stream()
				.filter(r -> r.getVersion() == 2)
				.findFirst().orElseThrow();
		assertEquals("1.35", v2Result.getValue());
		assertEquals(firstResult.getId(), v2Result.getParentResult().getId());

		// Nettoyer les fichiers créés
		Files.deleteIfExists(Paths.get(firstResult.getPdfFilePath()));
		Files.deleteIfExists(Paths.get(v2Result.getPdfFilePath()));
	}
}
