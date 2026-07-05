package com.joprelys.backend.patient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.joprelys.backend.audit.application.AuditService;
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
public class PatientPortalControllerTest {

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

    @Autowired
    private AuditService auditService;

    @Autowired
    private com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestRepository externalAccessRequestRepository;

    @Autowired
    private com.joprelys.backend.notification.infrastructure.persistence.NotificationRepository notificationRepository;

    @Autowired
    private com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository prescriptionRepository;

    @Autowired
    private com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository consultationRepository;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private PatientEntity patientA;
    private PatientEntity patientB;
    private VisitEntity visitA;
    private String tokenPatientA;
    private String tokenMedecinB;
    private com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity medecinBEntity;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM emergency_access_authorizations");
        jdbcTemplate.update("DELETE FROM patient_consents");
        jdbcTemplate.update("DELETE FROM dispensation_items");
        jdbcTemplate.update("DELETE FROM prescription_dispensations");
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM external_access_requests");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Créer Organisation
        orgA = new OrganizationEntity("Clinique Test A", "contact@testa.org", "123456", "Adresse A", "Douala");
        orgA = organizationRepository.save(orgA);

        orgB = new OrganizationEntity("Clinique Test B", "contact@testb.org", "654321", "Adresse B", "Yaoundé");
        orgB = organizationRepository.save(orgB);

        medecinBEntity = new com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity(
                "medecin.b@testb.org",
                "Médecin B",
                "MEDECIN",
                "passhash"
        );
        medecinBEntity.setOrganizationId(orgB.getId());
        medecinBEntity = userAccountRepository.save(medecinBEntity);

        tokenMedecinB = jwtService.createToken(medecinBEntity).value();

        TenantContext.setTenantId(orgA.getId());

        // 2. Créer Patients
        patientA = new PatientEntity(
                "PAT-20260702-000001",
                "LOC-A-001",
                "Jean Patient A",
                "MASCULIN",
                LocalDate.of(1990, 1, 1),
                "+237 699 99 99 99",
                "Douala",
                "Akwa",
                "Rue 1",
                "Marie",
                "+237 677 77 77 77",
                "Aucune",
                "Aucun"
        );
        patientA = patientRepository.save(patientA);

        patientB = new PatientEntity(
                "PAT-20260702-000002",
                "LOC-A-002",
                "Paul Patient B",
                "MASCULIN",
                LocalDate.of(1995, 5, 5),
                "+237 688 88 88 88",
                "Yaoundé",
                "Messa",
                "Rue 2",
                "Pierre",
                "+237 666 66 66 66",
                "Pénicilline",
                "Hypertension"
        );
        patientB = patientRepository.save(patientB);

        // 3. Créer une visite clôturée pour le Patient A
        visitA = new VisitEntity(patientA, "VIS-20260702-000001", "Consultation générale", "PÉDIATRIE");
        visitA.setOrganizationId(orgA.getId());
        visitA.setStatus("CLOTUREE");
        visitA = visitRepository.save(visitA);

        // 4. Créer un document médical pour le Patient A
        MedicalDocumentEntity docA = new MedicalDocumentEntity(
                visitA,
                "DOC-20260702-000001",
                "target/test-classes/test.pdf"
        );
        docA.setOrganizationId(orgA.getId());
        medicalDocumentRepository.save(docA);

        // 5. Générer le Token JWT pour Patient A
        JwtService.CreatedToken createdToken = jwtService.createPatientToken(patientA);
        tokenPatientA = createdToken.value();

