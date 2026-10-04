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

class HospitalizationWorkflowControllerTest extends HospitalizationControllerTestSupport {
    @Test
    void placementAndMedicationReadsCannotCrossTheTenantBoundary() throws Exception {
        VisitEntity ownVisit = createVisit(patientA, "VIS-OWN", "Admission", "MÉDECINE GÉNÉRALE");
        String stayId = admit(patientA, ownVisit, PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED, "Surveillance");
        OrganizationEntity other = organizationRepository.save(new OrganizationEntity(
                "Autre clinique", "other@local", "123", "Street", "Douala"));
        UUID foreignSpaceId, foreignPatientId, foreignItemId;
        TenantContext.setTenantId(other.getId());
        try {
            UserAccountEntity otherDoctor = new UserAccountEntity("other.doctor@local", "Autre médecin", "MEDECIN", "hash");
            otherDoctor.setOrganizationId(other.getId()); otherDoctor = userAccountRepository.save(otherDoctor);
            FacilitySpaceEntity space = facilitySpaceRepository.save(new FacilitySpaceEntity(
                    other.getId(), null, "OTHER", "Autre chambre", "HOSPITAL_ROOM"));
            inpatientSpaceProfileRepository.save(new InpatientSpaceProfileEntity(space.getId(), other.getId(), "HOSPITAL_ROOM", "STANDARD"));
            foreignSpaceId = space.getId();
            PatientEntity patient = patientRepository.save(new PatientEntity("DPU-FOREIGN", "PAT-FOREIGN", "Autre patient", "MASCULIN",
                    LocalDate.of(1990, 1, 1), "123", "Douala", "", "", "", "", "", ""));
            foreignPatientId = patient.getId();
            VisitEntity visit = visitRepository.save(new VisitEntity(patient, "VIS-FOREIGN", "Admission", "Médecine"));
            ConsultationEntity consultation = consultations.save(new ConsultationEntity(visit, otherDoctor,
                    "CONS-FOREIGN", "Symptômes", null, "Diagnostic", null, null));
            PrescriptionEntity prescription = new PrescriptionEntity(consultation);
            prescription.setStatus("ACTIVE"); prescription.setPrescriptionNumber("RX-FOREIGN");
            PrescriptionItemEntity item = new PrescriptionItemEntity(prescription, "Paracétamol Injectable", "1g", null, null, "1", null, 0);
            prescription.getItems().add(item); prescriptions.saveAndFlush(prescription); foreignItemId = item.getId();
        } finally { TenantContext.clear(); }
        mockMvc.perform(get("/api/hospitalizations/placement-options").header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spaces.length()").value(2))
                .andExpect(jsonPath("$.staff.length()").value(2));
        mockMvc.perform(get("/api/hospitalizations/placement-beds").param("spaceId", foreignSpaceId.toString())
                .header("Authorization", bearer(doctorToken))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/hospitalizations/admission-visits/{id}", foreignPatientId)
                .header("Authorization", bearer(doctorToken))).andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/hospitalizations/{id}/eligible-medications", stayId).header("Authorization", bearer(nurseToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", stayId)
                .header("Authorization", bearer(nurseToken)).contentType(MediaType.APPLICATION_JSON)
                .content(medicationPayload(foreignItemId, "Paracétamol Injectable"))).andExpect(status().isConflict());
    }
    @Test
    void admissionReadsAreAvailableWithoutConfigurationPermissions() throws Exception {
        mockMvc.perform(get("/api/hospitalizations/placement-options")).andExpect(status().isUnauthorized());
        VisitEntity visit = createVisit(patientA, "VIS-OPTIONS", "Surveillance", "MÉDECINE GÉNÉRALE");
        for (String token : java.util.List.of(doctorToken, hospitalizationManagerToken)) {
            mockMvc.perform(get("/api/hospitalizations/placement-options").header("Authorization", bearer(token)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.units.length()").value(2))
                    .andExpect(jsonPath("$.staff[0].email").doesNotExist());
            mockMvc.perform(get("/api/hospitalizations/placement-beds").param("spaceId", placement(PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED).space().getId().toString())
                    .header("Authorization", bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
            mockMvc.perform(get("/api/hospitalizations/admission-visits/{id}", patientA.getId()).header("Authorization", bearer(token)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(visit.getId().toString()));
            mockMvc.perform(get("/api/spatial/configuration/beds")
                            .param("spaceId", placement(PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED).space().getId().toString())
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                            "/api/spatial/configuration/beds/{id}", placement(PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED).bed().getId())
                            .header("Authorization", bearer(token))).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/staff").header("Authorization", bearer(hospitalizationManagerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hospitalizations/placement-options").header("Authorization", bearer(nurseToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/hospitalizations/admission-visits/{id}", patientA.getId()).header("Authorization", bearer(nurseToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hospitalizations/placement-options").header("Authorization", bearer(billingAgentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/hospitalizations/placement-beds").param("spaceId", UUID.randomUUID().toString())
                .header("Authorization", bearer(doctorToken))).andExpect(status().isNotFound());
    }

    @Test
    void medicationRequiresAnActiveUnexpiredPrescriptionForTheSamePatient() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-MED", "Admission", "MÉDECINE GÉNÉRALE");
        String stayId = admit(patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED, "Surveillance");
        UUID valid = createPrescription(visit, "ACTIVE", Instant.now().plusSeconds(3600));
        UUID draft = createPrescription(createVisit(patientA, "VIS-DRAFT", "Admission", "MÉDECINE GÉNÉRALE"), "DRAFT", null);
        UUID expired = createPrescription(createVisit(patientA, "VIS-EXPIRED", "Admission", "MÉDECINE GÉNÉRALE"), "ACTIVE", Instant.now().minusSeconds(60));
        UUID otherPatient = createPrescription(createVisit(patientB, "VIS-OTHER", "Admission", "MÉDECINE GÉNÉRALE"), "ACTIVE", null);
        mockMvc.perform(get("/api/hospitalizations/{id}/eligible-medications", stayId).header("Authorization", bearer(nurseToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].prescriptionItemId").value(valid.toString()));
        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", stayId)
                .header("Authorization", bearer(nurseToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"medicationName\":\"Paracétamol Injectable\",\"dose\":\"1g IV\"}"))
                .andExpect(status().isBadRequest());
        for (UUID invalid : java.util.List.of(draft, expired, otherPatient, UUID.randomUUID())) {
            mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", stayId)
                    .header("Authorization", bearer(nurseToken)).contentType(MediaType.APPLICATION_JSON)
                    .content(medicationPayload(invalid, "Paracétamol Injectable"))).andExpect(status().isConflict());
        }
        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", stayId)
                .header("Authorization", bearer(nurseToken)).contentType(MediaType.APPLICATION_JSON)
                .content(medicationPayload(valid, "Autre médicament"))).andExpect(status().isConflict());
        mockMvc.perform(get("/api/hospitalizations/{id}/medication-administrations", stayId).header("Authorization", bearer(doctorToken)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", stayId)
                .header("Authorization", bearer(nurseToken)).contentType(MediaType.APPLICATION_JSON)
                .content(medicationPayload(valid, "Paracétamol Injectable"))).andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE prescriptions SET status = 'CANCELLED' WHERE id = (SELECT prescription_id FROM prescription_items WHERE id = ?)", valid);
        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", stayId)
                .header("Authorization", bearer(nurseToken)).contentType(MediaType.APPLICATION_JSON)
                .content(medicationPayload(valid, "Paracétamol Injectable"))).andExpect(status().isConflict());
        mockMvc.perform(get("/api/hospitalizations/{id}/medication-administrations", stayId).header("Authorization", bearer(doctorToken)))
                .andExpect(jsonPath("$.length()").value(1));
    }

}
