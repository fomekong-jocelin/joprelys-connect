package com.joprelys.backend.patient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Tâche 5 — Téléchargement PDF Synthèse Médicale")
public class PatientSummaryPdfTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private VitalsRepository vitalsRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private OrganizationEntity orgA;
    private PatientEntity patientA;
    private String tokenDoctor;
    private String tokenPatient;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM vitals");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Organization
        orgA = new OrganizationEntity("Clinique Test Summary", "summary@joprelys.local", "111", "Street Test", "Douala");
        orgA = organizationRepository.save(orgA);

        TenantContext.setTenantId(orgA.getId());

        // 2. Create Doctor User
        UserAccountEntity doctor = new UserAccountEntity("medecin.summary@joprelys.local", "Dr. Summary", "MEDECIN", "passhash");
        doctor.setOrganizationId(orgA.getId());
        doctor = userAccountRepository.save(doctor);

        // 3. Create Patient
        patientA = new PatientEntity(
                "PAT-SUM-000001",
                "LOC-SUM-001",
                "Patrice Synthese",
                "MASCULIN",
                LocalDate.of(1992, 6, 20),
                "+237 600 00 00 01",
                "Douala",
                "Akwa",
                "Rue de la Synthese",
                "Urgent Contact",
                "+237 600 00 00 02",
                "Aspirine",
                "Asthme"
        );
        patientA.setOrganizationId(orgA.getId());
        patientA = patientRepository.save(patientA);

        // 4. Create Vitals
        VisitEntity visit = new VisitEntity(patientA, "VIS-SUM-0001", "Motif de test", "CONSULTATION");
        visit.setOrganizationId(orgA.getId());
        visit = visitRepository.save(visit);

        VitalsEntity vitals = new VitalsEntity(
                visit,
                new BigDecimal("37.2"),
                new BigDecimal("75.5"),
                180,
                72,
                120,
                80,
                98,
                new BigDecimal("1.1"),
                16,
                new BigDecimal("23.3")
        );
        vitalsRepository.save(vitals);

        // 5. Generate Tokens
        tokenDoctor = jwtService.createToken(doctor).value();
        tokenPatient = jwtService.createPatientToken(patientA).value();

        TenantContext.clear();
    }

    @Test
    @DisplayName("Génération et téléchargement du PDF de synthèse médicale par un médecin autorisé → 200 OK")
    void downloadSummaryPdf_asDoctor_shouldReturnPdf() throws Exception {
        mockMvc.perform(get("/api/patients/" + patientA.getId() + "/summary-pdf")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"patient-summary-" + patientA.getId() + ".pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("Tentative de téléchargement par un rôle non autorisé (ex: PATIENT) → 403 Forbidden")
    void downloadSummaryPdf_asPatient_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/patients/" + patientA.getId() + "/summary-pdf")
                .header("Authorization", "Bearer " + tokenPatient))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tentative de téléchargement sans authentification → 401 Unauthorized")
    void downloadSummaryPdf_noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/patients/" + patientA.getId() + "/summary-pdf"))
                .andExpect(status().isUnauthorized());
    }
}
