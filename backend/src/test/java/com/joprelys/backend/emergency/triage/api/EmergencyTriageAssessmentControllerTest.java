package com.joprelys.backend.emergency.triage.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.LocalDate;
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
class EmergencyTriageAssessmentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String doctorToken;
    private String agentToken;
    private String otherTenantDoctorToken;
    private PatientEntity patient;

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

        OrganizationEntity organizationA = organizationRepository.save(
                new OrganizationEntity("Clinique A", "triage-a@joprelys.local", "123", "Rue A", "Douala"));
        OrganizationEntity organizationB = organizationRepository.save(
                new OrganizationEntity("Clinique B", "triage-b@joprelys.local", "456", "Rue B", "Douala"));

        doctorToken = token("doctor.triage.a@joprelys.local", "Médecin A", "MEDECIN", organizationA.getId());
        agentToken = token("agent.triage.a@joprelys.local", "Agent A", "AGENT_ACCUEIL", organizationA.getId());
        otherTenantDoctorToken = token(
                "doctor.triage.b@joprelys.local",
                "Médecin B",
                "MEDECIN",
                organizationB.getId());

        TenantContext.setTenantId(organizationA.getId());
        patient = patientRepository.save(new PatientEntity(
                "DPU-TRIAGE-A",
                "PAT-TRIAGE-A",
                "Patient Triage",
                "MASCULIN",
                LocalDate.of(1988, 4, 12),
                "+237699100200",
                "Douala",
                "",
                "",
                "",
                "",
                "",
                ""));
        TenantContext.clear();
    }

    @Test
    void shouldCreateInitialAssessmentThenAppendReassessmentAndRejectAfterStabilization() throws Exception {
        UUID emergencyId = createEmergency();

        mockMvc.perform(get(historyUrl(emergencyId))
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].assessmentType").value("INITIAL"))
                .andExpect(jsonPath("$[0].sequenceNumber").value(1))
                .andExpect(jsonPath("$[0].airwayStatus").value("PATENT"))
                .andExpect(jsonPath("$[0].oxygenSaturation").value(94));

        mockMvc.perform(post(historyUrl(emergencyId))
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reassessmentPayload()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(
                        "/api/emergencies/" + emergencyId + "/triage-assessments/")))
                .andExpect(jsonPath("$.assessmentType").value("REASSESSMENT"))
                .andExpect(jsonPath("$.sequenceNumber").value(2))
                .andExpect(jsonPath("$.breathingStatus").value("DISTRESS"))
                .andExpect(jsonPath("$.recommendedOrientation").value("RESUSCITATION"));

        mockMvc.perform(get(historyUrl(emergencyId))
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].sequenceNumber").value(2));

        mockMvc.perform(post("/api/emergencies/" + emergencyId + "/stabilize")
                        .header("Authorization", bearer(doctorToken))
                        .param("orientation", "HOSPITALIZATION"))
                .andExpect(status().isOk());

        mockMvc.perform(post(historyUrl(emergencyId))
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reassessmentPayload()))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectReassessmentWhenOneAbcdeAxisIsNotAssessed() throws Exception {
        UUID emergencyId = createEmergency();
        String incomplete = reassessmentPayload().replace(
                "\"airwayStatus\": \"PATENT\"",
                "\"airwayStatus\": \"NOT_ASSESSED\"");

        mockMvc.perform(post(historyUrl(emergencyId))
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incomplete))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(historyUrl(emergencyId))
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldEnforceWritePermissionAndTenantIsolation() throws Exception {
        UUID emergencyId = createEmergency();

        mockMvc.perform(post(historyUrl(emergencyId))
                        .header("Authorization", bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reassessmentPayload()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(historyUrl(emergencyId))
                        .header("Authorization", bearer(otherTenantDoctorToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(historyUrl(emergencyId))
                        .header("Authorization", bearer(otherTenantDoctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reassessmentPayload()))
                .andExpect(status().isNotFound());
    }

    private UUID createEmergency() throws Exception {
        String response = mockMvc.perform(post("/api/emergencies")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initialEmergencyPayload()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return UUID.fromString(json.path("id").asText());
    }

    private String initialEmergencyPayload() {
        return """
                {
                  "patientId": "%s",
                  "arrivalMode": "AMBULANCE",
                  "triageLevel": "RED",
                  "hemodynamicStatus": "SHOCK",
                  "chiefComplaint": "Traumatisme thoracique",
                  "initialBpSystolic": 88,
                  "initialBpDiastolic": 54,
                  "initialHr": 128,
                  "initialTemp": 37.4,
                  "abcdeAssessment": {
                    "airwayStatus": "PATENT",
                    "breathingStatus": "DISTRESS",
                    "circulationStatus": "SHOCK",
                    "disabilityStatus": "RESPONDS_TO_VOICE",
                    "exposureStatus": "TRAUMA",
                    "respiratoryRate": 32,
                    "oxygenSaturation": 94,
                    "gcsScore": 12,
                    "painScore": 8,
                    "recommendedOrientation": "RESUSCITATION",
                    "clinicalNotes": "Évaluation initiale structurée."
                  }
                }
                """.formatted(patient.getId());
    }

    private String reassessmentPayload() {
        return """
                {
                  "triageLevel": "ORANGE",
                  "hemodynamicStatus": "UNSTABLE",
                  "bpSystolic": 96,
                  "bpDiastolic": 62,
                  "heartRate": 112,
                  "temperature": 37.2,
                  "abcdeAssessment": {
                    "airwayStatus": "PATENT",
                    "breathingStatus": "DISTRESS",
                    "circulationStatus": "COMPROMISED",
                    "disabilityStatus": "ALERT",
                    "exposureStatus": "TRAUMA",
                    "respiratoryRate": 26,
                    "oxygenSaturation": 96,
                    "gcsScore": 15,
                    "painScore": 6,
                    "recommendedOrientation": "RESUSCITATION",
                    "clinicalNotes": "Amélioration partielle après prise en charge."
                  }
                }
                """;
    }

    private String token(String email, String name, String role, UUID organizationId) {
        UserAccountEntity user = new UserAccountEntity(email, name, role, "passhash");
        user.setOrganizationId(organizationId);
        return jwtService.createToken(userAccountRepository.save(user)).value();
    }

    private String historyUrl(UUID emergencyId) {
        return "/api/emergencies/" + emergencyId + "/triage-assessments";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}