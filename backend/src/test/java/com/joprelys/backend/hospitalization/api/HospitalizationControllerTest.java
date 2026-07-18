package com.joprelys.backend.hospitalization.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.mock.web.MockMultipartFile;

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
    private com.joprelys.backend.visit.infrastructure.persistence.VisitRepository visitRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private OrganizationEntity org;
    private UserAccountEntity doctor;
    private UserAccountEntity billingAgent;
    private PatientEntity patientA;
    private PatientEntity patientB;
    private String tokenDoctor;
    private String tokenBillingAgent;

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
        jdbcTemplate.update("DELETE FROM surgical_implants");
        jdbcTemplate.update("DELETE FROM operating_reports");
        jdbcTemplate.update("DELETE FROM patient_consumptions");
        jdbcTemplate.update("DELETE FROM medication_administrations");
        jdbcTemplate.update("DELETE FROM hospitalization_daily_cares");
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

        billingAgent = new UserAccountEntity(
                "billing.hosp@joprelys.local",
                "Agent facturation",
                "AGENT_ACCUEIL",
                "passhash");
        billingAgent.setOrganizationId(org.getId());
        billingAgent = userAccountRepository.save(billingAgent);
        tokenBillingAgent = jwtService.createToken(billingAgent).value();

        // Create Patients
        patientA = new PatientEntity("DPU-H-00001", "PAT-H-001", "Alice Hospitalisée", "FEMININ", LocalDate.of(1990, 5, 10), "+237699999991", "Douala", "Akwa", "Street A", "Bob", "+237699445566", "Aucune", "Aucun");
        patientB = new PatientEntity("DPU-H-00002", "PAT-H-002", "Bob Hospitalisé", "MASCULIN", LocalDate.of(1985, 7, 20), "+237699999992", "Douala", "Akwa", "Street B", "Alice", "+237699445577", "Aucune", "Aucun");

        patientA = patientRepository.save(patientA);
        patientB = patientRepository.save(patientB);

        TenantContext.clear();
    }

    @Test
    void givenDoctor_whenAdmitAndDischargePatient_thenSuccess() throws Exception {
        TenantContext.setTenantId(org.getId());
        
        // Créer les visites de test
        com.joprelys.backend.visit.infrastructure.persistence.VisitEntity visitA = new com.joprelys.backend.visit.infrastructure.persistence.VisitEntity(
                patientA,
                "VIS-H-001",
                "Motif de visite A",
                "Médecine générale",
                "MÉDECINE GÉNÉRALE",
                doctor.getId(),
                java.time.Instant.now()
        );
        visitA = visitRepository.save(visitA);

        com.joprelys.backend.visit.infrastructure.persistence.VisitEntity visitB = new com.joprelys.backend.visit.infrastructure.persistence.VisitEntity(
                patientB,
                "VIS-H-002",
                "Motif de visite B",
                "Médecine générale",
                "MÉDECINE GÉNÉRALE",
                doctor.getId(),
                java.time.Instant.now()
        );
        visitB = visitRepository.save(visitB);

        TenantContext.clear();

        // 1. Admit Patient A in Room 101, Bed A
        String admitRequest = String.format("""
                {
                    "patientId": "%s",
                    "serviceName": "PÉDIATRIE",
                    "roomNumber": "101",
                    "bedNumber": "Lit A",
                    "admissionReason": "Surveillance post-opératoire",
                    "visitId": "%s",
                    "responsiblePractitionerId": "%s"
                }
                """, patientA.getId(), visitA.getId(), doctor.getId());

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
                    "admissionReason": "Fièvre élevée",
                    "visitId": "%s",
                    "responsiblePractitionerId": "%s"
                }
                """, patientB.getId(), visitB.getId(), doctor.getId());

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

    @Test
    void givenDoctor_whenDownloadEntryPdf_thenSuccess() throws Exception {
        TenantContext.setTenantId(org.getId());
        
        com.joprelys.backend.visit.infrastructure.persistence.VisitEntity visit = new com.joprelys.backend.visit.infrastructure.persistence.VisitEntity(
                patientA, "VIS-ENT-01", "Admission", "Pédiatrie", "MÉDECINE GÉNÉRALE", doctor.getId(), java.time.Instant.now()
        );
        visit = visitRepository.save(visit);
        TenantContext.clear();

        String admitRequest = String.format("""
                {
                    "patientId": "%s",
                    "serviceName": "PÉDIATRIE",
                    "roomNumber": "101",
                    "bedNumber": "Lit A",
                    "admissionReason": "Surveillance",
                    "visitId": "%s",
                    "responsiblePractitionerId": "%s"
                }
                """, patientA.getId(), visit.getId(), doctor.getId());

        String responseStr = mockMvc.perform(post("/api/hospitalizations")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(admitRequest))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String hospId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor()
                .newInstance().readTree(responseStr).get("id").asText();

        mockMvc.perform(get("/api/hospitalizations/" + hospId + "/entry-pdf")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.startsWith("application/pdf");
                });
    }

    @Test
    void givenDoctor_whenDischargeAgainstMedicalAdvice_thenStatusIsSortiContreAvis() throws Exception {
        TenantContext.setTenantId(org.getId());
        
        com.joprelys.backend.visit.infrastructure.persistence.VisitEntity visit = new com.joprelys.backend.visit.infrastructure.persistence.VisitEntity(
                patientA, "VIS-CAD-02", "Admission", "Pédiatrie", "MÉDECINE GÉNÉRALE", doctor.getId(), java.time.Instant.now()
        );
        visit = visitRepository.save(visit);
        TenantContext.clear();

        String admitRequest = String.format("""
                {
                    "patientId": "%s",
                    "serviceName": "PÉDIATRIE",
                    "roomNumber": "101",
                    "bedNumber": "Lit A",
                    "admissionReason": "Surveillance",
                    "visitId": "%s",
                    "responsiblePractitionerId": "%s"
                }
                """, patientA.getId(), visit.getId(), doctor.getId());

        String responseStr = mockMvc.perform(post("/api/hospitalizations")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(admitRequest))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String hospId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor()
                .newInstance().readTree(responseStr).get("id").asText();

        String dischargeRequest = """
                {
                    "dischargeDiagnosis": "Refus de soins.",
                    "dischargeInstructions": "Contre avis médical.",
                    "againstMedicalAdvice": true
                }
                """;

        mockMvc.perform(post("/api/hospitalizations/" + hospId + "/discharge")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(dischargeRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SORTI_CONTRE_AVIS"));

        // Download discharge PDF (which should have updated title)
        mockMvc.perform(get("/api/hospitalizations/" + hospId + "/pdf")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.startsWith("application/pdf");
                });
    }

    @Test
    void givenDoctor_whenAddAndGetSurgicalConsents_thenSuccess() throws Exception {
        TenantContext.setTenantId(org.getId());
        
        com.joprelys.backend.visit.infrastructure.persistence.VisitEntity visit = new com.joprelys.backend.visit.infrastructure.persistence.VisitEntity(
                patientA, "VIS-CS-03", "Admission", "Pédiatrie", "MÉDECINE GÉNÉRALE", doctor.getId(), java.time.Instant.now()
        );
        visit = visitRepository.save(visit);
        TenantContext.clear();

        String admitRequest = String.format("""
                {
                    "patientId": "%s",
                    "serviceName": "PÉDIATRIE",
                    "roomNumber": "101",
                    "bedNumber": "Lit A",
                    "admissionReason": "Surveillance",
                    "visitId": "%s",
                    "responsiblePractitionerId": "%s"
                }
                """, patientA.getId(), visit.getId(), doctor.getId());

        String responseStr = mockMvc.perform(post("/api/hospitalizations")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(admitRequest))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String hospId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor()
                .newInstance().readTree(responseStr).get("id").asText();

        // 1. Post consent without file
        mockMvc.perform(multipart("/api/hospitalizations/" + hospId + "/consents")
                .param("consentType", "ANESTHESIA")
                .param("patientSignaturePresent", "true")
                .param("witnessName", "Jean Dupont")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentType").value("ANESTHESIA"))
                .andExpect(jsonPath("$.patientSignaturePresent").value(true))
                .andExpect(jsonPath("$.witnessName").value("Jean Dupont"));

        // 2. Post consent with file
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "consent.pdf", MediaType.APPLICATION_PDF_VALUE, "fake-pdf".getBytes()
        );
        mockMvc.perform(multipart("/api/hospitalizations/" + hospId + "/consents")
                .file(mockFile)
                .param("consentType", "SURGERY")
                .param("patientSignaturePresent", "false")
                .param("witnessName", "")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentType").value("SURGERY"))
                .andExpect(jsonPath("$.patientSignaturePresent").value(false))
                .andExpect(jsonPath("$.documentId").isNotEmpty());

        // 3. Get consents
        mockMvc.perform(get("/api/hospitalizations/" + hospId + "/consents")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].consentType").value("ANESTHESIA"))
                .andExpect(jsonPath("$[1].consentType").value("SURGERY"));
    }

    @Test
    void givenDoctor_whenAddAndGetDailyCareMedsAndConsumptions_thenSuccess() throws Exception {
        TenantContext.setTenantId(org.getId());
        
        com.joprelys.backend.visit.infrastructure.persistence.VisitEntity visit = new com.joprelys.backend.visit.infrastructure.persistence.VisitEntity(
                patientA, "VIS-CARE-01", "Soins", "Pédiatrie", "MÉDECINE GÉNÉRALE", doctor.getId(), java.time.Instant.now()
        );
        visit = visitRepository.save(visit);
        TenantContext.clear();

        String admitRequest = String.format("""
                {
                    "patientId": "%s",
                    "serviceName": "PÉDIATRIE",
                    "roomNumber": "101",
                    "bedNumber": "Lit A",
                    "admissionReason": "Surveillance soins",
                    "visitId": "%s",
                    "responsiblePractitionerId": "%s"
                }
                """, patientA.getId(), visit.getId(), doctor.getId());

        String responseStr = mockMvc.perform(post("/api/hospitalizations")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(admitRequest))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String hospId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor()
                .newInstance().readTree(responseStr).get("id").asText();

        // 1. Post daily care (billable)
        String careRequest = """
                {
                    "careType": "PANSEMENT",
                    "description": "Pansement abdominal refait",
                    "billable": true,
                    "price": 4500.0
                }
                """;
        mockMvc.perform(post("/api/hospitalizations/" + hospId + "/daily-cares")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(careRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.careType").value("PANSEMENT"))
                .andExpect(jsonPath("$.billable").value(true))
                .andExpect(jsonPath("$.price").value(4500.0));

        // 2. Get daily cares
        mockMvc.perform(get("/api/hospitalizations/" + hospId + "/daily-cares")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].careType").value("PANSEMENT"));

        // 3. Post medication administration
        String medRequest = """
                {
                    "medicationName": "Paracétamol Injectable",
                    "dose": "1g IV",
                    "prescriptionItemId": null
                }
                """;
        mockMvc.perform(post("/api/hospitalizations/" + hospId + "/medication-administrations")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(medRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicationName").value("Paracétamol Injectable"))
                .andExpect(jsonPath("$.dose").value("1g IV"));

        // 4. Get medication administrations
        mockMvc.perform(get("/api/hospitalizations/" + hospId + "/medication-administrations")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].medicationName").value("Paracétamol Injectable"));

        // 5. Post patient consumption
        String consRequest = """
                {
                    "itemName": "Seringue 5ml",
                    "quantity": 3,
                    "unitPrice": 250.0
                }
                """;
        mockMvc.perform(post("/api/hospitalizations/" + hospId + "/patient-consumptions")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(consRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemName").value("Seringue 5ml"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.unitPrice").value(250.0));

        // 6. Get patient consumptions
        mockMvc.perform(get("/api/hospitalizations/" + hospId + "/patient-consumptions")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].itemName").value("Seringue 5ml"));

        // 7. Verify precalculate invoice endpoint includes care and consumption
        mockMvc.perform(post("/api/invoices/precalculate")
                .param("patientId", patientA.getId().toString())
                .param("visitId", visit.getId().toString())
                .header("Authorization", "Bearer " + tokenBillingAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.itemType == 'AMI_CARE')].label").value("Soin : PANSEMENT (Pansement abdominal refait)"))
                .andExpect(jsonPath("$.items[?(@.itemType == 'AMI_CARE')].unitPrice").value(4500.0))
                .andExpect(jsonPath("$.items[?(@.label == 'Consommation : Seringue 5ml')].unitPrice").value(250.0))
                .andExpect(jsonPath("$.items[?(@.label == 'Consommation : Seringue 5ml')].quantity").value(3.0));
    }

    @Test
    void givenDoctor_whenCreateAndValidateOperatingReport_thenSuccessAndInvoiceCalculated() throws Exception {
        TenantContext.setTenantId(org.getId());
        
        com.joprelys.backend.visit.infrastructure.persistence.VisitEntity visit = new com.joprelys.backend.visit.infrastructure.persistence.VisitEntity(
                patientA, "VIS-OR-01", "Soins", "Chirurgie", "CHIRURGIE", doctor.getId(), java.time.Instant.now()
        );
        visit = visitRepository.save(visit);
        TenantContext.clear();

        String admitRequest = String.format("""
                {
                    "patientId": "%s",
                    "serviceName": "CHIRURGIE",
                    "roomNumber": "202",
                    "bedNumber": "Lit X",
                    "admissionReason": "Chirurgie programmée",
                    "visitId": "%s",
                    "responsiblePractitionerId": "%s"
                }
                """, patientA.getId(), visit.getId(), doctor.getId());

        String responseStr = mockMvc.perform(post("/api/hospitalizations")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(admitRequest))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String hospId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor()
                .newInstance().readTree(responseStr).get("id").asText();

        // 1. Post operating report (not validated yet)
        String orRequest = """
                {
                    "procedureName": "Appendicectomie",
                    "procedureDescription": "Incision McBurney, ablation de l'appendice.",
                    "preOperativeDiagnosis": "Appendicite aiguë",
                    "postOperativeDiagnosis": "Appendicite phlegmoneuse",
                    "anesthesiaType": "GÉNÉRALE",
                    "anesthesiaDescription": "AG avec intubation",
                    "kSurgeonValue": 50.0,
                    "kAnesthesistValue": 20.0,
                    "kBlocValue": 30.0,
                    "implants": [
                        {
                            "implantName": "Fil de suture résorbable",
                            "lotNumber": "LOT12345",
                            "quantity": 2,
                            "unitPrice": 1200.0,
                            "manufacturer": "Ethicon"
                        }
                    ]
                }
                """;

        String reportStr = mockMvc.perform(post("/api/hospitalizations/" + hospId + "/operating-reports")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.procedureName").value("Appendicectomie"))
                .andExpect(jsonPath("$.kSurgeonValue").value(50.0))
                .andExpect(jsonPath("$.implants.length()").value(1))
                .andExpect(jsonPath("$.implants[0].implantName").value("Fil de suture résorbable"))
                .andExpect(jsonPath("$.validated").value(false))
                .andReturn().getResponse().getContentAsString();

        String reportId = com.fasterxml.jackson.databind.ObjectMapper.class.getDeclaredConstructor()
                .newInstance().readTree(reportStr).get("id").asText();

        // 2. Get operating reports
        mockMvc.perform(get("/api/hospitalizations/" + hospId + "/operating-reports")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reportId));

        // 3. Verify invoice precalculation DOES NOT include K values yet because report is NOT validated
        mockMvc.perform(post("/api/invoices/precalculate")
                .param("patientId", patientA.getId().toString())
                .param("visitId", visit.getId().toString())
                .header("Authorization", "Bearer " + tokenBillingAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.itemType == 'K_SURGEON')]").isEmpty())
                .andExpect(jsonPath("$.items[?(@.label == 'Implant : Fil de suture résorbable (Lot: LOT12345)')]").isEmpty());

        // 4. Validate operating report
        mockMvc.perform(post("/api/hospitalizations/operating-reports/" + reportId + "/validate")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validated").value(true))
                .andExpect(jsonPath("$.validatedBy").isNotEmpty());

        // 5. Verify invoice precalculation DOES include K values & implants now
        mockMvc.perform(post("/api/invoices/precalculate")
                .param("patientId", patientA.getId().toString())
                .param("visitId", visit.getId().toString())
                .header("Authorization", "Bearer " + tokenBillingAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.itemType == 'K_SURGEON')].label").value("CRO : Honoraires Chirurgien (K 50.0 - Appendicectomie)"))
                .andExpect(jsonPath("$.items[?(@.itemType == 'K_SURGEON')].unitPrice").value(50000.0)) // 50 * 1000
                .andExpect(jsonPath("$.items[?(@.itemType == 'K_ANESTHESIST')].unitPrice").value(20000.0)) // 20 * 1000
                .andExpect(jsonPath("$.items[?(@.itemType == 'K_BLOC')].unitPrice").value(30000.0)) // 30 * 1000
                .andExpect(jsonPath("$.items[?(@.label == 'Implant : Fil de suture résorbable (Lot: LOT12345)')].unitPrice").value(1200.0))
                .andExpect(jsonPath("$.items[?(@.label == 'Implant : Fil de suture résorbable (Lot: LOT12345)')].quantity").value(2.0));
    }
}
