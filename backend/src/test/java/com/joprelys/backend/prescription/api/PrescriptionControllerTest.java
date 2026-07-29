package com.joprelys.backend.prescription.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PrescriptionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Autowired private JwtService jwtService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserAccountEntity userMedecinA;
    private UserAccountEntity userMedecinB;
    private UserAccountEntity userAgentA;
    private String tokenMedecinA;
    private String tokenMedecinB;
    private String tokenAgentA;
    private ConsultationEntity consultationA;
    private ConsultationEntity consultationB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = organizationRepository.save(new OrganizationEntity(
                "Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala"));
        orgB = organizationRepository.save(new OrganizationEntity(
                "Clinique B", "contactb@joprelys.local", "456", "Street B", "Yaoundé"));

        userMedecinA = new UserAccountEntity("medecin.a@joprelys.local", "Dr. Alpha", "MEDECIN", "passhash");
        userMedecinA.setOrganizationId(orgA.getId());
        userMedecinA = userAccountRepository.save(userMedecinA);
        userMedecinB = new UserAccountEntity("medecin.b@joprelys.local", "Dr. Beta", "MEDECIN", "passhash");
        userMedecinB.setOrganizationId(orgB.getId());
        userMedecinB = userAccountRepository.save(userMedecinB);
        userAgentA = new UserAccountEntity("agent.a@joprelys.local", "Agent A", "AGENT_ACCUEIL", "passhash");
        userAgentA.setOrganizationId(orgA.getId());
        userAgentA = userAccountRepository.save(userAgentA);

        tokenMedecinA = jwtService.createToken(userMedecinA).value();
        tokenMedecinB = jwtService.createToken(userMedecinB).value();
        tokenAgentA = jwtService.createToken(userAgentA).value();

        TenantContext.setTenantId(orgA.getId());
        PatientEntity patientA = new PatientEntity("DPU-A", "PAT-A", "Patient Alpha", "MASCULIN",
                LocalDate.of(1985, 3, 15), "+237690000000", "Douala", "", "", "", "", "", "");
        patientA = patientRepository.save(patientA);
        VisitEntity visitA = visitRepository.save(new VisitEntity(
                patientA, "VIS-A001", "Fièvre persistante", "Médecine générale"));
        consultationA = consultationRepository.save(new ConsultationEntity(
                visitA, userMedecinA, "DOC-CONS-TEST-001",
                "Symptômes test", "Examen test", "Diagnostic test", null, null));
        TenantContext.clear();

        TenantContext.setTenantId(orgB.getId());
        PatientEntity patientB = new PatientEntity("DPU-B", "PAT-B", "Patient Beta", "FEMININ",
                LocalDate.of(1990, 6, 20), "+237699000000", "Yaoundé", "", "", "", "", "", "");
        patientB = patientRepository.save(patientB);
        VisitEntity visitB = visitRepository.save(new VisitEntity(
                patientB, "VIS-B001", "Consultation pré-natale", "Gynécologie"));
        consultationB = consultationRepository.save(new ConsultationEntity(
                visitB, userMedecinB, "DOC-CONS-TEST-002",
                "Symptômes test B", "Examen test B", "Diagnostic test B", null, null));
        TenantContext.clear();
    }

    @Test
    void givenMedecinA_whenSavePrescription_thenSuccess() throws Exception {
        String json = """{"items":[{"drugName":"Amoxicilline","dosage":"500mg","posology":"3x/jour","duration":"7 jours"},{"drugName":"Ibuprofène","dosage":"400mg"}]}""";
        mockMvc.perform(post("/api/consultations/" + consultationA.getId() + "/prescription")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].drugName").value("Amoxicilline"));
    }

    @Test
    void givenExistingPrescription_whenSaveAgain_thenUpsertSuccess() throws Exception {
        String json1 = """{"items":[{"drugName":"Amoxicilline","dosage":"500mg"},{"drugName":"Ibuprofène","dosage":"400mg"}]}""";
        mockMvc.perform(post("/api/consultations/" + consultationA.getId() + "/prescription")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isOk());
        String json2 = """{"items":[{"drugName":"Paracétamol","dosage":"1g","posology":"3x/jour","duration":"5 jours"}]}""";
        mockMvc.perform(post("/api/consultations/" + consultationA.getId() + "/prescription")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON).content(json2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void givenMedecinA_whenGetPrescription_thenSuccess() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        PrescriptionEntity prescription = new PrescriptionEntity(consultationA);
        prescription.getItems().add(new PrescriptionItemEntity(
                prescription, "Amoxicilline", "500mg", "3x/jour", "7 jours", null, null, 0));
        prescriptionRepository.save(prescription);
        TenantContext.clear();
        mockMvc.perform(get("/api/consultations/" + consultationA.getId() + "/prescription")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].drugName").value("Amoxicilline"));
    }

    @Test
    void givenAgentAccueil_whenSavePrescription_thenForbidden() throws Exception {
        mockMvc.perform(post("/api/consultations/" + consultationA.getId() + "/prescription")
                        .header("Authorization", "Bearer " + tokenAgentA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"items":[{"drugName":"Amoxicilline","dosage":"500mg"}]}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenEmptyItems_whenSavePrescription_thenBadRequest() throws Exception {
        mockMvc.perform(post("/api/consultations/" + consultationA.getId() + "/prescription")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON).content("""{"items":[]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenUnknownConsultation_whenSavePrescription_thenNotFound() throws Exception {
        mockMvc.perform(post("/api/consultations/" + UUID.randomUUID() + "/prescription")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"items":[{"drugName":"Amoxicilline","dosage":"500mg"}]}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNoPrescription_whenGetPrescription_thenNoContent() throws Exception {
        mockMvc.perform(get("/api/consultations/" + consultationA.getId() + "/prescription")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenNoToken_whenSavePrescription_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/consultations/" + consultationA.getId() + "/prescription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"items":[{"drugName":"Amoxicilline","dosage":"500mg"}]}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenMedecinB_whenSavePrescriptionOnConsultationA_thenExplicitForbidden() throws Exception {
        mockMvc.perform(post("/api/consultations/" + consultationA.getId() + "/prescription")
                        .header("Authorization", "Bearer " + tokenMedecinB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"items":[{"drugName":"Tentative","dosage":"cross-tenant"}]}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CONSENT_REQUIRED"))
                .andExpect(jsonPath("$.error.required_scope").value("prescriptions"));
    }

    @Test
    void givenMedecinA_whenTransmitPrescription_thenOk() throws Exception {
        PrescriptionEntity prescription = savePrescription("TX-DOC-GOOD", "ACTIVE", "NOT_TRANSMITTED");
        mockMvc.perform(post("/api/prescriptions/" + prescription.getId() + "/transmit")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transmissionStatus").value("TRANSMITTED"))
                .andExpect(jsonPath("$.transmittedAt").isNotEmpty());
    }

    @Test
    void givenMedecinA_whenTransmitAlreadyTransmitted_thenBadRequest() throws Exception {
        PrescriptionEntity prescription = savePrescription("TX-DOC-ALREADY", "ACTIVE", "TRANSMITTED");
        mockMvc.perform(post("/api/prescriptions/" + prescription.getId() + "/transmit")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenMedecinA_whenTransmitInactive_thenBadRequest() throws Exception {
        PrescriptionEntity prescription = savePrescription("TX-DOC-INACTIVE", "EXPIRED", "NOT_TRANSMITTED");
        mockMvc.perform(post("/api/prescriptions/" + prescription.getId() + "/transmit")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAgentAccueil_whenTransmitPrescription_thenForbidden() throws Exception {
        PrescriptionEntity prescription = savePrescription("TX-DOC-FORBIDDEN", "ACTIVE", "NOT_TRANSMITTED");
        mockMvc.perform(post("/api/prescriptions/" + prescription.getId() + "/transmit")
                        .header("Authorization", "Bearer " + tokenAgentA))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenMedecinB_whenTransmitPrescriptionA_thenExplicitForbidden() throws Exception {
        PrescriptionEntity prescription = savePrescription("TX-DOC-CROSS", "ACTIVE", "NOT_TRANSMITTED");
        mockMvc.perform(post("/api/prescriptions/" + prescription.getId() + "/transmit")
                        .header("Authorization", "Bearer " + tokenMedecinB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CONSENT_REQUIRED"))
                .andExpect(jsonPath("$.error.required_scope").value("prescriptions"));
    }

    @Test
    void givenDraftPrescription_whenFinalize_thenSuccessAndGeneratesPdf() throws Exception {
        PrescriptionEntity prescription = savePrescription("TX-DOC-FINALIZE", "DRAFT", null);
        mockMvc.perform(post("/api/prescriptions/" + prescription.getId() + "/finalize")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.issuedAt").isNotEmpty())
                .andExpect(jsonPath("$.documentId").isNotEmpty());
    }

    @Test
    void givenActivePrescription_whenCancel_thenSuccess() throws Exception {
        PrescriptionEntity prescription = savePrescription("TX-DOC-CANCEL", "ACTIVE", null);
        mockMvc.perform(patch("/api/prescriptions/" + prescription.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private PrescriptionEntity savePrescription(String number, String status, String transmissionStatus) {
        TenantContext.setTenantId(orgA.getId());
        try {
            PrescriptionEntity prescription = new PrescriptionEntity(consultationA);
            prescription.setPrescriptionNumber(number);
            prescription.setStatus(status);
            if (transmissionStatus != null) prescription.setTransmissionStatus(transmissionStatus);
            return prescriptionRepository.save(prescription);
        } finally {
            TenantContext.clear();
        }
    }
}
