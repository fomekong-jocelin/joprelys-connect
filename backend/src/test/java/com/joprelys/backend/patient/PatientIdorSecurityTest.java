package com.joprelys.backend.patient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests de sécurité IDOR (Insecure Direct Object Reference) pour le portail patient.
 * OWASP API Security Top 10 : A01:2023 - Broken Object Level Authorization.
 *
 * Vérifie que :
 * 1. Un patient authentifié accède à son propre profil (200).
 * 2. Un accès sans token est rejeté (401).
 * 3. Un patient ne peut pas accéder au document d'un autre patient (403/404).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("STORY-1102 — Sécurité IDOR Portail Patient (OWASP A01)")
public class PatientIdorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private MedicalDocumentRepository medicalDocumentRepository;

    @Autowired
    private com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository userAccountRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private PatientEntity patientA;
    private PatientEntity patientB;
    private VisitEntity visitB;
    private String tokenPatientA;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM emergency_access_authorizations");
        jdbcTemplate.update("DELETE FROM patient_consents");
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // Créer l'organisation
        OrganizationEntity org = new OrganizationEntity(
                "Clinique IDOR Test", "contact@idortest.org", "123IDOR", "Adresse IDOR", "Douala");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        // Patient A — le patient authentifié
        patientA = new PatientEntity(
                "PAT-IDOR-000001",
                "LOC-IDOR-001",
                "Alice Authentifiée",
                "FEMININ",
                LocalDate.of(1988, 3, 15),
                "+237 699 11 22 33",
                "Douala",
                "Akwa",
                "Rue IDOR A",
                "Bob",
                "+237 699 44 55 66",
                "Aucune",
                "Aucun"
        );
        patientA = patientRepository.save(patientA);

        // Patient B — la victime potentielle d'un accès IDOR
        patientB = new PatientEntity(
                "PAT-IDOR-000002",
                "LOC-IDOR-002",
                "Bob Confidentiel",
                "MASCULIN",
                LocalDate.of(1975, 7, 20),
                "+237 699 77 88 99",
                "Yaoundé",
                "Messa",
                "Rue IDOR B",
                "Alice",
                "+237 699 00 11 22",
                "Pénicilline",
                "Diabète type 2"
        );
        patientB = patientRepository.save(patientB);

        // Visite et document appartenant au Patient B
        visitB = new VisitEntity(patientB, "VIS-IDOR-000002", "Consultation cardiologie", "CARDIOLOGIE");
        visitB.setOrganizationId(org.getId());
        visitB.setStatus("CLOTUREE");
        visitB = visitRepository.save(visitB);

        MedicalDocumentEntity docB = new MedicalDocumentEntity(
                visitB,
                "DOC-IDOR-000002",
                "target/test-classes/test-idor.pdf"
        );
        docB.setOrganizationId(org.getId());
        medicalDocumentRepository.save(docB);

        // Générer le token JWT pour Patient A uniquement
        JwtService.CreatedToken createdToken = jwtService.createPatientToken(patientA);
        tokenPatientA = createdToken.value();

        TenantContext.clear();
    }

    // -------------------------------------------------------------------------
    // CAS 1 : Accès légitime à son propre profil
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAS 1 — GET /api/patient/me avec token PATIENT valide → 200 OK")
    void givenValidPatientToken_whenGetMe_thenReturns200() throws Exception {
        mockMvc.perform(get("/api/patient/me")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // CAS 2 : Accès sans token → 401 Unauthorized
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAS 2 — GET /api/patient/me sans token → 401 Unauthorized")
    void givenNoToken_whenGetMe_thenReturns401() throws Exception {
        mockMvc.perform(get("/api/patient/me"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // CAS 3 : Tentative IDOR — Patient A essaie d'accéder au document de Patient B
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAS 3 — GET /api/patient/visits/{visitId-de-B}/document avec token de A → 403 Forbidden (IDOR bloqué)")
    void givenPatientAToken_whenAccessingPatientBDocument_thenReturns403() throws Exception {
        mockMvc.perform(get("/api/patient/visits/" + visitB.getId() + "/document")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // CAS 4 : Tentative IDOR avec visitId totalement aléatoire → 404 Not Found
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAS 4 — GET /api/patient/visits/{uuid-inconnu}/document avec token valide → 404 Not Found")
    void givenValidPatientToken_whenAccessingUnknownVisitId_thenReturns404() throws Exception {
        mockMvc.perform(get("/api/patient/visits/" + UUID.randomUUID() + "/document")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isNotFound());
    }
}
