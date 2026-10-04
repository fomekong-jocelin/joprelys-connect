package com.joprelys.backend.hospitalization.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

class HospitalizationControllerTest extends HospitalizationControllerTestSupport {
    @Test
    void medicalDecisionShouldKeepBedOccupiedUntilPhysicalDeparture() throws Exception {
        VisitEntity visitA = createVisit(patientA, "VIS-H-001", "Motif de visite A", "MÉDECINE GÉNÉRALE");
        VisitEntity visitB = createVisit(patientB, "VIS-H-002", "Motif de visite B", "MÉDECINE GÉNÉRALE");

        String hospitalizationId = admit(
                patientA,
                visitA,
                PEDIATRICS_SERVICE,
                PEDIATRICS_SPACE,
                PEDIATRICS_BED,
                "Surveillance post-opératoire");

        mockMvc.perform(post("/api/hospitalizations")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(admissionPayload(
                                patientB,
                                visitB,
                                PEDIATRICS_SERVICE,
                                PEDIATRICS_SPACE,
                                PEDIATRICS_BED,
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
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andExpect(jsonPath("$.dischargeDecidedAt").isNotEmpty())
                .andExpect(jsonPath("$.physicalDepartureAt").isEmpty())
                .andExpect(jsonPath("$.dischargedAt").isEmpty());

        UUID stayId = UUID.fromString(hospitalizationId);
        TenantContext.setTenantId(organization.getId());
        try {
            assertTrue(bedAssignmentRepository.findActiveByHospitalizationId(stayId).isPresent());
            assertTrue(hospitalizationRepository.findActiveByPatientId(patientA.getId()).isPresent());
            assertTrue(configuredBed(PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED).getStatus()
                    == BedStatus.OCCUPIED);
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(get("/api/hospitalizations/{id}/pdf", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/hospitalizations/{id}/physical-departure", hospitalizationId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/hospitalizations/{id}/physical-departure", hospitalizationId)
                        .header("Authorization", bearer(hospitalizationManagerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmed":true,
                                  "note":"Patient accompagné jusqu'à la sortie principale."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SORTI"))
                .andExpect(jsonPath("$.physicalDepartureAt").isNotEmpty())
                .andExpect(jsonPath("$.dischargedAt").isNotEmpty());

        TenantContext.setTenantId(organization.getId());
        try {
            assertFalse(bedAssignmentRepository.findActiveByHospitalizationId(stayId).isPresent());
            assertFalse(hospitalizationRepository.findActiveByPatientId(patientA.getId()).isPresent());
            assertTrue(configuredBed(PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED).getStatus()
                    == BedStatus.CLEANING);
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(get("/api/hospitalizations/{id}/pdf", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(result -> assertPdf(result.getResponse().getContentType()));
    }

    @Test
    void physicalDepartureShouldRequirePriorMedicalDecision() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-NO-DIS-01", "Admission", "MÉDECINE GÉNÉRALE");
        String hospitalizationId = admit(
                patientA,
                visit,
                PEDIATRICS_SERVICE,
                PEDIATRICS_SPACE,
                PEDIATRICS_BED,
                "Surveillance");

        mockMvc.perform(post("/api/hospitalizations/{id}/physical-departure", hospitalizationId)
                        .header("Authorization", bearer(hospitalizationManagerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void givenDoctor_whenDownloadEntryPdf_thenSuccess() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-ENT-01", "Admission", "MÉDECINE GÉNÉRALE");
        String hospitalizationId = admit(
                patientA,
                visit,
                PEDIATRICS_SERVICE,
                PEDIATRICS_SPACE,
                PEDIATRICS_BED,
                "Surveillance");

        mockMvc.perform(get("/api/hospitalizations/{id}/entry-pdf", hospitalizationId)
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(result -> assertPdf(result.getResponse().getContentType()));
    }

    @Test
    void againstMedicalAdviceShouldBecomeFinalOnlyAfterPhysicalDeparture() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-CAD-02", "Admission", "MÉDECINE GÉNÉRALE");
        String hospitalizationId = admit(
                patientA,
                visit,
                PEDIATRICS_SERVICE,
                PEDIATRICS_SPACE,
                PEDIATRICS_BED,
                "Surveillance");

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
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andExpect(jsonPath("$.dischargeAgainstMedicalAdvice").value(true));

        mockMvc.perform(post("/api/hospitalizations/{id}/physical-departure", hospitalizationId)
                        .header("Authorization", bearer(hospitalizationManagerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true}"))
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
                patientA,
                visit,
                PEDIATRICS_SERVICE,
                PEDIATRICS_SPACE,
                PEDIATRICS_BED,
                "Surveillance");

        mockMvc.perform(multipart("/api/hospitalizations/{id}/consents", hospitalizationId)
                        .param("consentType", "ANESTHESIA")
                        .param("patientSignaturePresent", "true")
                        .param("witnessName", "Jean Dupont")
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentType").value("ANESTHESIA"))
                .andExpect(jsonPath("$.patientSignaturePresent").value(true));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "consent.pdf",
                MediaType.APPLICATION_PDF_VALUE,
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
    void clinicalWritesShouldRespectDoctorAndNurseDutySegregation() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-CARE-01", "Soins", "MÉDECINE GÉNÉRALE");
        String hospitalizationId = admit(
                patientA,
                visit,
                PEDIATRICS_SERVICE,
                PEDIATRICS_SPACE,
                PEDIATRICS_BED,
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

        UUID prescriptionItemId = createPrescription(visit, "ACTIVE", null);
        String medicationPayload = """
                {
                  "medicationName":"Paracétamol Injectable",
                  "dose":"1g IV",
                  "prescriptionItemId":"%s"
                }
                """.formatted(prescriptionItemId);

        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", hospitalizationId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicationPayload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", hospitalizationId)
                        .header("Authorization", bearer(nurseToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicationPayload))
                .andExpect(status().isCreated());

        String consumptionPayload = """
                {"itemName":"Seringue 5ml","quantity":3,"unitPrice":250.0}
                """;

        mockMvc.perform(post("/api/hospitalizations/{id}/patient-consumptions", hospitalizationId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(consumptionPayload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/hospitalizations/{id}/patient-consumptions", hospitalizationId)
                        .header("Authorization", bearer(nurseToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(consumptionPayload))
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
                patientA,
                visit,
                SURGERY_SERVICE,
                SURGERY_SPACE,
                SURGERY_BED,
                "Chirurgie programmée");

        String reportResponse = mockMvc.perform(post(
                                "/api/hospitalizations/{id}/operating-reports",
                                hospitalizationId)
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
                .andReturn()
                .getResponse()
                .getContentAsString();
        String reportId = jsonMapper.readTree(reportResponse).get("id").asString();

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
                .andExpect(jsonPath(
                                "$.items[?(@.label == 'Implant : Fil de suture résorbable (Lot: LOT12345)')].quantity")
                        .value(2.0));
    }

}
