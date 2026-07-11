package com.joprelys.backend.patient.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.util.UUID;
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
class ProvisionalPatientRegularizationControllerTest {

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
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenAgentA;
    private String tokenAgentB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM resuscitation_logs");
        jdbcTemplate.update("DELETE FROM emergencies");
        jdbcTemplate.update("DELETE FROM patient_identity_declarations");
        jdbcTemplate.update("DELETE FROM patient_identity_status_history");
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
                "agent.regularization.a@joprelys.local", "Agent A", "AGENT_ACCUEIL", "passhash");
        agentA.setOrganizationId(orgA.getId());
        agentA = userAccountRepository.save(agentA);

        UserAccountEntity agentB = new UserAccountEntity(
                "agent.regularization.b@joprelys.local", "Agent B", "AGENT_ACCUEIL", "passhash");
        agentB.setOrganizationId(orgB.getId());
        agentB = userAccountRepository.save(agentB);

        tokenAgentA = jwtService.createToken(agentA).value();
        tokenAgentB = jwtService.createToken(agentB).value();
    }

    @Test
    void shouldRegularizeUrgTempWithoutLosingTemporaryAliasOrHistory() throws Exception {
        Creation creation = createProvisional(tokenAgentA);

        String request = """
                {
                  "fullName": "Nadège Maffock",
                  "gender": "FEMININ",
                  "birthDate": "1987-04-18",
                  "phone": "+237699112233",
                  "city": "Douala",
                  "district": "Bonamoussadi",
                  "address": "Rue 12",
                  "email": "nadege@example.com",
                  "emergencyContactName": "Paul Maffock",
                  "emergencyContactPhone": "+237677445566",
                  "sourceType": "DOCUMENT",
                  "sourceDetails": "CNI 123456789 présentée après reprise de conscience",
                  "reason": "Identité confirmée par le patient et sa pièce d'identité"
                }
                """;

        mockMvc.perform(put("/api/patients/provisional/" + creation.patientId() + "/identity")
                        .header("Authorization", "Bearer " + tokenAgentA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nadège Maffock"))
                .andExpect(jsonPath("$.displayName").value("Nadège Maffock"))
                .andExpect(jsonPath("$.identityStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.identityConfidenceLevel").value("VERIFIED"))
                .andExpect(jsonPath("$.temporaryPatientNumber").value(creation.temporaryNumber()))
                .andExpect(jsonPath("$.globalPatientNumber").value(creation.globalNumber()));

        var patient = patientRepository.findByIdGlobally(creation.patientId()).orElseThrow();
        assertEquals(PatientIdentityStatus.VERIFIED, patient.getIdentityStatus());
        assertEquals("Nadège Maffock", patient.getFullName());
        assertEquals(creation.temporaryNumber(), patient.getTemporaryPatientNumber());
        assertEquals(creation.globalNumber(), patient.getGlobalPatientNumber());

        Integer verifiedDeclarations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM patient_identity_declarations WHERE patient_id = ? AND verification_status = 'VERIFIED'",
                Integer.class,
                creation.patientId());
        Integer statusChanges = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM patient_identity_status_history WHERE patient_id = ? AND new_status = 'VERIFIED'",
                Integer.class,
                creation.patientId());
        Integer auditEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE resource_id = ? AND action = 'REGULARIZE_PROVISIONAL_PATIENT'",
                Integer.class,
                creation.patientId());

        assertNotNull(verifiedDeclarations);
        assertEquals(6, verifiedDeclarations);
        assertEquals(1, statusChanges);
        assertEquals(1, auditEvents);
    }

    @Test
    void shouldHideProvisionalPatientFromAnotherTenantDuringRegularization() throws Exception {
        Creation creation = createProvisional(tokenAgentA);

        mockMvc.perform(put("/api/patients/provisional/" + creation.patientId() + "/identity")
                        .header("Authorization", "Bearer " + tokenAgentB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMinimalRegularization()))
                .andExpect(status().isNotFound());

        var patient = patientRepository.findByIdGlobally(creation.patientId()).orElseThrow();
        assertEquals(PatientIdentityStatus.PROVISIONAL_URGENCY, patient.getIdentityStatus());
    }

    @Test
    void shouldRejectSecondRegularizationOfVerifiedPatient() throws Exception {
        Creation creation = createProvisional(tokenAgentA);

        mockMvc.perform(put("/api/patients/provisional/" + creation.patientId() + "/identity")
                        .header("Authorization", "Bearer " + tokenAgentA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMinimalRegularization()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/patients/provisional/" + creation.patientId() + "/identity")
                        .header("Authorization", "Bearer " + tokenAgentA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMinimalRegularization()))
                .andExpect(status().isConflict());
    }

    private Creation createProvisional(String token) throws Exception {
        String response = mockMvc.perform(post("/api/patients/provisional")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apparentGender\":\"FEMININ\",\"estimatedAgeRange\":\"35-45\",\"foundLocation\":\"Bonamoussadi\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var patient = objectMapper.readTree(response).path("patient");
        return new Creation(
                UUID.fromString(patient.path("id").asText()),
                patient.path("temporaryPatientNumber").asText(),
                patient.path("globalPatientNumber").asText());
    }

    private String validMinimalRegularization() {
        return """
                {
                  "fullName": "Patient Identifié",
                  "gender": "MASCULIN",
                  "birthDate": "1990-01-10",
                  "city": "Douala",
                  "sourceType": "PATIENT",
                  "sourceDetails": "Déclaration directe après reprise de conscience",
                  "reason": "Patient conscient et cohérent"
                }
                """;
    }

    private record Creation(UUID patientId, String temporaryNumber, String globalNumber) {
    }
}
