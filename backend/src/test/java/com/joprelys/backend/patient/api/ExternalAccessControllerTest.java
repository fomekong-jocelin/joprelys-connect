package com.joprelys.backend.patient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ExternalAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserAccountEntity doctorA;
    private PatientEntity patientB; // patient belonging to orgB
    private PatientEntity patientA; // patient belonging to orgA
    private String tokenDoctorA;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM dispensation_items");
        jdbcTemplate.update("DELETE FROM prescription_dispensations");
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM vitals");
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM lab_results");
        jdbcTemplate.update("DELETE FROM lab_orders");
        jdbcTemplate.update("DELETE FROM hospitalization_notes");
        jdbcTemplate.update("DELETE FROM hospitalizations");
        jdbcTemplate.update("DELETE FROM patient_allergies");
        jdbcTemplate.update("DELETE FROM patient_medical_history");
        jdbcTemplate.update("DELETE FROM external_access_requests");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create Organization A
        orgA = new OrganizationEntity("Clinique A", "clinique.a@joprelys.local", "123456", "Street A", "Douala");
        orgA = organizationRepository.save(orgA);

        // Create Organization B
        orgB = new OrganizationEntity("Clinique B", "clinique.b@joprelys.local", "789012", "Street B", "Yaounde");
        orgB = organizationRepository.save(orgB);

        // Authenticate context under Org A
        TenantContext.setTenantId(orgA.getId());

        // Create Doctor in Org A
        doctorA = new UserAccountEntity("doctor.a@joprelys.local", "Dr. House", "MEDECIN", "passhash");
        doctorA.setOrganizationId(orgA.getId());
        doctorA = userAccountRepository.save(doctorA);

        tokenDoctorA = jwtService.createToken(doctorA).value();

        // Create Patient in Org A
        patientA = new PatientEntity(
                "DPU-JOP-20260704-11111",
                "PAT-11111",
                "Patient Local Org A",
                "MASCULIN",
                LocalDate.of(1990, 5, 10),
                "+237699999991",
                "Douala",
                null, null, null, null, null, null
        );
        patientA.setOrganizationId(orgA.getId());
        patientA = patientRepository.save(patientA);

        // Switch tenant to Org B to save Patient B
        TenantContext.setTenantId(orgB.getId());
        patientB = new PatientEntity(
                "DPU-JOP-20260704-22222",
                "PAT-22222",
                "Patient Externe Org B",
                "FEMININ",
                LocalDate.of(1995, 8, 20),
                "+237699999992",
                "Yaounde",
                null, null, null, null, null, null
        );
        patientB.setOrganizationId(orgB.getId());
        patientB = patientRepository.save(patientB);

        // Reset tenant context to Org A for the test execution
        TenantContext.setTenantId(orgA.getId());
    }

    @Test
    void shouldCreateExternalAccessRequestSuccessfully() throws Exception {
        String payload = """
                {
                    "patientDpu": "%s",
                    "reason": "Consultation cardiologique externe urgente",
                    "durationHours": 24
                }
                """.formatted(patientB.getGlobalPatientNumber());

        mockMvc.perform(post("/api/external-access/requests")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patientB.getId().toString()))
                .andExpect(jsonPath("$.reason").value("Consultation cardiologique externe urgente"))
                .andExpect(jsonPath("$.durationHours").value(24))
                .andExpect(jsonPath("$.status").value("EN_ATTENTE"));
    }

    @Test
    void shouldFailWhenPatientBelongsToSameOrganization() throws Exception {
        String payload = """
                {
                    "patientDpu": "%s",
                    "reason": "Consultation pour patient du meme etab",
                    "durationHours": 24
                }
                """.formatted(patientA.getGlobalPatientNumber());

        mockMvc.perform(post("/api/external-access/requests")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailWhenReasonTooShort() throws Exception {
        String payload = """
                {
                    "patientDpu": "%s",
                    "reason": "Court",
                    "durationHours": 24
                }
                """.formatted(patientB.getGlobalPatientNumber());

        mockMvc.perform(post("/api/external-access/requests")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailWhenPatientNotFound() throws Exception {
        String payload = """
                {
                    "patientDpu": "DPU-JOP-99999999-999999",
                    "reason": "Consultation externe patient inconnu",
                    "durationHours": 24
                }
                """;

        mockMvc.perform(post("/api/external-access/requests")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFailWhenDuplicateRequestPending() throws Exception {
        String payload = """
                {
                    "patientDpu": "%s",
                    "reason": "Premiere demande d'acces externe",
                    "durationHours": 12
                }
                """.formatted(patientB.getGlobalPatientNumber());

        // First attempt (success)
        mockMvc.perform(post("/api/external-access/requests")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Second attempt (fails with 409 Conflict)
        mockMvc.perform(post("/api/external-access/requests")
                        .header("Authorization", "Bearer " + tokenDoctorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }
}
