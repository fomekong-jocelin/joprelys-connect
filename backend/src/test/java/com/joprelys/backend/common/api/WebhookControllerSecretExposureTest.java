package com.joprelys.backend.common.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookControllerSecretExposureTest {

    private static final String SECRET = "whsec_fictif_0123456789";

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String adminToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM webhooks");
        OrganizationEntity organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Webhook", "webhook-" + System.nanoTime() + "@joprelys.local", "123", "Rue W", "Douala"));
        UserAccountEntity admin = new UserAccountEntity(
                "admin.webhook." + System.nanoTime() + "@joprelys.local", "Admin Webhook", "ADMIN_CLINIQUE", "hash");
        admin.setOrganizationId(organization.getId());
        adminToken = jwtService.createToken(userAccountRepository.save(admin)).value();
    }

    @Test
    void secretIsReturnedOnlyInTheCreationResponse() throws Exception {
        String created = mockMvc.perform(post("/api/webhooks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "https://partner.example.test/hook", "secret": "%s", "eventTypes": "VISIT_CLOSED"}
                                """.formatted(SECRET)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secret").value(SECRET))
                .andReturn().getResponse().getContentAsString();
        String webhookId = new ObjectMapper().readTree(created).get("id").asText();

        mockMvc.perform(get("/api/webhooks").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(webhookId))
                .andExpect(jsonPath("$[0].url").value("https://partner.example.test/hook"))
                .andExpect(jsonPath("$[0].secret").value(WebhookResponse.MASKED_SECRET));

        mockMvc.perform(put("/api/webhooks/" + webhookId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventTypes\": \"VISIT_CLOSED,LAB_RESULT_READY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTypes").value("VISIT_CLOSED,LAB_RESULT_READY"))
                .andExpect(jsonPath("$.secret").value(WebhookResponse.MASKED_SECRET));
    }
}
