package com.joprelys.backend.consultation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ConsultationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Autowired private JwtService jwtService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserAccountEntity userMedecinA;
    private UserAccountEntity userMedecinB;
    private UserAccountEntity userAgentA;
    private String tokenMedecinA;
    private String tokenMedecinB;
    private String tokenAgentA;
    private PatientEntity patientA;
    private VisitEntity visitA;
    private VisitEntity visitB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = organizationRepository.save(new OrganizationEntity(
                "Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala"));
        orgB = organizationRepository.save(new OrganizationEntity(
                "Clinique B", "contactb@joprelys.local", "456", "Street B", "Yaoundé"));

        userMedecinA = new UserAccountEntity("medecin.a@joprelys.local", "Dr. Alpha", "MEDECIN", "passhash");
        userMedecinA.setOrganizationId(orgA.getId());
        userMedecinA = userAccountRepository.save(userMedecinA);

        userMedecinB = new UserAccountEntity("medecin.b@joprelys.local", "Dr. Beta", "MEDECIN", "passhash");
        userMedecinB.setOrganizationId(orgB.getId());
        userMedecinB = userAccountRepository.save(userMedecinB);

        userAgentA = new UserAccountEntity("agent.a@joprelys.local", "Agent A", "AGENT_ACCUEIL", "passhash");
        userAgentA.setOrganizationId(orgA.getId());
        userAgentA = userAccountRepository.save(userAgentA);

        tokenMedecinA = jwtService.createToken(userMedecinA).value();
        tokenMedecinB = jwtService.createToken(userMedecinB).value();
        tokenAgentA = jwtService.createToken(userAgentA).value();

        TenantContext.setTenantId(orgA.getId());
        patientA = new PatientEntity("DPU-A", "PAT-A", "Patient Alpha", "MASCULIN",
                LocalDate.of(1985, 3, 15), "+237690000000", "Douala", "", "", "", "", "", "");
        patientA = patientRepository.save(patientA);
        visitA = visitRepository.save(new VisitEntity(patientA, "VIS-A001", "Fièvre persistante", "Médecine générale"));
        TenantContext.clear();

        TenantContext.setTenantId(orgB.getId());
        PatientEntity patientB = new PatientEntity("DPU-B", "PAT-B", "Patient Beta", "FEMININ",
                LocalDate.of(1990, 6, 20), "+237699000000", "Yaoundé", "", "", "", "", "", "");
        patientB = patientRepository.save(patientB);
        visitB = visitRepository.save(new VisitEntity(patientB, "VIS-B001", "Consultation pré-natale", "Gynécologie"));
        TenantContext.clear();
    }

    @Test
    void simultaneousConsultationSavesRejectTheSecondStaleRevision() throws Exception {
        String initial = mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                .header("Authorization", "Bearer " + tokenMedecinA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"symptoms\":\"Fièvre\",\"diagnosis\":\"Initial\"}"))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        String updatedAt = tools.jackson.databind.json.JsonMapper.builder().build().readTree(initial).get("updatedAt").asString();
        String body = "{\"symptoms\":\"Fièvre\",\"diagnosis\":\"Révision\",\"expectedUpdatedAt\":\"%s\"}".formatted(updatedAt);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> save = () -> {
                start.await();
                return mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA).contentType(MediaType.APPLICATION_JSON)
                        .content(body)).andReturn().getResponse().getStatus();
            };
            var first = workers.submit(save); var second = workers.submit(save); start.countDown();
            var results = java.util.stream.Stream.of(first.get(15, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(15, java.util.concurrent.TimeUnit.SECONDS)).sorted().toList();
            org.junit.jupiter.api.Assertions.assertEquals(409, results.get(1));
            org.junit.jupiter.api.Assertions.assertTrue(results.get(0) >= 200 && results.get(0) < 300);
        }
    }

    @Test
    void givenMedecinA_whenSaveConsultation_thenSuccess() throws Exception {
        String json = "{\"symptoms\":\"Fièvre à 39°C, frissons, maux de tête\",\"clinicalExam\":\"Gorge rouge, amygdales hypertrophiées\",\"diagnosis\":\"Angine bactérienne\",\"advice\":\"Repos, hydratation, éviter les contacts\",\"followUp\":\"Contrôle dans 7 jours\"}";
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value(org.hamcrest.Matchers.startsWith("DOC-CONS-")))
                .andExpect(jsonPath("$.diagnosis").value("Angine bactérienne"))
                .andExpect(jsonPath("$.symptoms").value("Fièvre à 39°C, frissons, maux de tête"))
                .andExpect(jsonPath("$.doctorName").value("Dr. Alpha"))
                .andExpect(jsonPath("$.status").value("BROUILLON"))
                .andExpect(jsonPath("$.visitNumber").value("VIS-A001"));
    }

    @Test
    void givenSoapAssessment_whenSaveAndRead_thenExposesOnlyOneDiagnosisField() throws Exception {
        String json = """
                {
                  "symptoms": "Douleur thoracique depuis deux heures",
                  "clinicalExam": "Auscultation normale",
                  "diagnosis": "Reflux gastro-œsophagien",
                  "conclusion": "Absence de signe de gravité immédiat",
                  "advice": "Consulter en urgence si aggravation",
                  "followUp": "Contrôle dans 48 heures"
                }
                """;

        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Reflux gastro-œsophagien"))
                .andExpect(jsonPath("$.suspectedDiagnosis").doesNotExist())
                .andExpect(jsonPath("$.finalDiagnosis").doesNotExist())
                .andExpect(jsonPath("$.conclusion").value("Absence de signe de gravité immédiat"));

        mockMvc.perform(get("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Reflux gastro-œsophagien"))
                .andExpect(jsonPath("$.suspectedDiagnosis").doesNotExist())
                .andExpect(jsonPath("$.finalDiagnosis").doesNotExist())
                .andExpect(jsonPath("$.advice").value("Consulter en urgence si aggravation"))
                .andExpect(jsonPath("$.followUp").value("Contrôle dans 48 heures"));
    }

    @Test
    void givenExistingConsultation_whenSaveAgain_thenUpsertSuccess() throws Exception {
        String json1 = "{\"symptoms\":\"Douleur thoracique\",\"diagnosis\":\"Suspicion angine de poitrine\"}";
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isOk());
        String json2 = "{\"symptoms\":\"Douleur thoracique irradiant dans le bras gauche\",\"clinicalExam\":\"ECG normal, auscultation normale\",\"diagnosis\":\"Douleur musculo-squelettique\",\"advice\":\"Repos, antalgiques\",\"followUp\":\"Bilan cardiologique si récidive\"}";
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON).content(json2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Douleur musculo-squelettique"))
                .andExpect(jsonPath("$.clinicalExam").value("ECG normal, auscultation normale"));
    }

    @Test
    void givenMedecinA_whenGetConsultation_thenSuccess() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        consultationRepository.save(new ConsultationEntity(
                visitA, userMedecinA, "DOC-CONS-20260702-000001",
                "Toux chronique", "Poumons sains à l'auscultation",
                "Bronchite virale", "Sirop, repos", "Contrôle si aggravation"));
        TenantContext.clear();
        mockMvc.perform(get("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Bronchite virale"))
                .andExpect(jsonPath("$.doctorName").value("Dr. Alpha"))
                .andExpect(jsonPath("$.visitNumber").value("VIS-A001"));
    }

    @Test
    void givenAgentAccueil_whenSaveConsultation_thenForbidden() throws Exception {
        String json = "{\"symptoms\":\"Tentative non autorisée\",\"diagnosis\":\"Interdit\"}";
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenAgentA)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenMissingSymptoms_whenSaveConsultation_thenBadRequest() throws Exception {
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Diagnostic sans symptômes\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenMissingDiagnosis_whenSaveDraft_thenPersistsEmptyDiagnosis() throws Exception {
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Fatigue depuis trois semaines et toux sèche\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symptoms").value("Fatigue depuis trois semaines et toux sèche"))
                .andExpect(jsonPath("$.diagnosis").value(""))
                .andExpect(jsonPath("$.status").value("BROUILLON"));

        mockMvc.perform(get("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symptoms").value("Fatigue depuis trois semaines et toux sèche"))
                .andExpect(jsonPath("$.diagnosis").value(""));
    }

    @Test
    void givenUnknownVisit_whenSaveConsultation_thenNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        mockMvc.perform(post("/api/visits/" + unknownId + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Symptômes valides\",\"diagnosis\":\"Diagnostic valide\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNoConsultation_whenGetConsultation_thenNoContent() throws Exception {
        mockMvc.perform(get("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenNoToken_whenSaveConsultation_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Symptômes\",\"diagnosis\":\"Diagnostic\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenMedecinB_whenSaveConsultationOnVisitA_thenExplicitForbidden() throws Exception {
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Tentative cross-tenant\",\"diagnosis\":\"Accès interdit\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CONSENT_REQUIRED"))
                .andExpect(jsonPath("$.error.action").value("REQUEST_ACCESS"))
                .andExpect(jsonPath("$.error.required_scope").value("medical_records"));
    }

    @Test
    void closingVisitSealsConsultationAndRejectsEditsEvenIfVisitIsReopened() throws Exception {
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Fièvre\",\"diagnosis\":\"Diagnostic initial\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/close")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDEE"))
                .andExpect(jsonPath("$.signedBy").value(userMedecinA.getId().toString()))
                .andExpect(jsonPath("$.signedAt").isNotEmpty())
                .andExpect(jsonPath("$.signedContentHash").value(org.hamcrest.Matchers.matchesPattern("[a-f0-9]{64}")));
        String originalHash = jdbcTemplate.queryForObject(
                "SELECT signed_content_hash FROM consultations WHERE visit_id = ?", String.class, visitA.getId());
        jdbcTemplate.update("UPDATE visits SET status = 'EN_COURS' WHERE id = ?", visitA.getId());
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Altération\",\"diagnosis\":\"Diagnostic modifié\"}"))
                .andExpect(status().isConflict());
        org.junit.jupiter.api.Assertions.assertEquals(originalHash, jdbcTemplate.queryForObject(
                "SELECT signed_content_hash FROM consultations WHERE visit_id = ?", String.class, visitA.getId()));
        org.junit.jupiter.api.Assertions.assertEquals("Diagnostic initial", jdbcTemplate.queryForObject(
                "SELECT diagnosis FROM consultations WHERE visit_id = ?", String.class, visitA.getId()));
    }

    @Test
    void givenClosedVisit_whenSaveConsultation_thenBadRequest() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        visitA.setStatus("TERMINEE");
        visitRepository.save(visitA);
        TenantContext.clear();
        mockMvc.perform(post("/api/visits/" + visitA.getId() + "/consultation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Symptômes sur visite clôturée\",\"diagnosis\":\"Ne devrait pas passer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Une consultation ne peut être saisie que sur une visite active (EN_COURS)."));
    }
}
