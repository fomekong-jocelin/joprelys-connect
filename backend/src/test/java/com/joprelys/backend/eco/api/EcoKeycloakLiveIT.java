package com.joprelys.backend.eco.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Intégration réelle avec Joprelys ID : jetons émis par un Keycloak local, clés téléchargées par le vrai décodeur.
 * Désactivé par défaut ; lancé par {@code node infra/keycloak/run-live-eco-test.mjs}, qui obtient les jetons
 * (PKCE avec consentement pour le patient fictif, client credentials pour le serveur) sans les afficher.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "JOPRELYS_ID_LIVE_PATIENT_TOKEN", matches = ".+")
@TestPropertySource(properties = {
        "joprelys.eco.identity.issuer-uri=http://localhost:8180/realms/joprelys-id",
        "joprelys.eco.identity.jwk-set-uri=http://localhost:8180/realms/joprelys-id/protocol/openid-connect/certs"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EcoKeycloakLiveIT {

    /** DPU du compte fictif patient.demo1 dans le realm de dev. */
    private static final String DEMO_DPU = "DPU-TEST-000123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void realKeycloakTokensLinkThenRevoke() throws Exception {
        OrganizationEntity organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique Demo Live", "live-" + UUID.randomUUID() + "@test.local", "600000000", "Littoral", "Douala"));
        TenantContext.setTenantId(organization.getId());
        try {
            patientRepository.saveAndFlush(new PatientEntity(DEMO_DPU, "PAT-LIVE-000123", "Patient Fictif", "FEMININ",
                    LocalDate.of(1990, 1, 1), null, "Douala", null, null, null, null, null, null));
        } finally {
            TenantContext.clear();
        }

        String created = mockMvc.perform(post("/api/eco/v1/patient-links")
                        .header("Authorization", "Bearer " + System.getenv("JOPRELYS_ID_LIVE_PATIENT_TOKEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allopharmaProfileId\":\"" + UUID.randomUUID()
                                + "\",\"relationship\":\"self\",\"scope\":[\"prescriptions\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientLink.dpu").value(DEMO_DPU))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/eco/v1/consents/" + JsonPath.read(created, "$.consent.id") + "/revoke")
                        .header("Authorization", "Bearer " + System.getenv("JOPRELYS_ID_LIVE_SERVER_TOKEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"revokedBy\":\"patient\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consent.status").value("revoked"));

        // Le jeton serveur n'a pas le scope de liaison ; le jeton patient n'a pas celui de révocation.
        mockMvc.perform(post("/api/eco/v1/patient-links")
                        .header("Authorization", "Bearer " + System.getenv("JOPRELYS_ID_LIVE_SERVER_TOKEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allopharmaProfileId\":\"" + UUID.randomUUID()
                                + "\",\"relationship\":\"self\",\"scope\":[\"prescriptions\"]}"))
                .andExpect(status().isForbidden());
    }
}
