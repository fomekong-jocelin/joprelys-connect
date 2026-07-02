package com.joprelys.backend.visit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * STORY-0603 — Tests d'intégration pour la révocation et l'annulation de documents médicaux.
 *
 * Cas couverts :
 *  1. MEDECIN révoque un document VALID → 200 + statut REVOQUE
 *  2. ADMIN_CLINIQUE annule un document VALID → 200 + statut ANNULE
 *  3. Révocation d'un document déjà révoqué → 409 CONFLICT
 *  4. Rôle non habilité (PHARMACIEN) → 403 FORBIDDEN
 *  5. Accès sans token → 401 UNAUTHORIZED
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DocumentRevocationControllerTest {

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
    private MedicalDocumentRepository medicalDocumentRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private OrganizationEntity orgA;
    private UserAccountEntity userMedecin;
    private UserAccountEntity userAdminClinique;
    private UserAccountEntity userPharmacien;

    private String tokenMedecin;
    private String tokenAdminClinique;
    private String tokenPharmacien;

    private PatientEntity patient;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = new OrganizationEntity("Clinique Test", "contact@test.local", "111", "Rue Test", "Douala");
        orgA = organizationRepository.save(orgA);

        userMedecin = new UserAccountEntity("medecin@test.local", "Dr Test", "MEDECIN", "hash");
        userMedecin.setOrganizationId(orgA.getId());
        userMedecin = userAccountRepository.save(userMedecin);

        userAdminClinique = new UserAccountEntity("admin@test.local", "Admin Clinique", "ADMIN_CLINIQUE", "hash");
        userAdminClinique.setOrganizationId(orgA.getId());
        userAdminClinique = userAccountRepository.save(userAdminClinique);

        userPharmacien = new UserAccountEntity("pharmacien@test.local", "Pharmacien Test", "PHARMACIEN", "hash");
        userPharmacien.setOrganizationId(orgA.getId());
        userPharmacien = userAccountRepository.save(userPharmacien);

        tokenMedecin       = jwtService.createToken(userMedecin).value();
        tokenAdminClinique = jwtService.createToken(userAdminClinique).value();
        tokenPharmacien    = jwtService.createToken(userPharmacien).value();

        TenantContext.setTenantId(orgA.getId());
        patient = new PatientEntity("DPU-T01", "PAT-T01", "Patient Test", "MASCULIN",
                LocalDate.of(1985, 6, 15), "+237600000001", "Douala", "", "", "", "", "", "");
        patient = patientRepository.save(patient);
        TenantContext.clear();
    }

    // --- Cas 1 : MEDECIN révoque un document VALID ---
    @Test
    void givenMedecin_whenRevokeValidDocument_thenStatusIsRevoque() throws Exception {
        MedicalDocumentEntity doc = createValidDocument("DOC-REVOKE-001");

        mockMvc.perform(patch("/api/documents/" + doc.getId() + "/revoke")
                        .header("Authorization", "Bearer " + tokenMedecin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Erreur de saisie sur le diagnostic\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOQUE"))
                .andExpect(jsonPath("$.documentNumber").value("DOC-REVOKE-001"))
                .andExpect(jsonPath("$.revocationReason").value("Erreur de saisie sur le diagnostic"))
                .andExpect(jsonPath("$.revokedAt").isNotEmpty())
                .andExpect(jsonPath("$.revokedByUserId").isNotEmpty());
    }

    // --- Cas 2 : ADMIN_CLINIQUE annule un document VALID ---
    @Test
    void givenAdminClinique_whenCancelValidDocument_thenStatusIsAnnule() throws Exception {
        MedicalDocumentEntity doc = createValidDocument("DOC-CANCEL-001");

        mockMvc.perform(patch("/api/documents/" + doc.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenAdminClinique)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Document généré en doublon par erreur système\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANNULE"))
                .andExpect(jsonPath("$.revocationReason").value("Document généré en doublon par erreur système"));
    }

    // --- Cas 3 : Double révocation → 409 CONFLICT ---
    @Test
    void givenAlreadyRevokedDocument_whenRevokeAgain_thenConflict() throws Exception {
        MedicalDocumentEntity doc = createValidDocument("DOC-DOUBLE-REVOKE");

        // Première révocation
        mockMvc.perform(patch("/api/documents/" + doc.getId() + "/revoke")
                        .header("Authorization", "Bearer " + tokenMedecin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Première révocation\"}"))
                .andExpect(status().isOk());

        // Deuxième révocation → doit échouer avec 409
        mockMvc.perform(patch("/api/documents/" + doc.getId() + "/revoke")
                        .header("Authorization", "Bearer " + tokenMedecin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Deuxième tentative\"}"))
                .andExpect(status().isConflict());
    }

    // --- Cas 4 : Rôle non habilité → 403 FORBIDDEN ---
    @Test
    void givenPharmacien_whenRevokeDocument_thenForbidden() throws Exception {
        MedicalDocumentEntity doc = createValidDocument("DOC-FORBIDDEN-001");

        mockMvc.perform(patch("/api/documents/" + doc.getId() + "/revoke")
                        .header("Authorization", "Bearer " + tokenPharmacien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Tentative non autorisée\"}"))
                .andExpect(status().isForbidden());
    }

    // --- Cas 5 : Sans token → 401 UNAUTHORIZED ---
    @Test
    void givenNoToken_whenRevokeDocument_thenUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/documents/" + UUID.randomUUID() + "/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Accès anonyme\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- Helper : crée un document VALID en base ---
    private MedicalDocumentEntity createValidDocument(String docNumber) {
        TenantContext.setTenantId(orgA.getId());
        VisitEntity visit = new VisitEntity(patient, "VIS-" + docNumber, "Motif test", "Orientation");
        visit = visitRepository.save(visit);
        MedicalDocumentEntity doc = new MedicalDocumentEntity(visit, docNumber, "/tmp/fake-path.pdf");
        doc = medicalDocumentRepository.save(doc);
        TenantContext.clear();
        return doc;
    }
}
