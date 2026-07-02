package com.joprelys.backend.audit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.audit.infrastructure.persistence.AuditLogEntity;
import com.joprelys.backend.audit.infrastructure.persistence.AuditLogRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuditControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtService jwtService;

	private OrganizationEntity orgA;
	private OrganizationEntity orgB;

	private UserAccountEntity userAuditeur;
	private UserAccountEntity userAdminA;
	private UserAccountEntity userMedecinA;
	private UserAccountEntity userAdminB;

	private String tokenAuditeur;
	private String tokenAdminA;
	private String tokenMedecinA;
	private String tokenAdminB;

	private PatientEntity patientA;
	private PatientEntity patientB;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM medical_documents");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();

		// Cliniques
		orgA = new OrganizationEntity("Clinique A", "contact@a.local", "111", "Rue A", "Douala");
		orgA = organizationRepository.save(orgA);

		orgB = new OrganizationEntity("Clinique B", "contact@b.local", "222", "Rue B", "Yaounde");
		orgB = organizationRepository.save(orgB);

		// Utilisateurs
		userAuditeur = new UserAccountEntity("auditeur@test.local", "Auditeur Global", "AUDITEUR", "hash");
		userAuditeur = userAccountRepository.save(userAuditeur);

		userAdminA = new UserAccountEntity("admin.a@test.local", "Admin Clinique A", "ADMIN_CLINIQUE", "hash");
		userAdminA.setOrganizationId(orgA.getId());
		userAdminA = userAccountRepository.save(userAdminA);

		userMedecinA = new UserAccountEntity("medecin.a@test.local", "Dr Medecin A", "MEDECIN", "hash");
		userMedecinA.setOrganizationId(orgA.getId());
		userMedecinA = userAccountRepository.save(userMedecinA);

		userAdminB = new UserAccountEntity("admin.b@test.local", "Admin Clinique B", "ADMIN_CLINIQUE", "hash");
		userAdminB.setOrganizationId(orgB.getId());
		userAdminB = userAccountRepository.save(userAdminB);

		// Tokens
		tokenAuditeur = jwtService.createToken(userAuditeur).value();
		tokenAdminA = jwtService.createToken(userAdminA).value();
		tokenMedecinA = jwtService.createToken(userMedecinA).value();
		tokenAdminB = jwtService.createToken(userAdminB).value();

		// Patients
		TenantContext.setTenantId(orgA.getId());
		patientA = new PatientEntity(
				"P-GLOBAL-A", "P-LOCAL-A", "Jean Patient A", "M",
				LocalDate.of(1990, 1, 1), "677777777", "Douala",
				"District A", "Rue Patient A", "Contact Name", "688888888",
				"Aucune", "Aucun"
		);
		patientA = patientRepository.save(patientA);

		TenantContext.setTenantId(orgB.getId());
		patientB = new PatientEntity(
				"P-GLOBAL-B", "P-LOCAL-B", "Jeanne Patient B", "F",
				LocalDate.of(1995, 5, 5), "677777778", "Yaounde",
				"District B", "Rue Patient B", "Contact Name B", "688888889",
				"Allergie Penicilline", "Diabete"
		);
		patientB = patientRepository.save(patientB);

		TenantContext.setTenantId(orgA.getId());

		// Logs d'audit initiaux
		AuditLogEntity log1 = new AuditLogEntity(
				UUID.randomUUID(), userMedecinA.getId(), orgA.getId(), patientA.getId(),
				"PATIENT_RECORD", patientA.getId(), "CONSULTATION", "Accès initial",
				"127.0.0.1", "Mozilla/5.0", "SUCCESS", Instant.now().minusSeconds(3600)
		);
		auditLogRepository.save(log1);

		AuditLogEntity log2 = new AuditLogEntity(
				UUID.randomUUID(), userAdminB.getId(), orgB.getId(), patientB.getId(),
				"PATIENT_RECORD", patientB.getId(), "CONSULTATION", "Accès clinique B",
				"127.0.0.1", "Mozilla/5.0", "SUCCESS", Instant.now()
		);
		auditLogRepository.save(log2);
	}

	@Test
	void givenAdminA_whenGetPatientALogs_thenSuccess() throws Exception {
		mockMvc.perform(get("/api/audit/patients/" + patientA.getId())
						.header("Authorization", "Bearer " + tokenAdminA)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].action").value("READ_AUDIT"))
				.andExpect(jsonPath("$[1].action").value("CONSULTATION"))
				.andExpect(jsonPath("$[1].actorUserId").value(userMedecinA.getId().toString()))
				.andExpect(jsonPath("$[1].patientId").value(patientA.getId().toString()));
	}

	@Test
	void givenAdminA_whenGetPatientBLogs_thenForbidden() throws Exception {
		mockMvc.perform(get("/api/audit/patients/" + patientB.getId())
						.header("Authorization", "Bearer " + tokenAdminA)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	void givenAuditeur_whenGetPatientBLogs_thenSuccess() throws Exception {
		mockMvc.perform(get("/api/audit/patients/" + patientB.getId())
						.header("Authorization", "Bearer " + tokenAuditeur)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].action").value("READ_AUDIT"))
				.andExpect(jsonPath("$[1].patientId").value(patientB.getId().toString()))
				.andExpect(jsonPath("$[1].actorOrganizationId").value(orgB.getId().toString()));
	}

	@Test
	void givenAdminA_whenGetOrgALogs_thenSuccess() throws Exception {
		mockMvc.perform(get("/api/audit/organizations/" + orgA.getId())
						.header("Authorization", "Bearer " + tokenAdminA)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].actorOrganizationId").value(orgA.getId().toString()));
	}

	@Test
	void givenAdminA_whenGetOrgBLogs_thenForbidden() throws Exception {
		mockMvc.perform(get("/api/audit/organizations/" + orgB.getId())
						.header("Authorization", "Bearer " + tokenAdminA)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	void givenAuditeur_whenGetOrgBLogs_thenSuccess() throws Exception {
		mockMvc.perform(get("/api/audit/organizations/" + orgB.getId())
						.header("Authorization", "Bearer " + tokenAuditeur)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	void givenUnauthenticated_whenGetLogs_thenUnauthorized() throws Exception {
		mockMvc.perform(get("/api/audit/patients/" + patientA.getId())
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}
}
