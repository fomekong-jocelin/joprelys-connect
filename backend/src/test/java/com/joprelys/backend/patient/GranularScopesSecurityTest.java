package com.joprelys.backend.patient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientConsentEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientConsentRepository;
import com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestEntity;
import com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("STORY-1302 — Granular Scopes and Consent Verification")
public class GranularScopesSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientConsentRepository patientConsentRepository;

    @Autowired
    private ExternalAccessRequestRepository externalAccessRequestRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository userAccountRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private PatientEntity patientB;
    private VisitEntity visitB;
    private ConsultationEntity consultationB;
    private PrescriptionEntity prescriptionB;

    private com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity doctorA;

    private String tokenDoctorA;
    private String tokenPatientB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM emergency_access_authorizations");
        jdbcTemplate.update("DELETE FROM patient_consents");
        jdbcTemplate.update("DELETE FROM external_access_requests");
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create orgs
        orgA = new OrganizationEntity("Clinique A", "contact@clinique-a.org", "CLIN-A", "Adresse A", "Douala");
        orgA = organizationRepository.save(orgA);

        orgB = new OrganizationEntity("Clinique B", "contact@clinique-b.org", "CLIN-B", "Adresse B", "Yaoundé");
        orgB = organizationRepository.save(orgB);

        // 2. Doctor A from Org A
        doctorA = 
            new com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity(
                "dupont@clinique-a.org", "Dr. Dupont", "MEDECIN", "password"
            );
        doctorA.setOrganizationId(orgA.getId());
        doctorA = userAccountRepository.save(doctorA);

        // 3. Patient B belongs to Org B
        TenantContext.setTenantId(orgB.getId());
        patientB = new PatientEntity(
                "PAT-000001", "LOC-001", "Bob Patient", "MASCULIN", LocalDate.of(1990, 1, 1),
                "+237 600 000 000", "Yaoundé", "Messa", "Rue B", "Alice", "+237 699 000 000",
                "Pénicilline", "Aucun"
        );
        patientB.setOrganizationId(orgB.getId());
        patientB = patientRepository.save(patientB);

        visitB = new VisitEntity(patientB, "VIS-000001", "Consultation", "GENERAL");
        visitB.setOrganizationId(orgB.getId());
        visitB.setStatus("EN_COURS");
        visitB = visitRepository.save(visitB);

        consultationB = new ConsultationEntity(visitB, doctorA, "DOC-CONS-123", "symptoms", "exam", "diagnosis", "advice", "followup");
        consultationB = consultationRepository.save(consultationB);

        prescriptionB = new PrescriptionEntity(consultationB);
        prescriptionB.setPrescriptionNumber("TX-DOC-GOOD");
        prescriptionB.setPinCode("1234");
        prescriptionB.setStatus("ACTIVE");
        prescriptionB = prescriptionRepository.save(prescriptionB);

        TenantContext.clear();

        // 4. Generate Tokens
        tokenDoctorA = jwtService.createToken(doctorA).value();
        tokenPatientB = jwtService.createPatientToken(patientB).value();
    }

    @Test
    @DisplayName("GET /api/visits/{id}/consultation: fails with 403 when no consent exists")
    void getConsultation_noConsent_fails() throws Exception {
        mockMvc.perform(get("/api/visits/" + visitB.getId() + "/consultation")
                .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/visits/{id}/consultation: fails with 403 when scope medical_records is missing")
    void getConsultation_consentWithoutScope_fails() throws Exception {
        // Create consent for Org A but without medical_records scope
        PatientConsentEntity consent = new PatientConsentEntity(patientB.getId(), orgA.getId(), "ACTIVE");
        consent.setScopes("prescriptions,lab_results");
        patientConsentRepository.save(consent);

        mockMvc.perform(get("/api/visits/" + visitB.getId() + "/consultation")
                .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/visits/{id}/consultation: succeeds when scope medical_records is present")
    void getConsultation_consentWithScope_succeeds() throws Exception {
        PatientConsentEntity consent = new PatientConsentEntity(patientB.getId(), orgA.getId(), "ACTIVE");
        consent.setScopes("medical_records,prescriptions");
        patientConsentRepository.save(consent);

        mockMvc.perform(get("/api/visits/" + visitB.getId() + "/consultation")
                .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/consultations/{id}/prescription: fails when prescriptions scope is missing")
    void getPrescription_consentWithoutScope_fails() throws Exception {
        PatientConsentEntity consent = new PatientConsentEntity(patientB.getId(), orgA.getId(), "ACTIVE");
        consent.setScopes("medical_records,lab_results");
        patientConsentRepository.save(consent);

        mockMvc.perform(get("/api/consultations/" + consultationB.getId() + "/prescription")
                .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/consultations/{id}/prescription: succeeds when prescriptions scope is present")
    void getPrescription_consentWithScope_succeeds() throws Exception {
        PatientConsentEntity consent = new PatientConsentEntity(patientB.getId(), orgA.getId(), "ACTIVE");
        consent.setScopes("prescriptions");
        patientConsentRepository.save(consent);

        mockMvc.perform(get("/api/consultations/" + consultationB.getId() + "/prescription")
                .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/allergies: fails when allergies_history scope is missing")
    void getAllergies_consentWithoutScope_fails() throws Exception {
        PatientConsentEntity consent = new PatientConsentEntity(patientB.getId(), orgA.getId(), "ACTIVE");
        consent.setScopes("medical_records,prescriptions");
        patientConsentRepository.save(consent);

        mockMvc.perform(get("/api/patients/" + patientB.getId() + "/allergies")
                .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/allergies: succeeds when allergies_history scope is present")
    void getAllergies_consentWithScope_succeeds() throws Exception {
        PatientConsentEntity consent = new PatientConsentEntity(patientB.getId(), orgA.getId(), "ACTIVE");
        consent.setScopes("allergies_history");
        patientConsentRepository.save(consent);

        mockMvc.perform(get("/api/patients/" + patientB.getId() + "/allergies")
                .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/visits/{id}/consultation: succeeds via approved external access request with medical_records scope")
    void getConsultation_externalAccessWithScope_succeeds() throws Exception {
        ExternalAccessRequestEntity request = new ExternalAccessRequestEntity(
                patientB.getId(), doctorA.getId(), orgA.getId(), "Accès clinique externe", 12
        );
        request.setStatus("APPROUVEE");
        request.setExpiresAt(Instant.now().plus(java.time.Duration.ofHours(12)));
        request.setScopes("medical_records");
        externalAccessRequestRepository.save(request);

        mockMvc.perform(get("/api/visits/" + visitB.getId() + "/consultation")
                .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/visits/{id}/consultation: fails via approved external access request without medical_records scope")
    void getConsultation_externalAccessWithoutScope_fails() throws Exception {
        ExternalAccessRequestEntity request = new ExternalAccessRequestEntity(
                patientB.getId(), doctorA.getId(), orgA.getId(), "Accès clinique externe", 12
        );
        request.setStatus("APPROUVEE");
        request.setExpiresAt(Instant.now().plus(java.time.Duration.ofHours(12)));
        request.setScopes("prescriptions");
        externalAccessRequestRepository.save(request);

        mockMvc.perform(get("/api/visits/" + visitB.getId() + "/consultation")
                .header("Authorization", "Bearer " + tokenDoctorA))
                .andExpect(status().isForbidden());
    }
}
