package com.joprelys.backend.visit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;
import javax.imageio.ImageIO;
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
public class VisitControllerTest {

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

	private OrganizationEntity orgA;
	private OrganizationEntity orgB;
	private UserAccountEntity userAgentA;
	private UserAccountEntity userAgentB;
	private UserAccountEntity userMedecinA;
	private UserAccountEntity userAdminJoprelys;

	private String tokenAgentA;
	private String tokenAgentB;
	private String tokenMedecinA;
	private String tokenAdminJoprelys;

	private PatientEntity patientA;
	private PatientEntity patientB;

	@BeforeEach
	void setUp() {
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

		userMedecinA = new UserAccountEntity("medecin.a@joprelys.local", "Médecin A", "MEDECIN", "passhash");
		userMedecinA.setOrganizationId(orgA.getId());
		userMedecinA = userAccountRepository.save(userMedecinA);

		userAdminJoprelys = new UserAccountEntity("admin.jop@joprelys.local", "Admin Joprelys", "ADMIN_JOPRELYS", "passhash");
		userAdminJoprelys = userAccountRepository.save(userAdminJoprelys);

		// Generate Tokens
		tokenAgentA = jwtService.createToken(userAgentA).value();
		tokenAgentB = jwtService.createToken(userAgentB).value();
		tokenMedecinA = jwtService.createToken(userMedecinA).value();
		tokenAdminJoprelys = jwtService.createToken(userAdminJoprelys).value();

		// Create Patients (using native save to set organizationId correctly under TenantContext)
		// Patient A in Tenant A
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		patientA = new PatientEntity("DPU-A", "PAT-A", "Patient A", "MASCULIN", LocalDate.of(1990, 1, 1), "+123", "Douala", "", "", "", "", "", "");
		patientA = patientRepository.save(patientA);

		// Patient B in Tenant B
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgB.getId());
		patientB = new PatientEntity("DPU-B", "PAT-B", "Patient B", "MASCULIN", LocalDate.of(1992, 2, 2), "+456", "Yaounde", "", "", "", "", "", "");
		patientB = patientRepository.save(patientB);

