package com.joprelys.backend.clinic.api;

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

	@BeforeEach
	void cleanUp() {
		organizationRepository.deleteAll();
	}

	@Test
	@WithMockUser(roles = "ADMIN_JOPRELYS")
	void givenAdmin_whenCreateOrganization_thenSuccess() throws Exception {
		String jsonRequest = """
				{
					"name": "Clinique Test",
					"email": "test@joprelys.local",
					"phone": "+237 123",
					"address": "Street Test",
					"city": "Yaoundé"
				}
				""";

		mockMvc.perform(post("/api/organizations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Clinique Test"))
				.andExpect(jsonPath("$.email").value("test@joprelys.local"))
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
	@WithMockUser(roles = "ADMIN_JOPRELYS")
	void givenAdmin_whenListOrganizations_thenReturnsList() throws Exception {
		OrganizationEntity org1 = new OrganizationEntity("Espoir", "espoir@joprelys.local", "123", "street1", "Douala");
		OrganizationEntity org2 = new OrganizationEntity("Paix", "paix@joprelys.local", "456", "street2", "Yaoundé");
		organizationRepository.save(org1);
		organizationRepository.save(org2);

		mockMvc.perform(get("/api/organizations"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Espoir"))
				.andExpect(jsonPath("$[1].name").value("Paix"));
	}

	@Test
	@WithMockUser(roles = "ADMIN_JOPRELYS")
	void givenAdmin_whenUpdateStatus_thenStatusChanges() throws Exception {
		OrganizationEntity org = new OrganizationEntity("Espoir", "espoir@joprelys.local", "123", "street1", "Douala");
		var saved = organizationRepository.save(org);

		mockMvc.perform(put("/api/organizations/" + saved.getId() + "/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("\"INACTIVE\""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"));
	}
}
