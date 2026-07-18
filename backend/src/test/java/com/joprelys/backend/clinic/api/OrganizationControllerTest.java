package com.joprelys.backend.clinic.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrganizationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUp() {
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM medical_documents");
		jdbcTemplate.update("DELETE FROM prescription_items");
		jdbcTemplate.update("DELETE FROM prescriptions");
		jdbcTemplate.update("DELETE FROM consultations");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM organization_api_keys");
		organizationRepository.deleteAll();
	}

	@Test
	@WithMockUser(authorities = "ORGANIZATION_MANAGE")
	void givenAdmin_whenCreateOrganization_thenSuccess() throws Exception {
		String jsonRequest = """
				{
					"name": "Clinique Test",
					"email": "test@joprelys.local",
					"phone": "+237 123",
					"address": "Street Test",
					"city": "Yaoundé",
					"country": "Cameroun",
					"type": "CLINIC",
					"responsibleName": "Jean R",
					"apiEnabled": true,
					"logoPath": "uploads/logo/clinic-test.png"
				}
				""";

		mockMvc.perform(post("/api/organizations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Clinique Test"))
				.andExpect(jsonPath("$.email").value("test@joprelys.local"))
				.andExpect(jsonPath("$.country").value("Cameroun"))
				.andExpect(jsonPath("$.type").value("CLINIC"))
				.andExpect(jsonPath("$.responsibleName").value("Jean R"))
				.andExpect(jsonPath("$.apiEnabled").value(true))
				.andExpect(jsonPath("$.logoPath").value("uploads/logo/clinic-test.png"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	@WithMockUser(roles = "MEDECIN")
	void givenDoctor_whenCreateOrganization_thenForbidden() throws Exception {
		String jsonRequest = """
				{
					"name": "Clinique Forbidden",
					"email": "forbidden@joprelys.local",
					"city": "Douala"
				}
				""";

		mockMvc.perform(post("/api/organizations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(authorities = "ORGANIZATION_MANAGE")
	void givenAdmin_whenListOrganizations_thenSuccess() throws Exception {
		OrganizationEntity org1 = new OrganizationEntity("Espoir", "espoir@joprelys.local", "123", "street1", "Douala");
		OrganizationEntity org2 = new OrganizationEntity("Paix", "paix@joprelys.local", "456", "street2", "Yaoundé");
		organizationRepository.save(org1);
		organizationRepository.save(org2);

		mockMvc.perform(get("/api/organizations"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	@WithMockUser(authorities = "ORGANIZATION_MANAGE")
	void givenAdmin_whenUpdateStatus_thenSuccess() throws Exception {
		OrganizationEntity org = new OrganizationEntity("Espoir", "espoir@joprelys.local", "123", "street1", "Douala");
		var saved = organizationRepository.save(org);

		mockMvc.perform(put("/api/organizations/" + saved.getId() + "/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("\"INACTIVE\""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"));
	}

	@Test
	@WithMockUser(authorities = "ORGANIZATION_MANAGE")
	void givenAdmin_whenUpdateOrganization_thenSuccess() throws Exception {
		OrganizationEntity org = new OrganizationEntity("Espoir", "espoir@joprelys.local", "123", "street1", "Douala");
		var saved = organizationRepository.save(org);

		String jsonRequest = """
				{
					"name": "Nouveau Nom Clinique",
					"email": "nouveau.email@joprelys.local",
					"phone": "+237 999",
					"address": "Nouvelle Adresse",
					"city": "Yaoundé",
					"country": "France",
					"type": "HOSPITAL",
					"responsibleName": "Pierre M",
					"apiEnabled": false
				}
				""";

		mockMvc.perform(put("/api/organizations/" + saved.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Nouveau Nom Clinique"))
				.andExpect(jsonPath("$.email").value("nouveau.email@joprelys.local"))
				.andExpect(jsonPath("$.phone").value("+237 999"))
				.andExpect(jsonPath("$.address").value("Nouvelle Adresse"))
				.andExpect(jsonPath("$.city").value("Yaoundé"))
				.andExpect(jsonPath("$.country").value("France"))
				.andExpect(jsonPath("$.type").value("HOSPITAL"))
				.andExpect(jsonPath("$.responsibleName").value("Pierre M"))
				.andExpect(jsonPath("$.apiEnabled").value(false));
	}

	@Test
	@WithMockUser(roles = "MEDECIN")
	void givenDoctor_whenUpdateOrganization_thenForbidden() throws Exception {
		OrganizationEntity org = new OrganizationEntity("Espoir", "espoir@joprelys.local", "123", "street1", "Douala");
		var saved = organizationRepository.save(org);

		String jsonRequest = """
				{
					"name": "Forbidden Update",
					"email": "espoir@joprelys.local",
					"city": "Douala"
				}
				""";

		mockMvc.perform(put("/api/organizations/" + saved.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(authorities = "ORGANIZATION_MANAGE")
	void givenAdmin_whenManageApiKeys_thenSuccess() throws Exception {
		OrganizationEntity org = new OrganizationEntity("Clinique Clés", "contact@cles.local", "1234", "Rue", "Yaoundé");
		var saved = organizationRepository.save(org);

		// 1. Generate API Key
		String genResponse = mockMvc.perform(post("/api/organizations/" + saved.getId() + "/api-keys")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Clé Test\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Clé Test"))
				.andExpect(jsonPath("$.rawKey").exists())
				.andReturn().getResponse().getContentAsString();

		String keyId = com.jayway.jsonpath.JsonPath.read(genResponse, "$.id");

		// 2. List API Keys
		mockMvc.perform(get("/api/organizations/" + saved.getId() + "/api-keys"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(keyId))
				.andExpect(jsonPath("$[0].name").value("Clé Test"))
				.andExpect(jsonPath("$[0].rawKey").isEmpty());

		// 3. Revoke API Key
		mockMvc.perform(delete("/api/organizations/" + saved.getId() + "/api-keys/" + keyId))
				.andExpect(status().isNoContent());

		// 4. Verify listing shows revoked
		mockMvc.perform(get("/api/organizations/" + saved.getId() + "/api-keys"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("REVOKED"));
	}

	@Test
	@WithMockUser(authorities = "ORGANIZATION_MANAGE")
	void givenApiKey_whenAuthenticate_thenFilterAppliesCorrectly() throws Exception {
		OrganizationEntity org = new OrganizationEntity("Clinique Sécurité", "contact@sec.local", "12345", "Rue", "Yaoundé");
		var saved = organizationRepository.save(org);

		String genResponse = mockMvc.perform(post("/api/organizations/" + saved.getId() + "/api-keys")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"Clé Sec\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String rawKey = com.jayway.jsonpath.JsonPath.read(genResponse, "$.rawKey");

		// 1. Test filter authentication with valid key -> Should return 403 instead of 401 (meaning authed but forbidden by method security)
		mockMvc.perform(get("/api/organizations")
				.header("X-API-KEY", rawKey))
				.andExpect(status().isForbidden());

		// 2. Test filter authentication with invalid key -> 401
		mockMvc.perform(get("/api/organizations")
				.header("X-API-KEY", "jop_live_invalidkey"))
				.andExpect(status().isUnauthorized());

		// 3. Test filter authentication with suspended organization -> 403
		saved.setStatus("INACTIVE");
		organizationRepository.save(saved);

		mockMvc.perform(get("/api/organizations")
				.header("X-API-KEY", rawKey))
				.andExpect(status().isForbidden());
	}
}
