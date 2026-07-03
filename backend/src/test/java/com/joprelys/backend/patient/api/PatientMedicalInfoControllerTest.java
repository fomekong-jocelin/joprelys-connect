package com.joprelys.backend.patient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PatientMedicalInfoControllerTest {

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
    private UserAccountEntity doctorA;
    private PatientEntity patientA;
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

        // Create Org
        orgA = new OrganizationEntity("Clinique Test S6", "test.s6@joprelys.local", "123456", "Street S6", "Douala");
        orgA = organizationRepository.save(orgA);

        TenantContext.setTenantId(orgA.getId());

        // Create Doctor User
        doctorA = new UserAccountEntity("dr.s6@joprelys.local", "Dr. House", "MEDECIN", "passhash");
        doctorA.setOrganizationId(orgA.getId());
        doctorA = userAccountRepository.save(doctorA);

        // Generate Token
        tokenDoctorA = jwtService.createToken(doctorA).value();

        // Create Patient
        patientA = new PatientEntity(
                "DPU-JOP-20260703-99999",
                "PAT-99999",
                "Patient Test S6",
                "MASCULIN",
                LocalDate.of(1990, 5, 10),
                "+237699999999",
                "Douala",
                "Akwa",
                "Street 9",
                "Bob",
                "+237699445566",
                "Aucune",
                "Aucun"
        );
        patientA = patientRepository.save(patientA);

        TenantContext.clear();
    }

    @Test
    void givenDoctor_whenAddAndGetAllergy_thenSuccess() throws Exception {
        String addRequest = """
                {
                    "substance": "Pénicilline",
                    "severity": "CRITICAL",
                    "reaction": "Choc anaphylactique",
                    "status": "ACTIVE",
                    "discoveredAt": "2026-01-01",
                    "comment": "Antécédent grave de réaction à l'amoxicilline"
                }
                """;

        // Add
        mockMvc.perform(post("/api/patients/" + patientA.getId() + "/allergies")
                .header("Authorization", "Bearer " + tokenDoctorA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.substance").value("Pénicilline"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.reaction").value("Choc anaphylactique"));

        // Get
        mockMvc.perform(get("/api/patients/" + patientA.getId() + "/allergies")
                .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].substance").value("Pénicilline"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));
    }

    @Test
    void givenDoctor_whenAddAndUpdateMedicalHistory_thenSuccess() throws Exception {
        String addHistory = """
                {
                    "category": "MEDICAL",
                    "description": "Diabète de Type 2",
                    "onsetDate": "2020-05-15",
                    "isOngoing": true,
                    "comment": "Traité par Metformine"
                }
                """;

        // Add
        String responseStr = mockMvc.perform(post("/api/patients/" + patientA.getId() + "/medical-history")
                .header("Authorization", "Bearer " + tokenDoctorA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addHistory))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("MEDICAL"))
                .andExpect(jsonPath("$.description").value("Diabète de Type 2"))
                .andExpect(jsonPath("$.isOngoing").value(true))
                .andReturn().getResponse().getContentAsString();

        String historyId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor()
                .newInstance().readTree(responseStr).get("id").asText();

        // Update to not ongoing
        String updateHistory = """
                {
                    "category": "MEDICAL",
                    "description": "Diabète de Type 2",
                    "onsetDate": "2020-05-15",
                    "isOngoing": false,
                    "comment": "Diabète équilibré par le régime"
                }
                """;

        mockMvc.perform(put("/api/patients/" + patientA.getId() + "/medical-history/" + historyId)
                .header("Authorization", "Bearer " + tokenDoctorA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateHistory))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOngoing").value(false))
                .andExpect(jsonPath("$.comment").value("Diabète équilibré par le régime"));
    }
}
