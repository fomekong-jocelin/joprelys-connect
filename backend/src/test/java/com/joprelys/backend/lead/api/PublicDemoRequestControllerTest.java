package com.joprelys.backend.lead.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.joprelys.backend.lead.domain.DemoRequest;
import com.joprelys.backend.lead.domain.DemoRequestStatus;
import com.joprelys.backend.lead.dto.DemoRequestDto;
import com.joprelys.backend.lead.repository.DemoRequestRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PublicDemoRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private DemoRequestRepository repository;

    @Test
    void shouldCreateDemoRequestWithoutAuthentication() throws Exception {
        DemoRequestDto dto = new DemoRequestDto(
                "Dr. Jocelin Fomekong",
                "Clinique Sainte-Anne Douala",
                "Directeur Médical",
                "+237 691893198",
                "jocelin@clinique-ste-anne.cm",
                "Douala",
                "Déploiement DPU et IA vocale aux urgences",
                "landing-page",
                "fr"
        );

        mockMvc.perform(post("/api/public/demo-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fullName", is("Dr. Jocelin Fomekong")))
                .andExpect(jsonPath("$.organizationName", is("Clinique Sainte-Anne Douala")))
                .andExpect(jsonPath("$.status", is("NEW")));

        List<DemoRequest> saved = repository.findAll();
        assertThat(saved).anyMatch(r ->
                r.getFullName().equals("Dr. Jocelin Fomekong")
                        && r.getOrganizationName().equals("Clinique Sainte-Anne Douala")
                        && r.getPhone().equals("+237 691893198")
                        && r.getStatus() == DemoRequestStatus.NEW
        );
    }

    @Test
    void shouldRejectDemoRequestWhenRequiredFieldsAreMissing() throws Exception {
        DemoRequestDto dto = new DemoRequestDto(
                "",
                "",
                "Directeur",
                "",
                "invalid-email",
                "Douala",
                "test",
                "landing-page",
                "fr"
        );

        mockMvc.perform(post("/api/public/demo-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
