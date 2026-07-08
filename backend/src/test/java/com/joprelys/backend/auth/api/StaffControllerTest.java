package com.joprelys.backend.auth.api;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StaffControllerTest {

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
	private OrganizationEntity orgB;
	private UserAccountEntity adminA;
	private UserAccountEntity medecinA;
	private UserAccountEntity infirmierA;
	private UserAccountEntity medecinB;
	private String tokenAdminA;
	private String tokenMedecinA;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM medical_documents");
		jdbcTemplate.update("DELETE FROM prescription_items");
		jdbcTemplate.update("DELETE FROM prescriptions");
		jdbcTemplate.update("DELETE FROM consultations");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		jdbcTemplate.update("DELETE FROM auth_audit_events");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();

		orgA = organizationRepository.save(new OrganizationEntity(
				"Clinique A", "staff-a@joprelys.local", "123", "Street A", "Douala"));
		orgB = organizationRepository.save(new OrganizationEntity(
				"Clinique B", "staff-b@joprelys.local", "456", "Street B", "Yaounde"));

		adminA = saveUser("admin.a@joprelys.local", "Admin A", "ADMIN_CLINIQUE", "Admin@12345", orgA);
		medecinA = saveUser("medecin.a@joprelys.local", "Dr Alpha", "MEDECIN", "Staff@12345", orgA);
		infirmierA = saveUser("infirmier.a@joprelys.local", "Infirmier A", "INFIRMIER", "Staff@12345", orgA);
		medecinB = saveUser("medecin.b@joprelys.local", "Dr Beta", "MEDECIN", "Staff@12345", orgB);

		tokenAdminA = jwtService.createToken(adminA).value();
		tokenMedecinA = jwtService.createToken(medecinA).value();
	}

	@Test
	void givenAdmin_whenListStaff_thenReturnsOnlyClinicStaff() throws Exception {
		mockMvc.perform(get("/api/staff")
						.header("Authorization", "Bearer " + tokenAdminA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].email").value("medecin.a@joprelys.local"))
				.andExpect(jsonPath("$[1].email").value("infirmier.a@joprelys.local"));
	}

	@Test
	void givenAdmin_whenInviteNewStaff_thenReturnsTemporaryPasswordAndStoresHash() throws Exception {
		String request = """
				{
					"email": "Nouveau.Medecin@JOPRELYS.local",
					"displayName": "Dr Nouveau",
					"role": "medecin"
				}
				""";

		mockMvc.perform(post("/api/staff")
						.header("Authorization", "Bearer " + tokenAdminA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("nouveau.medecin@joprelys.local"))
				.andExpect(jsonPath("$.role").value("MEDECIN"))
				.andExpect(jsonPath("$.enabled").value(true))
				.andExpect(jsonPath("$.temporaryPassword").value(matchesPattern("Jop-[A-Z2-9]{6}")));

		UserAccountEntity created = userAccountRepository.findByEmail("nouveau.medecin@joprelys.local").orElseThrow();
		org.assertj.core.api.Assertions.assertThat(created.getOrganizationId()).isEqualTo(orgA.getId());
		org.assertj.core.api.Assertions.assertThat(created.getPasswordHash()).doesNotStartWith("Jop-");
	}

	@Test
	void givenAdmin_whenInviteExistingEmail_thenReturnsBadRequest() throws Exception {
		String request = """
				{
					"email": "medecin.a@joprelys.local",
					"displayName": "Duplicate",
					"role": "MEDECIN"
				}
				""";

		mockMvc.perform(post("/api/staff")
						.header("Authorization", "Bearer " + tokenAdminA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Un utilisateur avec cet email existe déjà."));
	}

	@Test
	void givenMedecin_whenListStaff_thenReturnsSuccess() throws Exception {
		mockMvc.perform(get("/api/staff")
						.header("Authorization", "Bearer " + tokenMedecinA))
				.andExpect(status().isOk());
	}

	@Test
	void givenAdminA_whenModifyStaffOfClinicB_thenReturnsNotFound() throws Exception {
		String request = """
				{
					"displayName": "Dr Intrus",
					"role": "MEDECIN"
				}
				""";

		mockMvc.perform(put("/api/staff/" + medecinB.getId())
						.header("Authorization", "Bearer " + tokenAdminA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isNotFound());
	}

	@Test
	void givenAdmin_whenUpdateStaff_thenNameAndRoleAreChanged() throws Exception {
		String request = """
				{
					"displayName": "Dr Alpha Senior",
					"role": "PHARMACIEN"
				}
				""";

		mockMvc.perform(put("/api/staff/" + medecinA.getId())
						.header("Authorization", "Bearer " + tokenAdminA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Dr Alpha Senior"))
				.andExpect(jsonPath("$.role").value("PHARMACIEN"));
	}

	@Test
	void givenAdmin_whenToggleStaffStatus_thenLoginIsBlocked() throws Exception {
		mockMvc.perform(post("/api/staff/" + infirmierA.getId() + "/toggle")
						.header("Authorization", "Bearer " + tokenAdminA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(false));

		String loginRequest = """
				{
					"email": "infirmier.a@joprelys.local",
					"password": "Staff@12345"
				}
				""";

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginRequest))
				.andExpect(status().isUnauthorized());
	}

	private UserAccountEntity saveUser(
			String email,
			String displayName,
			String role,
			String password,
			OrganizationEntity organization) {
		UserAccountEntity user = new UserAccountEntity(
				email,
				displayName,
				role,
				passwordEncoder.encode(password));
		user.setOrganizationId(organization.getId());
		return userAccountRepository.save(user);
	}
}
