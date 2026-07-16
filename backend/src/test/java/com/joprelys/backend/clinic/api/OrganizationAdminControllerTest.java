package com.joprelys.backend.clinic.api;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.notification.application.AccountMailService;
import com.joprelys.backend.notification.application.MailDeliveryUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrganizationAdminControllerTest {

	@MockitoBean
	private AccountMailService accountMailService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private OrganizationEntity orgA;
	private UserAccountEntity superAdmin;
	private String tokenSuperAdmin;
	private String tokenOther;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM medical_documents");
		jdbcTemplate.update("DELETE FROM prescription_items");
		jdbcTemplate.update("DELETE FROM prescriptions");
		jdbcTemplate.update("DELETE FROM consultations");
		jdbcTemplate.update("DELETE FROM vitals");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		jdbcTemplate.update("DELETE FROM users WHERE email LIKE '%@orgadmintest.local'");
		jdbcTemplate.update("DELETE FROM organizations WHERE email LIKE '%@orgadmintest.local'");

		orgA = organizationRepository.save(new OrganizationEntity(
				"Clinique Test Admin", "contact@orgadmintest.local", "+237600000001", "Rue A", "Douala"));
		orgA.setStatus("ACTIVE");
		orgA = organizationRepository.save(orgA);

		superAdmin = new UserAccountEntity(
				"superadmin@orgadmintest.local", "Super Admin", "ADMIN_JOPRELYS",
				passwordEncoder.encode("Test1234!"));
		userAccountRepository.save(superAdmin);

		UserAccountEntity otherUser = new UserAccountEntity(
				"other@orgadmintest.local", "Other User", "ADMIN_CLINIQUE",
				passwordEncoder.encode("Test1234!"));
		otherUser.setOrganizationId(orgA.getId());
		userAccountRepository.save(otherUser);

		tokenSuperAdmin = "Bearer " + jwtService.createToken(superAdmin).value();
		tokenOther = "Bearer " + jwtService.createToken(otherUser).value();
	}

	// --- Cas nominaux ---

	@Test
	void createClinicAdmin_shouldReturn201WithTemporaryPassword() throws Exception {
		mockMvc.perform(post("/api/organizations/" + orgA.getId() + "/admin")
						.header("Authorization", tokenSuperAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "Dr Admin Clinique",
								  "email": "admin.clinique@orgadmintest.local"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email", is("admin.clinique@orgadmintest.local")))
				.andExpect(jsonPath("$.role", is("ADMIN_CLINIQUE")))
				.andExpect(jsonPath("$.enabled", is(true)))
				.andExpect(jsonPath("$.temporaryPassword").doesNotExist())
				.andExpect(jsonPath("$.organizationId", is(orgA.getId().toString())));
	}

	@Test
	void createClinicAdmin_shouldRollbackAndReturn503WhenEmailDeliveryFails() throws Exception {
		doThrow(new MailDeliveryUnavailableException(new MailSendException("SMTP unavailable")))
				.when(accountMailService).sendTemporaryPassword(anyString(), anyString(), anyString());

		String email = "mail-failure@orgadmintest.local";
		mockMvc.perform(post("/api/organizations/" + orgA.getId() + "/admin")
						.header("Authorization", tokenSuperAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "Admin Mail Failure",
								  "email": "%s"
								}
								""".formatted(email)))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.error.code", is("MAIL_DELIVERY_UNAVAILABLE")));

		org.assertj.core.api.Assertions.assertThat(userAccountRepository.existsByEmail(email)).isFalse();
	}

	// --- Sécurité / RBAC ---

	@Test
	void createClinicAdmin_shouldReturn403WhenNotSuperAdmin() throws Exception {
		mockMvc.perform(post("/api/organizations/" + orgA.getId() + "/admin")
						.header("Authorization", tokenOther)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "Pirate Admin",
								  "email": "pirate@orgadmintest.local"
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void createClinicAdmin_shouldReturn401WhenNoToken() throws Exception {
		mockMvc.perform(post("/api/organizations/" + orgA.getId() + "/admin")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "No Token",
								  "email": "notoken@orgadmintest.local"
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	// --- Validation métier ---

	@Test
	void createClinicAdmin_shouldReturn404WhenOrgNotFound() throws Exception {
		mockMvc.perform(post("/api/organizations/" + UUID.randomUUID() + "/admin")
						.header("Authorization", tokenSuperAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "Admin Fantôme",
								  "email": "fantome@orgadmintest.local"
								}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void createClinicAdmin_shouldReturn409WhenEmailAlreadyExists() throws Exception {
		// Premier appel
		mockMvc.perform(post("/api/organizations/" + orgA.getId() + "/admin")
						.header("Authorization", tokenSuperAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "Admin Un",
								  "email": "admindup@orgadmintest.local"
								}
								"""))
				.andExpect(status().isCreated());

		// Deuxième appel avec le même email
		mockMvc.perform(post("/api/organizations/" + orgA.getId() + "/admin")
						.header("Authorization", tokenSuperAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "Admin Deux",
								  "email": "admindup@orgadmintest.local"
								}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void createClinicAdmin_shouldReturn400WhenEmailInvalid() throws Exception {
		mockMvc.perform(post("/api/organizations/" + orgA.getId() + "/admin")
						.header("Authorization", tokenSuperAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "Admin Invalide",
								  "email": "pas-un-email"
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createClinicAdmin_shouldReturn400WhenDisplayNameBlank() throws Exception {
		mockMvc.perform(post("/api/organizations/" + orgA.getId() + "/admin")
						.header("Authorization", tokenSuperAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "displayName": "",
								  "email": "adminblank@orgadmintest.local"
								}
								"""))
				.andExpect(status().isBadRequest());
	}
}
