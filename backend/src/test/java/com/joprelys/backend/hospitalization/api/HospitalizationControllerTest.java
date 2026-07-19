package com.joprelys.backend.hospitalization.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.spatial.domain.HospitalServiceType;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.WardRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HospitalizationControllerTest {

    private static final String PEDIATRICS_SERVICE = "PÉDIATRIE";
    private static final String PEDIATRICS_ROOM = "101";
    private static final String PEDIATRICS_BED = "Lit A";
    private static final String SURGERY_SERVICE = "CHIRURGIE";
    private static final String SURGERY_ROOM = "202";
    private static final String SURGERY_BED = "Lit X";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private HospitalizationRepository hospitalizationRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private OrganizationEntity organization;
    private UserAccountEntity doctor;
    private PatientEntity patientA;
    private PatientEntity patientB;
    private String doctorToken;
    private String billingAgentToken;

    @BeforeEach
    void setUp() {
        clearData();

        organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Hospitalisation",
                "hosp@joprelys.local",
                "123456",
                "Street",
                "Douala"));

        TenantContext.setTenantId(organization.getId());
        try {
            doctor = new UserAccountEntity("dr.hosp@joprelys.local", "Dr. House", "MEDECIN", "passhash");
            doctor.setOrganizationId(organization.getId());
            doctor = userAccountRepository.save(doctor);
            doctorToken = jwtService.createToken(doctor).value();

            UserAccountEntity billingAgent = new UserAccountEntity(
                    "billing.hosp@joprelys.local",
                    "Agent facturation",
                    "AGENT_ACCUEIL",
                    "passhash");
            billingAgent.setOrganizationId(organization.getId());
            billingAgent = userAccountRepository.save(billingAgent);
            billingAgentToken = jwtService.createToken(billingAgent).value();

            patientA = patientRepository.save(new PatientEntity(
                    "DPU-H-00001", "PAT-H-001", "Alice Hospitalisée", "FEMININ",
                    LocalDate.of(1990, 5, 10), "+237699999991", "Douala", "Akwa", "Street A",
                    "Bob", "+237699445566", "Aucune", "Aucun"));
            patientB = patientRepository.save(new PatientEntity(
                    "DPU-H-00002", "PAT-H-002", "Bob Hospitalisé", "MASCULIN",
                    LocalDate.of(1985, 7, 20), "+237699999992", "Douala", "Akwa", "Street B",
                    "Alice", "+237699445577", "Aucune", "Aucun"));

            createConfiguredBed(PEDIATRICS_SERVICE, PEDIATRICS_ROOM, PEDIATRICS_BED);
            createConfiguredBed(SURGERY_SERVICE, SURGERY_ROOM, SURGERY_BED);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void givenDoctor_whenAdmitAndDischargePatient_thenSuccess() throws Exception {
        VisitEntity visitA = createVisit(patientA, "VIS-H-001", "Motif de visite A", "MÉDECINE GÉNÉRALE");
        VisitEntity visitB = createVisit(patientB, "VIS-H-002", "Motif de visite B", "MÉDECINE GÉNÉRALE");

        String hospitalizationId = admit(
                patientA, visitA, PEDIATRICS_SERVICE, PEDIATRICS_ROOM, PEDIATRICS_BED,
                "Surveillance post-opératoire");

        mockMvc.perform(post("/api/hospitalizations")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(admissionPayload(
                                patientB, visitB, PEDIATRICS_SERVICE, PEDIATRICS_ROOM, PEDIATRICS_BED,
                                "Fièvre élevée")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/hospitalizations/{id}/notes", hospitalizationId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"noteContent":"Température stable à 37.2°C, réveil calme."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.noteContent").value("Température stable à 37.2°C, réveil calme."))
                .andExpect(jsonPath("$.authorName").value("Dr. House"));

        mockMvc.perform(get("/api/hospitalizations/{id}/notes", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(post("/api/hospitalizations/{id}/discharge", hospitalizationId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dischargeDiagnosis":"Guérison complète, ablation des fils OK.",
                                  "dischargeInstructions":"Repos de 5 jours, paracétamol en cas de douleur."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SORTI"));

        mockMvc.perform(get("/api/hospitalizations/{id}/pdf", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.startsWith(MediaType.APPLICATION_PDF_VALUE);
                });
    }

    @Test
    void givenDoctor_whenDownloadEntryPdf_thenSuccess() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-ENT-01", "Admission", "MÉDECINE GÉNÉRALE");
        String hospitalizationId = admit(
                patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_ROOM, PEDIATRICS_BED, "Surveillance");

        mockMvc.perform(get("/api/hospitalizations/{id}/entry-pdf", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.startsWith(MediaType.APPLICATION_PDF_VALUE);
                });
    }

    @Test
    void givenDoctor_whenDischargeAgainstMedicalAdvice_thenStatusIsSortiContreAvis() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-CAD-02", "Admission", "MÉDECINE GÉNÉRALE");
        String hospitalizationId = admit(
                patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_ROOM, PEDIATRICS_BED, "Surveillance");

        mockMvc.perform(post("/api/hospitalizations/{id}/discharge", hospitalizationId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dischargeDiagnosis":"Refus de soins.",
                                  "dischargeInstructions":"Contre avis médical.",
                                  "againstMedicalAdvice":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SORTI_CONTRE_AVIS"));

        mockMvc.perform(get("/api/hospitalizations/{id}/pdf", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk());
    }

    @Test
    void givenDoctor_whenAddAndGetSurgicalConsents_thenSuccess() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-CS-03", "Admission", "MÉDECINE GÉNÉRALE");
        String hospitalizationId = admit(
                patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_ROOM, PEDIATRICS_BED, "Surveillance");

        mockMvc.perform(multipart("/api/hospitalizations/{id}/consents", hospitalizationId)
                        .param("consentType", "ANESTHESIA")
                        .param("patientSignaturePresent", "true")
                        .param("witnessName", "Jean Dupont")
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentType").value("ANESTHESIA"))
                .andExpect(jsonPath("$.patientSignaturePresent").value(true));

        MockMultipartFile file = new MockMultipartFile(
                "file", "consent.pdf", MediaType.APPLICATION_PDF_VALUE,
                "fake-pdf".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/hospitalizations/{id}/consents", hospitalizationId)
                        .file(file)
                        .param("consentType", "SURGERY")
                        .param("patientSignaturePresent", "false")
                        .param("witnessName", "")
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").isNotEmpty());

        mockMvc.perform(get("/api/hospitalizations/{id}/consents", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void givenDoctor_whenAddAndGetDailyCareMedsAndConsumptions_thenSuccess() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-CARE-01", "Soins", "MÉDECINE GÉNÉRALE");
        String hospitalizationId = admit(
                patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_ROOM, PEDIATRICS_BED,
                "Surveillance soins");

        mockMvc.perform(post("/api/hospitalizations/{id}/daily-cares", hospitalizationId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "careType":"PANSEMENT",
                                  "description":"Pansement abdominal refait",
                                  "billable":true,
                                  "price":4500.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.careType").value("PANSEMENT"));

        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", hospitalizationId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicationName":"Paracétamol Injectable",
                                  "dose":"1g IV",
                                  "prescriptionItemId":null
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/hospitalizations/{id}/patient-consumptions", hospitalizationId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemName":"Seringue 5ml","quantity":3,"unitPrice":250.0}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/hospitalizations/{id}/daily-cares", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/hospitalizations/{id}/medication-administrations", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/hospitalizations/{id}/patient-consumptions", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(post("/api/invoices/precalculate")
                        .param("patientId", patientA.getId().toString())
                        .param("visitId", visit.getId().toString())
                        .header("Authorization", bearer(billingAgentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.itemType == 'AMI_CARE')].unitPrice").value(4500.0))
                .andExpect(jsonPath("$.items[?(@.label == 'Consommation : Seringue 5ml')].quantity").value(3.0));
    }

    @Test
    void givenDoctor_whenCreateAndValidateOperatingReport_thenSuccessAndInvoiceCalculated() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-OR-01", "Soins", "CHIRURGIE");
        String hospitalizationId = admit(
                patientA, visit, SURGERY_SERVICE, SURGERY_ROOM, SURGERY_BED,
                "Chirurgie programmée");

        String reportResponse = mockMvc.perform(post("/api/hospitalizations/{id}/operating-reports", hospitalizationId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "procedureName":"Appendicectomie",
                                  "procedureDescription":"Incision McBurney, ablation de l'appendice.",
                                  "preOperativeDiagnosis":"Appendicite aiguë",
                                  "postOperativeDiagnosis":"Appendicite phlegmoneuse",
                                  "anesthesiaType":"GÉNÉRALE",
                                  "anesthesiaDescription":"AG avec intubation",
                                  "kSurgeonValue":50.0,
                                  "kAnesthesistValue":20.0,
                                  "kBlocValue":30.0,
                                  "implants":[{
                                    "implantName":"Fil de suture résorbable",
                                    "lotNumber":"LOT12345",
                                    "quantity":2,
                                    "unitPrice":1200.0,
                                    "manufacturer":"Ethicon"
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validated").value(false))
                .andReturn().getResponse().getContentAsString();
        String reportId = objectMapper.readTree(reportResponse).get("id").asText();

        mockMvc.perform(post("/api/invoices/precalculate")
                        .param("patientId", patientA.getId().toString())
                        .param("visitId", visit.getId().toString())
                        .header("Authorization", bearer(billingAgentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.itemType == 'K_SURGEON')]").isEmpty());

        mockMvc.perform(post("/api/hospitalizations/operating-reports/{id}/validate", reportId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validated").value(true));

        mockMvc.perform(post("/api/invoices/precalculate")
                        .param("patientId", patientA.getId().toString())
                        .param("visitId", visit.getId().toString())
                        .header("Authorization", bearer(billingAgentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.itemType == 'K_SURGEON')].unitPrice").value(50000.0))
                .andExpect(jsonPath("$.items[?(@.itemType == 'K_ANESTHESIST')].unitPrice").value(20000.0))
                .andExpect(jsonPath("$.items[?(@.itemType == 'K_BLOC')].unitPrice").value(30000.0))
                .andExpect(jsonPath("$.items[?(@.label == 'Implant : Fil de suture résorbable (Lot: LOT12345)')].quantity").value(2.0));
    }

    private String admit(
            PatientEntity patient,
            VisitEntity visit,
            String serviceName,
            String roomNumber,
            String bedNumber,
            String reason) throws Exception {
        String response = mockMvc.perform(post("/api/hospitalizations")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(admissionPayload(patient, visit, serviceName, roomNumber, bedNumber, reason)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andExpect(jsonPath("$.serviceName").value(serviceName))
                .andExpect(jsonPath("$.roomNumber").value(roomNumber))
                .andExpect(jsonPath("$.bedNumber").value(bedNumber))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String admissionPayload(
            PatientEntity patient,
            VisitEntity visit,
            String serviceName,
            String roomNumber,
            String bedNumber,
            String reason) {
        return """
                {
                  "patientId":"%s",
                  "serviceName":"%s",
                  "roomNumber":"%s",
                  "bedNumber":"%s",
                  "admissionReason":"%s",
                  "visitId":"%s",
                  "responsiblePractitionerId":"%s"
                }
                """.formatted(
                patient.getId(), serviceName, roomNumber, bedNumber, reason, visit.getId(), doctor.getId());
    }

    private VisitEntity createVisit(PatientEntity patient, String number, String reason, String specialty) {
        TenantContext.setTenantId(organization.getId());
        try {
            return visitRepository.save(new VisitEntity(
                    patient,
                    number,
                    reason,
                    specialty,
                    specialty,
                    doctor.getId(),
                    Instant.now()));
        } finally {
            TenantContext.clear();
        }
    }

    private void createConfiguredBed(String serviceName, String roomNumber, String bedNumber) {
        WardEntity ward = new WardEntity(serviceName, HospitalServiceType.HOSPITALIZATION);
        ward.setOrganizationId(organization.getId());
        ward = wardRepository.save(ward);

        RoomEntity room = new RoomEntity(ward, roomNumber, 1, "STANDARD");
        room.setOrganizationId(organization.getId());
        room = roomRepository.save(room);

        BedEntity bed = new BedEntity(room, bedNumber);
        bed.setOrganizationId(organization.getId());
        bedRepository.save(bed);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void clearData() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM dispensation_items");
        jdbcTemplate.update("DELETE FROM prescription_dispensations");
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM vitals");
        jdbcTemplate.update("DELETE FROM surgical_consents");
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM lab_results");
        jdbcTemplate.update("DELETE FROM lab_orders");
        jdbcTemplate.update("DELETE FROM surgical_implants");
        jdbcTemplate.update("DELETE FROM operating_reports");
        jdbcTemplate.update("DELETE FROM patient_consumptions");
        jdbcTemplate.update("DELETE FROM medication_administrations");
        jdbcTemplate.update("DELETE FROM hospitalization_daily_cares");
        jdbcTemplate.update("DELETE FROM hospitalization_notes");
        jdbcTemplate.update("DELETE FROM bed_assignments");
        jdbcTemplate.update("DELETE FROM hospitalizations");
        jdbcTemplate.update("DELETE FROM beds");
        jdbcTemplate.update("DELETE FROM rooms");
        jdbcTemplate.update("DELETE FROM wards");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patient_allergies");
        jdbcTemplate.update("DELETE FROM patient_medical_history");
        jdbcTemplate.update("DELETE FROM external_access_requests");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();
    }
}
