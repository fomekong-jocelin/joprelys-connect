package com.joprelys.backend.patient.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientIdentityDeclarationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientIdentityStatusHistoryRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class ProvisionalPatientControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientIdentityDeclarationRepository declarationRepository;

    @Autowired
    private PatientIdentityStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenAgentA;
    private String tokenAgentB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity orgA = organizationRepository.save(
                new OrganizationEntity("Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala"));
        OrganizationEntity orgB = organizationRepository.save(
                new OrganizationEntity("Clinique B", "contactb@joprelys.local", "456", "Street B", "Yaoundé"));

        UserAccountEntity agentA = new UserAccountEntity(
                "agent.a@joprelys.local", "Agent A", "AGENT_ACCUEIL", "passhash");
        agentA.setOrganizationId(orgA.getId());
        agentA = userAccountRepository.save(agentA);

        UserAccountEntity agentB = new UserAccountEntity(
                "agent.b@joprelys.local", "Agent B", "AGENT_ACCUEIL", "passhash");
        agentB.setOrganizationId(orgB.getId());
        agentB = userAccountRepository.save(agentB);

        tokenAgentA = jwtService.createToken(agentA).value();
        tokenAgentB = jwtService.createToken(agentB).value();
    }

    @Test
    void shouldCreateMinimalUrgTempWithoutInventedIdentity() throws Exception {
        String response = mockMvc.perform(post("/api/patients/provisional")
                        .header("Authorization", "Bearer " + tokenAgentA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patient.identityStatus").value("PROVISIONAL_URGENCY"))
                .andExpect(jsonPath("$.patient.temporaryPatientNumber").value(
                        org.hamcrest.Matchers.matchesPattern("URG-TEMP-[0-9]{8}-[0-9]{6}")))
                .andExpect(jsonPath("$.patient.displayName").value(
                        org.hamcrest.Matchers.startsWith("URG-TEMP-")))
                .andExpect(jsonPath("$.identityDeclarations.length()").value(0))
                .andReturn().getResponse().getContentAsString();

        UUID patientId = UUID.fromString(objectMapper.readTree(response).path("patient").path("id").asText());
        var patient = patientRepository.findById(patientId).orElseThrow();

        assertEquals(PatientIdentityStatus.PROVISIONAL_URGENCY, patient.getIdentityStatus());
        assertNull(patient.getFullName());
        assertNull(patient.getGender());
        assertNull(patient.getBirthDate());
        assertNull(patient.getCity());
        assertNull(patient.getPhone());
        assertEquals(1, statusHistoryRepository.findAllByPatientIdOrderByChangedAtAsc(patientId).size());
    }

    @Test
    void shouldPersistSourcedIdentityDeclarations() throws Exception {
        String request = """
                {
                  "apparentGender": "MASCULIN",
                  "estimatedAgeRange": "35-45",
                  "physicalDescription": "Cicatrice au front",
                  "foundLocation": "Bonamoussadi",
                  "confidenceLevel": "LOW",
                  "identityDeclarations": [
                    {
                      "fieldName": "fullName",
                      "value": "Nom déclaré non vérifié",
                      "sourceType": "ACCOMPANYING_PERSON",
                      "sourceDetails": "Personne ayant amené le patient",
                      "confidenceLevel": "LOW"
                    }
                  ]
                }
                """;

        String response = mockMvc.perform(post("/api/patients/provisional")
                        .header("Authorization", "Bearer " + tokenAgentA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patient.apparentGender").value("MASCULIN"))
                .andExpect(jsonPath("$.patient.estimatedAgeRange").value("35-45"))
                .andExpect(jsonPath("$.identityDeclarations[0].fieldName").value("fullName"))
                .andExpect(jsonPath("$.identityDeclarations[0].sourceType").value("ACCOMPANYING_PERSON"))
                .andExpect(jsonPath("$.identityDeclarations[0].verificationStatus").value("DECLARED"))
                .andReturn().getResponse().getContentAsString();

        UUID patientId = UUID.fromString(objectMapper.readTree(response).path("patient").path("id").asText());
        var declarations = declarationRepository.findAllByPatientIdOrderByDeclaredAtAsc(patientId);
        assertEquals(1, declarations.size());
        assertEquals("Nom déclaré non vérifié", declarations.getFirst().getDeclaredValue());
        assertNotNull(declarations.getFirst().getDeclaredBy());
    }

    @Test
    void shouldKeepUrgTempSearchPrivateToOwningOrganization() throws Exception {
        String response = createMinimal(tokenAgentA);
        String temporaryNumber = objectMapper.readTree(response)
                .path("patient").path("temporaryPatientNumber").asText();

        mockMvc.perform(get("/api/patients").param("q", temporaryNumber)
                        .header("Authorization", "Bearer " + tokenAgentA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/patients").param("q", temporaryNumber)
                        .header("Authorization", "Bearer " + tokenAgentB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldRejectVerifiedConfidenceForProvisionalIdentity() throws Exception {
        mockMvc.perform(post("/api/patients/provisional")
                        .header("Authorization", "Bearer " + tokenAgentA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confidenceLevel\":\"VERIFIED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGenerateDistinctNumbersForConcurrentCreations() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        Callable<String> create = () -> {
            start.await();
            return createMinimal(tokenAgentA);
        };

        Future<String> first = executor.submit(create);
        Future<String> second = executor.submit(create);
        start.countDown();

        JsonNode firstJson = objectMapper.readTree(first.get());
        JsonNode secondJson = objectMapper.readTree(second.get());
        executor.shutdownNow();

        String firstTemporary = firstJson.path("patient").path("temporaryPatientNumber").asText();
        String secondTemporary = secondJson.path("patient").path("temporaryPatientNumber").asText();
        String firstDpu = firstJson.path("patient").path("globalPatientNumber").asText();
        String secondDpu = secondJson.path("patient").path("globalPatientNumber").asText();

        assertNotEquals(firstTemporary, secondTemporary);
        assertNotEquals(firstDpu, secondDpu);
        assertEquals(Set.of(firstTemporary, secondTemporary).size(), 2);
        assertTrue(patientRepository.existsByTemporaryPatientNumber(firstTemporary));
        assertTrue(patientRepository.existsByTemporaryPatientNumber(secondTemporary));
        assertFalse(firstTemporary.isBlank());
    }

    private String createMinimal(String token) throws Exception {
        return mockMvc.perform(post("/api/patients/provisional")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
