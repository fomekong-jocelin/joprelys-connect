package com.joprelys.backend.fhir;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository;
import com.joprelys.backend.audit.infrastructure.persistence.AuditLogRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FhirControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private VitalsRepository vitalsRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private PatientEntity patientA;
    private VisitEntity visitA;
    private VitalsEntity vitalsA;
    private String tokenMedecinA;
    private String tokenMedecinB;
    private String tokenPatientA;

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
        jdbcTemplate.update("DELETE FROM vitals");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM external_access_requests");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Organizations
        orgA = new OrganizationEntity("Clinique A", "contact@cliniquea.org", "123", "Adresse A", "Douala");
        orgA = organizationRepository.save(orgA);

        orgB = new OrganizationEntity("Clinique B", "contact@cliniqueb.org", "456", "Adresse B", "Yaoundé");
        orgB = organizationRepository.save(orgB);

        // 2. Create Doctors
        UserAccountEntity medecinA = new UserAccountEntity(
                "medecin.a@cliniquea.org",
                "Dr. Alice",
                "MEDECIN",
                "passhash"
        );
        medecinA.setOrganizationId(orgA.getId());
        medecinA = userAccountRepository.save(medecinA);
        tokenMedecinA = jwtService.createToken(medecinA).value();

        UserAccountEntity medecinB = new UserAccountEntity(
                "medecin.b@cliniqueb.org",
                "Dr. Bob",
                "MEDECIN",
                "passhash"
        );
        medecinB.setOrganizationId(orgB.getId());
        medecinB = userAccountRepository.save(medecinB);
        tokenMedecinB = jwtService.createToken(medecinB).value();

        // 3. Create Patient A in Organization A
        TenantContext.setTenantId(orgA.getId());
        patientA = new PatientEntity(
                "DPU-000001",
                "LOC-A-001",
                "Jean Dupont",
                "MASCULIN",
                LocalDate.of(1980, 1, 1),
                "+237600000001",
                "Douala",
                "Akwa",
                "Rue 1",
                "Marie",
                "+237600000002",
                "Aucune",
                "Aucun"
        );
        patientA = patientRepository.save(patientA);

        // 4. Create Visit and Vitals for Patient A
        visitA = new VisitEntity(patientA, "VIS-000001", "Routine check", "PÉDIATRIE");
        visitA.setOrganizationId(orgA.getId());
        visitA = visitRepository.save(visitA);

        vitalsA = new VitalsEntity(
                visitA,
                BigDecimal.valueOf(37.5),
                BigDecimal.valueOf(80.2),
                180,
                72,
                120,
                80,
                98,
                BigDecimal.valueOf(0.95),
                16,
                BigDecimal.valueOf(24.75)
        );
        vitalsA = vitalsRepository.save(vitalsA);

        // Generate Patient Token for role authorization verification
        tokenPatientA = jwtService.createPatientToken(patientA).value();

        TenantContext.clear();
    }

    @Test
    void whenGetPatientAuthorized_thenReturnsFhirPatient() throws Exception {
        mockMvc.perform(get("/fhir/Patient/" + patientA.getId())
                .header("Authorization", "Bearer " + tokenMedecinA)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Patient"))
                .andExpect(jsonPath("$.id").value(patientA.getId().toString()))
                .andExpect(jsonPath("$.gender").value("male"))
                .andExpect(jsonPath("$.name[0].text").value("Jean Dupont"))
                .andExpect(jsonPath("$.name[0].family").value("Dupont"))
                .andExpect(jsonPath("$.name[0].given[0]").value("Jean"));
    }

    @Test
    void whenGetPatientUnauthorizedCrossTenant_thenForbidden() throws Exception {
        mockMvc.perform(get("/fhir/Patient/" + patientA.getId())
                .header("Authorization", "Bearer " + tokenMedecinB)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenGetPatientInvalidRole_thenForbidden() throws Exception {
        mockMvc.perform(get("/fhir/Patient/" + patientA.getId())
                .header("Authorization", "Bearer " + tokenPatientA)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenGetEncounterAuthorized_thenReturnsFhirEncounter() throws Exception {
        mockMvc.perform(get("/fhir/Encounter/" + visitA.getId())
                .header("Authorization", "Bearer " + tokenMedecinA)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Encounter"))
                .andExpect(jsonPath("$.id").value(visitA.getId().toString()))
                .andExpect(jsonPath("$.identifier[0].value").value("VIS-000001"))
                .andExpect(jsonPath("$.status").value("in-progress"));
    }

    @Test
    void whenGetEncounterUnauthorizedCrossTenant_thenForbidden() throws Exception {
        mockMvc.perform(get("/fhir/Encounter/" + visitA.getId())
                .header("Authorization", "Bearer " + tokenMedecinB)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenGetObservationsAuthorized_thenReturnsFhirBundle() throws Exception {
        mockMvc.perform(get("/fhir/Observation?patient=" + patientA.getId())
                .header("Authorization", "Bearer " + tokenMedecinA)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.type").value("searchset"))
                .andExpect(jsonPath("$.total").value(9)) // 8 simple vitals + 1 blood pressure
                .andExpect(jsonPath("$.entry[0].resource.resourceType").value("Observation"));
    }

    @Test
    void whenGetObservationsUnauthorizedCrossTenant_thenForbidden() throws Exception {
        mockMvc.perform(get("/fhir/Observation?patient=" + patientA.getId())
                .header("Authorization", "Bearer " + tokenMedecinB)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
