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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class HospitalizationControllerTestSupport {

    protected static final String PEDIATRICS_SERVICE = "PÉDIATRIE";
    protected static final String PEDIATRICS_SPACE = "101";
    protected static final String PEDIATRICS_BED = "Lit A";
    protected static final String SURGERY_SERVICE = "CHIRURGIE";
    protected static final String SURGERY_SPACE = "202";
    protected static final String SURGERY_BED = "Lit X";

    @Autowired protected ConsultationRepository consultations;
    @Autowired protected PrescriptionRepository prescriptions;
    @Autowired protected MockMvc mockMvc;
    @Autowired protected OrganizationRepository organizationRepository;
    @Autowired protected UserAccountRepository userAccountRepository;
    @Autowired protected PatientRepository patientRepository;
    @Autowired protected HospitalizationRepository hospitalizationRepository;
    @Autowired protected VisitRepository visitRepository;
    @Autowired protected OrganizationalUnitRepository organizationalUnitRepository;
    @Autowired protected FacilitySpaceRepository facilitySpaceRepository;
    @Autowired protected InpatientSpaceProfileRepository inpatientSpaceProfileRepository;
    @Autowired protected OrganizationalUnitSpaceAssignmentRepository unitSpaceAssignmentRepository;
    @Autowired protected StaffOrganizationalUnitAssignmentRepository staffUnitAssignmentRepository;
    @Autowired protected BedRepository bedRepository;
    @Autowired protected BedAssignmentRepository bedAssignmentRepository;
    @Autowired protected JdbcTemplate jdbcTemplate;
    @Autowired protected JwtService jwtService;
    @Autowired protected JsonMapper jsonMapper;

    protected final Map<String, PlacementFixture> placements = new HashMap<>();

    protected OrganizationEntity organization;
    protected UserAccountEntity doctor;
    protected PatientEntity patientA;
    protected PatientEntity patientB;
    protected String doctorToken;
    protected String nurseToken;
    protected String hospitalizationManagerToken;
    protected String billingAgentToken;

    @BeforeEach
    void setUp() {
        clearData();
        placements.clear();

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

            UserAccountEntity nurse = new UserAccountEntity(
                    "nurse.hosp@joprelys.local",
                    "Infirmier hospitalisation",
                    "INFIRMIER",
                    "passhash");
            nurse.setOrganizationId(organization.getId());
            nurse = userAccountRepository.save(nurse);
            nurseToken = jwtService.createToken(nurse).value();

            UserAccountEntity hospitalizationManager = new UserAccountEntity(
                    "manager.hosp@joprelys.local",
                    "Responsable hospitalisation",
                    "RESPONSABLE_HOSPITALISATION",
                    "passhash");
            hospitalizationManager.setOrganizationId(organization.getId());
            hospitalizationManager = userAccountRepository.save(hospitalizationManager);
            hospitalizationManagerToken = jwtService.createToken(hospitalizationManager).value();

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

            createConfiguredBed(PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED);
            createConfiguredBed(SURGERY_SERVICE, SURGERY_SPACE, SURGERY_BED);
        } finally {
            TenantContext.clear();
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        clearData();
    }

    protected String medicationPayload(UUID itemId, String name) {
        return "{\"medicationName\":\"%s\",\"dose\":\"1g IV\",\"prescriptionItemId\":\"%s\"}".formatted(name, itemId);
    }

    protected UUID createPrescription(VisitEntity visit, String state, Instant expiresAt) {
        TenantContext.setTenantId(organization.getId());
        try {
            ConsultationEntity consultation = consultations.save(new ConsultationEntity(visit, doctor,
                    "CONS-" + UUID.randomUUID(), "Symptômes", null, "Diagnostic", null, null));
            PrescriptionEntity prescription = new PrescriptionEntity(consultation);
            prescription.setStatus(state); prescription.setExpiresAt(expiresAt);
            prescription.setPrescriptionNumber("RX-" + UUID.randomUUID());
            PrescriptionItemEntity item = new PrescriptionItemEntity(prescription, "Paracétamol Injectable", "1g",
                    "Si douleur", "1 jour", "1", null, 0);
            prescription.getItems().add(item); prescriptions.saveAndFlush(prescription);
            return item.getId();
        } finally { TenantContext.clear(); }
    }

    protected String admit(
            PatientEntity patient,
            VisitEntity visit,
            String serviceName,
            String spaceName,
            String bedNumber,
            String reason) throws Exception {
        String response = mockMvc.perform(post("/api/hospitalizations")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(admissionPayload(patient, visit, serviceName, spaceName, bedNumber, reason)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andExpect(jsonPath("$.serviceName").value(serviceName))
                .andExpect(jsonPath("$.spaceName").value(spaceName))
                .andExpect(jsonPath("$.bedNumber").value(bedNumber))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return jsonMapper.readTree(response).get("id").asString();
    }

    protected String admissionPayload(
            PatientEntity patient,
            VisitEntity visit,
            String serviceName,
            String spaceName,
            String bedNumber,
            String reason) {
        PlacementFixture placement = placement(serviceName, spaceName, bedNumber);
        return """
                {
                  "patientId":"%s",
                  "serviceUnitId":"%s",
                  "spaceId":"%s",
                  "bedId":"%s",
                  "admissionReason":"%s",
                  "visitId":"%s",
                  "responsiblePractitionerId":"%s"
                }
                """.formatted(
                patient.getId(),
                placement.unit().getId(),
                placement.space().getId(),
                placement.bed().getId(),
                reason,
                visit.getId(),
                doctor.getId());
    }

    protected VisitEntity createVisit(PatientEntity patient, String number, String reason, String specialty) {
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

    protected void createConfiguredBed(String serviceName, String spaceName, String bedNumber) {
        String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OrganizationalUnitEntity unit = organizationalUnitRepository.save(new OrganizationalUnitEntity(
                organization.getId(),
                null,
                "UNIT-" + token,
                serviceName,
                OrganizationalUnitType.CARE_UNIT,
                null));

        // Le médecin responsable doit être affecté à l'unité d'admission (règle d'admission hospitalière).
        staffUnitAssignmentRepository.save(new StaffOrganizationalUnitAssignmentEntity(
                organization.getId(),
                doctor.getId(),
                unit.getId(),
                "PRACTITIONER",
                false,
                Instant.now().minusSeconds(60),
                null));

        FacilitySpaceEntity space = facilitySpaceRepository.save(new FacilitySpaceEntity(
                organization.getId(),
                null,
                "SPACE-" + token,
                spaceName,
                "HOSPITAL_ROOM"));

        inpatientSpaceProfileRepository.save(new InpatientSpaceProfileEntity(
                space.getId(),
                organization.getId(),
                "HOSPITAL_ROOM",
                "STANDARD"));

        unitSpaceAssignmentRepository.save(new OrganizationalUnitSpaceAssignmentEntity(
                organization.getId(),
                unit.getId(),
                space.getId(),
                Instant.now().minusSeconds(60),
                null));

        BedEntity bed = bedRepository.save(new BedEntity(space, bedNumber));
        placements.put(key(serviceName, spaceName, bedNumber), new PlacementFixture(unit, space, bed));
    }

    protected PlacementFixture placement(String serviceName, String spaceName, String bedNumber) {
        PlacementFixture fixture = placements.get(key(serviceName, spaceName, bedNumber));
        if (fixture == null) {
            throw new IllegalStateException(
                    "Placement de test introuvable : " + serviceName + " / " + spaceName + " / " + bedNumber);
        }
        return fixture;
    }

    protected BedEntity configuredBed(String serviceName, String spaceName, String bedNumber) {
        PlacementFixture fixture = placement(serviceName, spaceName, bedNumber);
        return bedRepository.findById(fixture.bed().getId()).orElseThrow();
    }

    protected void assertPdf(String contentType) {
        assertNotNull(contentType);
        if (!contentType.startsWith(MediaType.APPLICATION_PDF_VALUE)) {
            throw new AssertionError("Type MIME PDF attendu, reçu : " + contentType);
        }
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String key(String serviceName, String spaceName, String bedNumber) {
        return serviceName + "|" + spaceName + "|" + bedNumber;
    }

    protected void clearData() {
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
        jdbcTemplate.update("DELETE FROM bed_state_changes");
        jdbcTemplate.update("DELETE FROM bed_assignments");
        jdbcTemplate.update("DELETE FROM hospitalizations");
        jdbcTemplate.update("DELETE FROM beds");
        jdbcTemplate.update("DELETE FROM organizational_unit_space_assignments");
        jdbcTemplate.update("DELETE FROM inpatient_space_profiles");
        jdbcTemplate.update("DELETE FROM facility_spaces");
        jdbcTemplate.update("DELETE FROM facility_location_nodes");
        jdbcTemplate.update("DELETE FROM staff_organizational_unit_assignments");
        jdbcTemplate.update("DELETE FROM organizational_units");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patient_allergies");
        jdbcTemplate.update("DELETE FROM patient_medical_history");
        jdbcTemplate.update("DELETE FROM external_access_requests");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    protected record PlacementFixture(
            OrganizationalUnitEntity unit,
            FacilitySpaceEntity space,
            BedEntity bed) {
    }
}
