package com.joprelys.backend.emergency.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
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
class ProvisionalEmergencyAdmissionControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM emergency_admission_requests");
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

        OrganizationEntity organization = organizationRepository.save(
                new OrganizationEntity("Clinique A", "contact@joprelys.local", "123", "Street A", "Douala"));
        UserAccountEntity clinician = new UserAccountEntity(
                "clinician.atomic@joprelys.local", "Clinician", "MEDECIN", "passhash");
        clinician.setOrganizationId(organization.getId());
        clinician = userAccountRepository.save(clinician);
        token = jwtService.createToken(clinician).value();
    }

    @Test
    void shouldCreatePatientEmergencyAndInitialTriageAtomicallyAndIdempotently() throws Exception {
        UUID requestId = UUID.randomUUID();
        String payload = payload(requestId);

        String first = mockMvc.perform(post("/api/emergencies/provisional")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identityStatus").value("PROVISIONAL_URGENCY"))
                .andExpect(jsonPath("$.temporaryPatientNumber").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post("/api/emergencies/provisional")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertEquals(objectMapper.readTree(first).path("id").asText(), objectMapper.readTree(second).path("id").asText());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM emergency_admission_requests WHERE request_id = ?",
                Integer.class,
                requestId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM emergencies", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM emergency_triage_assessments WHERE assessment_type = 'INITIAL'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM patients WHERE identity_status = 'PROVISIONAL_URGENCY'",
                Integer.class));
    }

    @Test
    void shouldRejectInvalidTriageWithoutCreatingPatient() throws Exception {
        UUID requestId = UUID.randomUUID();
        String invalid = payload(requestId).replace("\"chiefComplaint\": \"Traumatisme\"", "\"chiefComplaint\": \"\"");

        mockMvc.perform(post("/api/emergencies/provisional")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM emergency_admission_requests", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM emergencies", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM emergency_triage_assessments", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patients", Integer.class));
    }

    private String payload(UUID requestId) {
        return """
                {
                  "requestId": "%s",
                  "patient": {
                    "apparentGender": "FEMININ",
                    "estimatedAgeRange": "35-45",
                    "foundLocation": "Bonamoussadi",
                    "confidenceLevel": "NONE",
                    "identityDeclarations": []
                  },
                  "emergency": {
                    "arrivalMode": "ACCOMPANIED",
                    "triageLevel": "RED",
                    "hemodynamicStatus": "SHOCK",
                    "chiefComplaint": "Traumatisme",
                    "initialHr": 110,
                    "thirdPartyName": "Paul Tamo",
                    "thirdPartyPhone": "+237699000111",
                    "thirdPartyRelationship": "WITNESS",
                    "thirdPartyConsentToContact": true
                  }
                }
                """.formatted(requestId);
    }
}