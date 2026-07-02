package com.joprelys.backend.clinic.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClinicControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void givenUnauthenticated_whenAccessClinic_thenUnauthorized() throws Exception {
		mockMvc.perform(get("/api/clinic/admin"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/clinic/medecin"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/clinic/pharmacien"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "ADMIN_JOPRELYS")
	public void givenAdminUser_whenAccessClinic_thenAllAllowed() throws Exception {
		mockMvc.perform(get("/api/clinic/admin"))
				.andExpect(status().isOk())
				.andExpect(content().string("Données administratives confidentielles"));

		mockMvc.perform(get("/api/clinic/medecin"))
				.andExpect(status().isOk())
				.andExpect(content().string("Dossiers cliniques des patients"));

		mockMvc.perform(get("/api/clinic/pharmacien"))
				.andExpect(status().isOk())
				.andExpect(content().string("Ordonnances et délivrances en attente"));
	}

	@Test
	@WithMockUser(roles = "MEDECIN")
	public void givenMedecinUser_whenAccessClinic_thenOnlyMedecinAllowed() throws Exception {
		mockMvc.perform(get("/api/clinic/admin"))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/clinic/medecin"))
				.andExpect(status().isOk())
				.andExpect(content().string("Dossiers cliniques des patients"));

		mockMvc.perform(get("/api/clinic/pharmacien"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "PHARMACIEN")
	public void givenPharmacienUser_whenAccessClinic_thenOnlyPharmacienAllowed() throws Exception {
		mockMvc.perform(get("/api/clinic/admin"))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/clinic/medecin"))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/clinic/pharmacien"))
				.andExpect(status().isOk())
				.andExpect(content().string("Ordonnances et délivrances en attente"));
	}
}
