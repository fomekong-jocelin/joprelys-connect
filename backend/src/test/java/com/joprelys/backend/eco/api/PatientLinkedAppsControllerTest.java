package com.joprelys.backend.eco.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.eco.domain.ConsentScope;
import com.joprelys.backend.eco.domain.EcoStatus;
import com.joprelys.backend.eco.domain.GranteeType;
import com.joprelys.backend.eco.domain.LinkRelationship;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentRepository;
import com.joprelys.backend.eco.infrastructure.persistence.EcoOutboxRepository;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * S7 — portail patient « Applications reliées » : liste et révocation, contrôle d'accès par ressource.
 * Données fictives uniquement ({@code DPU-TEST-…}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientLinkedAppsControllerTest {

    private static final String PATH = "/api/patient/linked-apps";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PatientRepository patientRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private EcoPatientLinkRepository linkRepository;
    @Autowired private EcoConsentRepository consentRepository;
    @Autowired private EcoOutboxRepository outboxRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private PatientEntity owner;
    private EcoPatientLinkEntity ownerLink;
    private EcoPatientLinkEntity otherLink;
    private String ownerToken;

    @BeforeEach
    void createFictitiousPatients() {
        owner = savePatient();
        PatientEntity other = savePatient();
        ownerLink = saveLink(owner, Set.of(ConsentScope.LAB_RESULTS, ConsentScope.PRESCRIPTIONS));
        otherLink = saveLink(other, Set.of(ConsentScope.PRESCRIPTIONS));
        ownerToken = "Bearer " + jwtService.createPatientToken(owner).value();
    }

    /** La base H2 est partagée : sans ce nettoyage, d'autres tests ne pourraient plus supprimer leurs patients. */
    @AfterEach
    void removeEcoRows() {
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM eco_outbox");
        jdbcTemplate.update("DELETE FROM eco_consents");
        jdbcTemplate.update("DELETE FROM eco_patient_links");
    }

    @Test
    void listsOnlyTheActiveLinksOfTheSignedInPatient() throws Exception {
        mockMvc.perform(get(PATH).header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(ownerLink.getId().toString()))
                .andExpect(jsonPath("$[0].application").value("allopharma"))
                .andExpect(jsonPath("$[0].scopes[0]").value("prescriptions"))
                .andExpect(jsonPath("$[0].scopes[1]").value("lab-results"))
                .andExpect(jsonPath("$[0].linkedAt").exists())
                .andExpect(jsonPath("$[0].validUntil").exists())
                .andExpect(jsonPath("$[0].dpu").doesNotExist());
    }

    @Test
    void revokingOwnLinkRemovesItAndNotifiesTheApplication() throws Exception {
        mockMvc.perform(delete(PATH + "/" + ownerLink.getId()).header("Authorization", ownerToken))
                .andExpect(status().isNoContent());

        assertThat(linkRepository.findById(ownerLink.getId()).orElseThrow().getStatus())
                .isEqualTo(EcoStatus.REVOKED);
        assertThat(outboxRepository.findAll())
                .anySatisfy(entry -> assertThat(entry.getPayload()).contains(ownerLink.getId().toString()));
        mockMvc.perform(get(PATH).header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void revokingTwiceIsHarmless() throws Exception {
        mockMvc.perform(delete(PATH + "/" + ownerLink.getId()).header("Authorization", ownerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(PATH + "/" + ownerLink.getId()).header("Authorization", ownerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void anotherPatientsLinkIsNotFoundAndStaysActive() throws Exception {
        mockMvc.perform(delete(PATH + "/" + otherLink.getId()).header("Authorization", ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PATIENT_LINK_NOT_FOUND"));

        assertThat(linkRepository.findById(otherLink.getId()).orElseThrow().getStatus())
                .isEqualTo(EcoStatus.ACTIVE);
    }

    @Test
    void anonymousCallsAreRejected() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(PATH + "/" + ownerLink.getId())).andExpect(status().isUnauthorized());
    }

    private PatientEntity savePatient() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        OrganizationEntity organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique Demo " + suffix, "demo-" + suffix.toLowerCase() + "@test.local", "600000000", "Littoral",
                "Douala"));
        TenantContext.setTenantId(organization.getId());
        try {
            return patientRepository.saveAndFlush(new PatientEntity("DPU-TEST-" + suffix, "PAT-" + suffix,
                    "Patient Fictif", "FEMININ", LocalDate.of(1990, 1, 1), null, "Douala", null, null, null, null,
                    null, null));
        } finally {
            TenantContext.clear();
        }
    }

    private EcoPatientLinkEntity saveLink(PatientEntity patient, Set<ConsentScope> scopes) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        EcoPatientLinkEntity link = linkRepository.saveAndFlush(new EcoPatientLinkEntity(patient.getId(),
                patient.getGlobalPatientNumber(), "joprelys:clinique-demo", "allopharma", UUID.randomUUID(),
                LinkRelationship.SELF, "sub-" + patient.getGlobalPatientNumber(), "allopharma-mobile", now));
        consentRepository.saveAndFlush(new EcoConsentEntity(link.getId(), scopes, GranteeType.APPLICATION,
                "allopharma", now, now.plus(Duration.ofDays(365))));
        return link;
    }
}