		com.joprelys.backend.auth.security.TenantContext.clear();
	}

	@Test
	void givenAgentA_whenCreateVisit_thenSuccess() throws Exception {
		String jsonRequest = String.format("""
				{
					"patientId": "%s",
					"reason": "Consultation générale pour fièvre",
					"orientation": "Médecine générale"
				}
				""", patientA.getId());

		mockMvc.perform(post("/api/visits")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visitNumber").value(org.hamcrest.Matchers.startsWith("VIS-")))
				.andExpect(jsonPath("$.patientName").value("Patient A"))
				.andExpect(jsonPath("$.reason").value("Consultation générale pour fièvre"))
				.andExpect(jsonPath("$.status").value("EN_COURS"));
	}

	@Test
	void givenExistingActiveVisit_whenCreateAnother_thenConflict() throws Exception {
		// Open first visit
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = new VisitEntity(patientA, "VIS-001", "Premier motif", "Tri");
		visitRepository.save(visit);
		com.joprelys.backend.auth.security.TenantContext.clear();

		String jsonRequest = String.format("""
				{
					"patientId": "%s",
					"reason": "Second motif",
					"orientation": "Médecine générale"
				}
				""", patientA.getId());

		mockMvc.perform(post("/api/visits")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Ce patient possède déjà une visite active en cours."));
	}

	@Test
	void givenTwoTenants_whenGetActiveVisits_thenIsolateData() throws Exception {
		// Create visit in Tenant A
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visitA = new VisitEntity(patientA, "VIS-A", "Motif A", "Tri");
		visitRepository.save(visitA);

		// Create visit in Tenant B
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgB.getId());
		VisitEntity visitB = new VisitEntity(patientB, "VIS-B", "Motif B", "Consultation");
		visitRepository.save(visitB);
		com.joprelys.backend.auth.security.TenantContext.clear();

		// Agent A should only see visit A
		mockMvc.perform(get("/api/visits/active")
				.header("Authorization", "Bearer " + tokenAgentA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].visitNumber").value("VIS-A"));

		// Agent B should only see visit B
		mockMvc.perform(get("/api/visits/active")
				.header("Authorization", "Bearer " + tokenAgentB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].visitNumber").value("VIS-B"));
	}

	@Test
	void givenActiveVisitWithVitals_whenGetActiveVisits_thenIncludesVitals() throws Exception {
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = new VisitEntity(patientA, "VIS-A", "Motif A", "Tri");
		visit = visitRepository.save(visit);

		com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity vitals =
				new com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity(
						visit,
						java.math.BigDecimal.valueOf(37.2),
						java.math.BigDecimal.valueOf(70.0),
						175,
						80,
						120,
						80,
						98,
						java.math.BigDecimal.valueOf(0.95),
						16,
						java.math.BigDecimal.valueOf(22.86)
				);
		visit.setVitals(vitals);
		visitRepository.save(visit);
		com.joprelys.backend.auth.security.TenantContext.clear();

		mockMvc.perform(get("/api/visits/active")
				.header("Authorization", "Bearer " + tokenAgentA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].visitNumber").value("VIS-A"))
				.andExpect(jsonPath("$[0].vitals.temperature").value(37.2))
				.andExpect(jsonPath("$[0].vitals.bmi").value(22.86));
	}

	@Test
	void givenActiveVisit_whenDoctorCloses_thenSuccess() throws Exception {
		// Create visit in Tenant A
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = new VisitEntity(patientA, "VIS-A", "Motif A", "Tri");
		visit = visitRepository.save(visit);
		com.joprelys.backend.auth.security.TenantContext.clear();

		// Doctor A should close it successfully
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/close")
				.header("Authorization", "Bearer " + tokenMedecinA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("TERMINEE"))
				.andExpect(jsonPath("$.closedAt").isNotEmpty());

		// Re-attempt to close it should fail
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/close")
				.header("Authorization", "Bearer " + tokenMedecinA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Seule une visite active peut être clôturée."));
	}

	@Test
	void givenActiveVisit_whenAgentACloses_thenForbidden() throws Exception {
		// Create visit in Tenant A
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = new VisitEntity(patientA, "VIS-A", "Motif A", "Tri");
		visit = visitRepository.save(visit);
		com.joprelys.backend.auth.security.TenantContext.clear();

		// Agent A (receptionist) should get 403 Forbidden
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/close")
				.header("Authorization", "Bearer " + tokenAgentA))
				.andExpect(status().isForbidden());
	}

	@Test
	void givenActiveVisit_whenSaveVitals_thenSuccessWithBmi() throws Exception {
		// Create visit in Tenant A
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = new VisitEntity(patientA, "VIS-A", "Motif A", "Tri");
		visit = visitRepository.save(visit);
		com.joprelys.backend.auth.security.TenantContext.clear();

		String jsonRequest = """
				{
					"temperature": 37.5,
					"weight": 70.0,
					"height": 175,
					"pulse": 80,
					"systolic": 120,
					"diastolic": 80,
					"spo2": 98,
					"glycemia": 0.95,
					"respiratoryRate": 16
				}
				""";

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/vitals")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.temperature").value(37.5))
				.andExpect(jsonPath("$.weight").value(70.0))
				.andExpect(jsonPath("$.bmi").value(22.86)); // 70 / 1.75^2 = 22.857... -> 22.86
	}

	@Test
	void givenActiveVisit_whenSaveVitalsWithInvalidBounds_thenBadRequest() throws Exception {
		// Create visit in Tenant A
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = new VisitEntity(patientA, "VIS-A", "Motif A", "Tri");
		visit = visitRepository.save(visit);
		com.joprelys.backend.auth.security.TenantContext.clear();

		String jsonRequest = """
				{
					"temperature": 46.0,
					"weight": 70.0,
					"height": 175
				}
				""";

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/vitals")
				.header("Authorization", "Bearer " + tokenAgentA)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("La température doit être inférieure ou égale à 45°C.")));
	}

	@Test
	void givenVisitWithVitals_whenGetVitals_thenSuccess() throws Exception {
		// Create visit in Tenant A
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = new VisitEntity(patientA, "VIS-A", "Motif A", "Tri");
		visit = visitRepository.save(visit);

		com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity vitals =
				new com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity(
						visit,
						java.math.BigDecimal.valueOf(36.8),
						java.math.BigDecimal.valueOf(65.0),
						170,
						72,
						110,
						70,
						99,
						java.math.BigDecimal.valueOf(0.85),
						14,
						java.math.BigDecimal.valueOf(22.49)
				);
		visit.setVitals(vitals);
		visitRepository.save(visit);
		com.joprelys.backend.auth.security.TenantContext.clear();

		mockMvc.perform(get("/api/visits/" + visit.getId() + "/vitals")
				.header("Authorization", "Bearer " + tokenAgentA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.temperature").value(36.8))
				.andExpect(jsonPath("$.bmi").value(22.49));
	}

	@Test
	void givenActiveVisit_whenDoctorCancels_thenSuccess() throws Exception {
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = new VisitEntity(patientA, "VIS-CANCEL", "Motif annulation", "Tri");
		visit = visitRepository.save(visit);
		com.joprelys.backend.auth.security.TenantContext.clear();

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/cancel")
				.header("Authorization", "Bearer " + tokenMedecinA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ANNULEE"))
				.andExpect(jsonPath("$.closedAt").isNotEmpty());
	}

	@Test
	void givenActiveVisit_whenAgentCancels_thenForbidden() throws Exception {
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = new VisitEntity(patientA, "VIS-CANCEL-AGENT", "Motif", "Tri");
		visit = visitRepository.save(visit);
		com.joprelys.backend.auth.security.TenantContext.clear();

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/cancel")
				.header("Authorization", "Bearer " + tokenAgentA))
				.andExpect(status().isForbidden());
	}

	@Test
	void givenActiveVisit_whenDoctorRequestsQrCode_thenReturnsConsultationUrlPng() throws Exception {
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = visitRepository.save(
				new VisitEntity(patientA, "VIS-QR", "Motif", "Consultation"));
		com.joprelys.backend.auth.security.TenantContext.clear();

		byte[] png = mockMvc.perform(get("/api/visits/" + visit.getId() + "/qrcode")
						.header("Authorization", "Bearer " + tokenMedecinA))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_PNG))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andReturn()
				.getResponse()
				.getContentAsByteArray();

		var image = ImageIO.read(new ByteArrayInputStream(png));
		var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
		String decodedUrl = new MultiFormatReader().decode(bitmap).getText();
		org.assertj.core.api.Assertions.assertThat(decodedUrl)
				.isEqualTo("https://joprelys.com/clinic/consultation/" + visit.getId());
	}

	@Test
	void givenClosedVisit_whenDoctorRequestsQrCode_thenReturnsConflict() throws Exception {
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		VisitEntity visit = new VisitEntity(patientA, "VIS-QR-CLOSED", "Motif", "Consultation");
		visit.setStatus("TERMINEE");
		visit = visitRepository.save(visit);
		com.joprelys.backend.auth.security.TenantContext.clear();

		mockMvc.perform(get("/api/visits/" + visit.getId() + "/qrcode")
						.header("Authorization", "Bearer " + tokenMedecinA))
				.andExpect(status().isConflict());
	}

	@Test
	void givenVisitFromOtherTenant_whenAgentRequestsQrCode_thenReturnsNotFound() throws Exception {
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgB.getId());
		VisitEntity visit = visitRepository.save(
				new VisitEntity(patientB, "VIS-QR-B", "Motif", "Consultation"));
		com.joprelys.backend.auth.security.TenantContext.clear();

		mockMvc.perform(get("/api/visits/" + visit.getId() + "/qrcode")
						.header("Authorization", "Bearer " + tokenAgentA))
				.andExpect(status().isNotFound());
	}

	@Test
	void givenUnauthorizedRole_whenRequestingQrCode_thenReturnsForbidden() throws Exception {
		mockMvc.perform(get("/api/visits/" + UUID.randomUUID() + "/qrcode")
						.header("Authorization", "Bearer " + tokenAdminJoprelys))
				.andExpect(status().isForbidden());
	}

	@Test
	void givenAnonymousUser_whenRequestingQrCode_thenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/visits/" + UUID.randomUUID() + "/qrcode"))
				.andExpect(status().isUnauthorized());
	}
}