        TenantContext.clear();
    }

    @Test
    void givenValidPatient_whenRequestOtp_thenOk() throws Exception {
        String json = """
                {
                    "globalPatientNumber": "%s",
                    "phone": "%s",
                    "birthDate": "%s"
                }
                """.formatted(patientA.getGlobalPatientNumber(), patientA.getPhone(), patientA.getBirthDate().toString());

        mockMvc.perform(post("/api/public/patient/auth/otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void givenInvalidPatientPhone_whenRequestOtp_thenNotFound() throws Exception {
        String json = """
                {
                    "globalPatientNumber": "%s",
                    "phone": "+33600000000",
                    "birthDate": "%s"
                }
                """.formatted(patientA.getGlobalPatientNumber(), patientA.getBirthDate().toString());

        mockMvc.perform(post("/api/public/patient/auth/otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenPatientMe_whenAuthorized_thenReturnProfile() throws Exception {
        mockMvc.perform(get("/api/patient/me")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jean Patient A"))
                .andExpect(jsonPath("$.globalPatientNumber").value(patientA.getGlobalPatientNumber()))
                .andExpect(jsonPath("$.allergies").value("Aucune"));
    }

    @Test
    void givenPatientMe_whenNoToken_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/patient/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenPatientOwnDocument_whenDownload_thenOk() throws Exception {
        java.io.File tempFile = new java.io.File("target/test-classes/test.pdf");
        if (!tempFile.exists()) {
            tempFile.getParentFile().mkdirs();
            java.nio.file.Files.write(tempFile.toPath(), new byte[]{1, 2, 3});
        }

        mockMvc.perform(get("/api/patient/visits/" + visitA.getId() + "/document")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk());
    }

    @Test
    void givenPatientOtherDocument_whenDownload_thenForbidden() throws Exception {
        // Créer une visite et un document pour le Patient B
        TenantContext.setTenantId(orgA.getId());
        VisitEntity visitB = new VisitEntity(patientB, "VIS-20260702-000002", "Consultation cardiologie", "CARDIOLOGIE");
        visitB.setOrganizationId(orgA.getId());
        visitB.setStatus("CLOTUREE");
        visitB = visitRepository.save(visitB);

        MedicalDocumentEntity docB = new MedicalDocumentEntity(
                visitB,
                "DOC-20260702-000002",
                "target/test-classes/test2.pdf"
        );
        docB.setOrganizationId(orgA.getId());
        medicalDocumentRepository.save(docB);
        TenantContext.clear();

        // Le Patient A tente de télécharger le document du Patient B
        mockMvc.perform(get("/api/patient/visits/" + visitB.getId() + "/document")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenPatient_whenGetConsents_thenOk() throws Exception {
        mockMvc.perform(get("/api/patient/consents")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationName").value("Clinique Test A"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].isCreator").value(true));
    }

    @Test
    void givenPatient_whenUpdateConsent_thenOk() throws Exception {
        mockMvc.perform(post("/api/patient/consents/" + orgA.getId())
                .param("status", "REVOKED")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/patient/consents")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REVOKED"));
    }

    @Test
    void givenClinicianWithoutConsent_whenGetPatient_thenForbidden() throws Exception {
        mockMvc.perform(get("/api/patients/" + patientA.getId())
                .header("Authorization", "Bearer " + tokenMedecinB))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenClinicianWithoutConsent_whenTriggerEmergencyAccess_thenCanAccess() throws Exception {
        mockMvc.perform(get("/api/patients/" + patientA.getId())
                .header("Authorization", "Bearer " + tokenMedecinB))
                .andExpect(status().isForbidden());

        String json = """
                {
                    "reason": "Suspicion d'arrêt cardio-respiratoire"
                }
                """;
        mockMvc.perform(post("/api/patients/" + patientA.getId() + "/emergency-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .header("Authorization", "Bearer " + tokenMedecinB))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/patients/" + patientA.getId())
                .header("Authorization", "Bearer " + tokenMedecinB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jean Patient A"))
                .andExpect(jsonPath("$.emergencyAccessActive").value(true));
    }

    @Test
    void givenPatient_whenGetAuditLogs_thenReturnsLogsList() throws Exception {
        auditService.log(
                UUID.randomUUID(),
                orgA.getId(),
                patientA.getId(),
                "PATIENT",
                patientA.getId(),
                "VIEW_PORTAL_DASHBOARD",
                "Consultation de l'espace patient",
                "127.0.0.1",
                "Mozilla/5.0",
                "SUCCESS"
        );

        mockMvc.perform(get("/api/patient/audit-logs")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("VIEW_PORTAL_DASHBOARD"))
                .andExpect(jsonPath("$[0].reason").value("Consultation de l'espace patient"))
                .andExpect(jsonPath("$[0].organizationName").value("Clinique Test A"));
    }

    @Test
    void givenPatient_whenGetAccessRequests_thenReturnsList() throws Exception {
        var request = new com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestEntity(
                patientA.getId(),
                medecinBEntity.getId(),
                orgB.getId(),
                "Consultation externe de cardiologie",
                24
        );
        externalAccessRequestRepository.save(request);

        mockMvc.perform(get("/api/patient/access-requests")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reason").value("Consultation externe de cardiologie"))
                .andExpect(jsonPath("$[0].requesterOrganizationName").value("Clinique Test B"))
                .andExpect(jsonPath("$[0].status").value("EN_ATTENTE"));
    }

    @Test
    void givenPatient_whenApproveAccessRequest_thenApprove() throws Exception {
        var request = new com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestEntity(
                patientA.getId(),
                medecinBEntity.getId(),
                orgB.getId(),
                "Consultation externe de cardiologie",
                24
        );
        request = externalAccessRequestRepository.save(request);

        mockMvc.perform(post("/api/patient/access-requests/" + request.getId() + "/approve")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROUVEE"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void givenPatient_whenRejectAccessRequest_thenReject() throws Exception {
        var request = new com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestEntity(
                patientA.getId(),
                medecinBEntity.getId(),
                orgB.getId(),
                "Consultation externe de cardiologie",
                24
        );
        request = externalAccessRequestRepository.save(request);

        mockMvc.perform(post("/api/patient/access-requests/" + request.getId() + "/reject")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUSEE"));
    }

    @Test
    void givenPatient_whenApproveOtherPatientRequest_thenForbidden() throws Exception {
        // Crée une demande d'accès pour Patient B
        var request = new com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestEntity(
                patientB.getId(),
                medecinBEntity.getId(),
                orgB.getId(),
                "Consultation externe de cardiologie pour B",
                24
        );
        request = externalAccessRequestRepository.save(request);

        // Patient A tente d'approuver la demande de Patient B
        mockMvc.perform(post("/api/patient/access-requests/" + request.getId() + "/approve")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenPatient_whenGetNotifications_thenReturnsNotificationsList() throws Exception {
        var notif = new com.joprelys.backend.notification.infrastructure.persistence.NotificationEntity(
                patientA.getId(),
                "Alerte test",
                "Ceci est un message de test",
                "INFO"
        );
        notificationRepository.save(notif);

        mockMvc.perform(get("/api/patient/notifications")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Alerte test"))
                .andExpect(jsonPath("$[0].status").value("NON_LU"));
    }

    @Test
    void givenPatient_whenMarkNotificationAsRead_thenMarkedAsRead() throws Exception {
        var notif = new com.joprelys.backend.notification.infrastructure.persistence.NotificationEntity(
                patientA.getId(),
                "Alerte test",
                "Ceci est un message de test",
                "INFO"
        );
        notif = notificationRepository.save(notif);

        mockMvc.perform(post("/api/patient/notifications/" + notif.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LU"));
    }

    @Test
    void givenPatient_whenMarkAllNotificationsAsRead_thenAllMarked() throws Exception {
        var notif1 = new com.joprelys.backend.notification.infrastructure.persistence.NotificationEntity(
                patientA.getId(),
                "Alerte test 1",
                "Ceci est un message de test 1",
                "INFO"
        );
        notificationRepository.save(notif1);

        mockMvc.perform(post("/api/patient/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/patient/notifications")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("LU"));
    }

    @Test
    void givenPatient_whenMarkOtherPatientNotification_thenForbidden() throws Exception {
        // Crée une notification pour Patient B
        var notif = new com.joprelys.backend.notification.infrastructure.persistence.NotificationEntity(
                patientB.getId(),
                "Alerte B",
                "Message pour B",
                "INFO"
        );
        notif = notificationRepository.save(notif);

        // Patient A tente de marquer la notification de Patient B comme lue
        mockMvc.perform(post("/api/patient/notifications/" + notif.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenPatientPrescription_whenTransmit_thenOk() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        var consultation = consultationRepository.save(new com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity(
                visitA, medecinBEntity, "DOC-TX-1", "symptoms", "exam", "diag", null, null));
        var prescription = new com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity(consultation);
        prescription.setPrescriptionNumber("TX-GOOD");
        prescription.setStatus("ACTIVE");
        prescription.setTransmissionStatus("NOT_TRANSMITTED");
        prescription = prescriptionRepository.save(prescription);
        TenantContext.clear();

        mockMvc.perform(post("/api/patient/me/prescriptions/" + prescription.getId() + "/transmit")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transmissionStatus").value("TRANSMITTED"))
                .andExpect(jsonPath("$.transmittedAt").isNotEmpty());
    }

    @Test
    void givenOtherPatientPrescription_whenTransmit_thenForbidden() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        // Visite et consultation pour patient B
        VisitEntity visitB = new VisitEntity(patientB, "VIS-20260702-9999", "Consultation cardiologie", "CARDIOLOGIE");
        visitB.setOrganizationId(orgA.getId());
        visitB.setStatus("CLOTUREE");
        visitB = visitRepository.save(visitB);
        var consultation = consultationRepository.save(new com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity(
                visitB, medecinBEntity, "DOC-TX-2", "symptoms", "exam", "diag", null, null));
        var prescription = new com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity(consultation);
        prescription.setPrescriptionNumber("TX-OTHER");
        prescription.setStatus("ACTIVE");
        prescription.setTransmissionStatus("NOT_TRANSMITTED");
        prescription = prescriptionRepository.save(prescription);
        TenantContext.clear();

        // Le Patient A tente de transmettre l'ordonnance du Patient B
        mockMvc.perform(post("/api/patient/me/prescriptions/" + prescription.getId() + "/transmit")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAlreadyTransmittedPrescription_whenTransmit_thenBadRequest() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        var consultation = consultationRepository.save(new com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity(
                visitA, medecinBEntity, "DOC-TX-3", "symptoms", "exam", "diag", null, null));
        var prescription = new com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity(consultation);
        prescription.setPrescriptionNumber("TX-ALREADY");
        prescription.setStatus("ACTIVE");
        prescription.setTransmissionStatus("TRANSMITTED");
        prescription = prescriptionRepository.save(prescription);
        TenantContext.clear();

        mockMvc.perform(post("/api/patient/me/prescriptions/" + prescription.getId() + "/transmit")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenInactivePrescription_whenTransmit_thenBadRequest() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        var consultation = consultationRepository.save(new com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity(
                visitA, medecinBEntity, "DOC-TX-4", "symptoms", "exam", "diag", null, null));
        var prescription = new com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity(consultation);
        prescription.setPrescriptionNumber("TX-INACTIVE");
        prescription.setStatus("EXPIRED");
        prescription.setTransmissionStatus("NOT_TRANSMITTED");
        prescription = prescriptionRepository.save(prescription);
        TenantContext.clear();

        mockMvc.perform(post("/api/patient/me/prescriptions/" + prescription.getId() + "/transmit")
                        .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isBadRequest());
    }
}
