package com.joprelys.backend.hospitalization.api;

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
public class HospitalizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository hospitalizationRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private OrganizationEntity org;
    private UserAccountEntity doctor;
    private PatientEntity patientA;
    private PatientEntity patientB;
    private String tokenDoctor;

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
        org = new OrganizationEntity("Clinique Hospitalisation", "hosp@joprelys.local", "123456", "Street", "Douala");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        // Create Doctor
        doctor = new UserAccountEntity("dr.hosp@joprelys.local", "Dr. House", "MEDECIN", "passhash");
        doctor.setOrganizationId(org.getId());
        doctor = userAccountRepository.save(doctor);

        tokenDoctor = jwtService.createToken(doctor).value();

        // Create Patients
        patientA = new PatientEntity("DPU-H-00001", "PAT-H-001", "Alice Hospitalisée", "FEMININ", LocalDate.of(1990, 5, 10), "+237699999991", "Douala", "Akwa", "Street A", "Bob", "+237699445566", "Aucune", "Aucun");
        patientB = new PatientEntity("DPU-H-00002", "PAT-H-002", "Bob Hospitalisé", "MASCULIN", LocalDate.of(1985, 7, 20), "+237699999992", "Douala", "Akwa", "Street B", "Alice", "+237699445577", "Aucune", "Aucun");

        patientA = patientRepository.save(patientA);
        patientB = patientRepository.save(patientB);

        TenantContext.clear();
    }

    @Test
    void givenDoctor_whenAdmitAndDischargePatient_thenSuccess() throws Exception {
        // 1. Admit Patient A in Room 101, Bed A
        String admitRequest = String.format("""
                {
                    "patientId": "%s",
                    "serviceName": "PÉDIATRIE",
                    "roomNumber": "101",
                    "bedNumber": "Lit A",
                    "admissionReason": "Surveillance post-opératoire"
                }
                """, patientA.getId());

        String responseStr = mockMvc.perform(post("/api/hospitalizations")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(admitRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andExpect(jsonPath("$.roomNumber").value("101"))
                .andExpect(jsonPath("$.bedNumber").value("Lit A"))
                .andReturn().getResponse().getContentAsString();

        String hospId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor()
                .newInstance().readTree(responseStr).get("id").asText();

        // 2. Try to admit Patient B in the SAME Bed A (should fail with 409 Conflict due to index/check)
        String admitRequestB = String.format("""
                {
                    "patientId": "%s",
                    "serviceName": "PÉDIATRIE",
                    "roomNumber": "101",
                    "bedNumber": "Lit A",
                    "admissionReason": "Fièvre élevée"
                }
                """, patientB.getId());

        mockMvc.perform(post("/api/hospitalizations")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(admitRequestB))
                .andExpect(status().isConflict());

        // 3. Add Note
        String noteRequest = """
                {
                    "noteContent": "Température stable à 37.2°C, réveil calme."
                }
                """;

        mockMvc.perform(post("/api/hospitalizations/" + hospId + "/notes")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(noteRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.noteContent").value("Température stable à 37.2°C, réveil calme."))
                .andExpect(jsonPath("$.authorName").value("Dr. House"));

        // 4. Get Notes
        mockMvc.perform(get("/api/hospitalizations/" + hospId + "/notes")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].noteContent").value("Température stable à 37.2°C, réveil calme."));

        // 5. Discharge
        String dischargeRequest = """
                {
                    "dischargeDiagnosis": "Guérison complète, ablation des fils OK.",
                    "dischargeInstructions": "Repos de 5 jours, paracétamol en cas de douleur."
                }
                """;

        mockMvc.perform(post("/api/hospitalizations/" + hospId + "/discharge")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(dischargeRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SORTI"));

        // 6. Download discharge PDF
        mockMvc.perform(get("/api/hospitalizations/" + hospId + "/pdf")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.startsWith("application/pdf");
                });
    }
}
