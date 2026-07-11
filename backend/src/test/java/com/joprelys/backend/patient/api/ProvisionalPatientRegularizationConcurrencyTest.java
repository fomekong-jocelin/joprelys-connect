package com.joprelys.backend.patient.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
class ProvisionalPatientRegularizationConcurrencyTest {

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
        UserAccountEntity agent = new UserAccountEntity(
                "agent.concurrent@joprelys.local", "Agent", "AGENT_ACCUEIL", "passhash");
        agent.setOrganizationId(organization.getId());
        agent = userAccountRepository.save(agent);
        token = jwtService.createToken(agent).value();
    }

    @Test
    void shouldAllowOnlyOneConcurrentRegularization() throws Exception {
        UUID patientId = createProvisional();
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);

        Callable<Integer> first = () -> regularize(patientId, "Patient Alpha", start);
        Callable<Integer> second = () -> regularize(patientId, "Patient Beta", start);
        var firstFuture = executor.submit(first);
        var secondFuture = executor.submit(second);
        start.countDown();

        Set<Integer> statuses = Set.of(firstFuture.get(), secondFuture.get());
        executor.shutdownNow();

        assertEquals(Set.of(200, 409), statuses);
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM patient_identity_status_history WHERE patient_id = ? AND new_status = 'VERIFIED'",
                Integer.class,
                patientId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE resource_id = ? AND action = 'REGULARIZE_PROVISIONAL_PATIENT'",
                Integer.class,
                patientId));
        assertTrue(Set.of("Patient Alpha", "Patient Beta")
                .contains(patientRepository.findByIdGlobally(patientId).orElseThrow().getFullName()));
    }

    @Test
    void shouldRejectFutureBirthDateAndInvalidEmail() throws Exception {
        UUID patientId = createProvisional();
        String invalid = """
                {
                  "fullName": "Patient Test",
                  "gender": "MASCULIN",
                  "birthDate": "2999-01-01",
                  "city": "Douala",
                  "email": "invalid-email",
                  "sourceType": "PATIENT",
                  "sourceDetails": "Déclaration directe",
                  "reason": "Patient conscient"
                }
                """;

        mockMvc.perform(put("/api/patients/provisional/" + patientId + "/identity")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());

        assertEquals("PROVISIONAL_URGENCY", patientRepository.findByIdGlobally(patientId)
                .orElseThrow().getIdentityStatus().name());
    }

    private UUID createProvisional() throws Exception {
        String response = mockMvc.perform(post("/api/patients/provisional")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).path("patient").path("id").asText());
    }

    private int regularize(UUID patientId, String fullName, CountDownLatch start) throws Exception {
        start.await();
        String payload = """
                {
                  "fullName": "%s",
                  "gender": "MASCULIN",
                  "birthDate": "1990-01-10",
                  "city": "Douala",
                  "sourceType": "PATIENT",
                  "sourceDetails": "Déclaration directe",
                  "reason": "Patient conscient"
                }
                """.formatted(fullName);
        return mockMvc.perform(put("/api/patients/provisional/" + patientId + "/identity")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn().getResponse().getStatus();
    }
}
