package com.joprelys.backend.patient.reconciliation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientIdentityAliasRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientReconciliationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired EmergencyRepository emergencyRepository;
    @Autowired PatientCanonicalLinkRepository canonicalLinkRepository;
    @Autowired PatientIdentityAliasRepository aliasRepository;
    @Autowired PatientReconciliationEventRepository eventRepository;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbcTemplate;

    private OrganizationEntity organization;
    private UserAccountEntity administrator;
    private String token;
    private PatientEntity provisionalPatient;
    private PatientEntity canonicalPatient;
    private EmergencyEntity emergency;

    @BeforeEach
    void setUp() {
        cleanup();

        organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Réconciliation",
                "reconciliation@joprelys.local",
                "237600000000",
                "Rue de la clinique",
                "Douala"));

        administrator = new UserAccountEntity(
                "admin.reconciliation@joprelys.local",
                "Administrateur Réconciliation",
                "ADMIN_CLINIQUE",
                "passhash");
        administrator.setOrganizationId(organization.getId());
        administrator = userAccountRepository.save(administrator);
        token = jwtService.createToken(administrator).value();

        TenantContext.setTenantId(organization.getId());
        provisionalPatient = PatientEntity.provisionalEmergency(
                "DPU-JOP-20260711-700001",
                "PAT-20260711-700001",
                "URG-TEMP-20260711-700001",
                "FEMININ",
                "25-35",
                "Cicatrice légère au sourcil gauche",
                Instant.parse("2026-07-11T10:00:00Z"),
                "Carrefour Akwa",
                IdentityConfidenceLevel.HIGH);
        provisionalPatient.setFullName("Nadège Maffock");
        provisionalPatient.setGender("FEMININ");
        provisionalPatient.setBirthDate(LocalDate.of(1994, 5, 10));
        provisionalPatient.setPhone("+237699000111");
        provisionalPatient.setCity("Douala");
        provisionalPatient.transitionIdentityStatus(PatientIdentityStatus.VERIFIED);
        provisionalPatient = patientRepository.save(provisionalPatient);

        canonicalPatient = patientRepository.save(new PatientEntity(
                "DPU-JOP-20240101-000010",
                "PAT-20240101-000010",
                "Nadège Maffock",
                "FEMININ",
                LocalDate.of(1994, 5, 10),
                "+237699000111",
                "Douala",
                "Akwa",
                "Rue 10",
                "Paul Maffock",
                "+237699000222",
                "",
                ""));

        emergency = emergencyRepository.save(new EmergencyEntity(
                provisionalPatient,
                null,
                "ACCOMPANIED",
                "ORANGE",
                "UNSTABLE",
                "Douleur abdominale intense",
                100,
                60,
                110,
                BigDecimal.valueOf(38.2),
                administrator.getId()));
        TenantContext.clear();
    }

    @Test
    void shouldLinkUrgTempToCanonicalPatientWithoutMovingEmergencyHistory() throws Exception {
        String idempotencyKey = "reconciliation-link-700001";
        String body = """
                {
                  "decision": "LINK_EXISTING_DPU",
                  "candidatePatientId": "%s",
                  "evidenceSourceType": "DOCUMENT",
                  "evidenceReference": "CNI-700001",
                  "justification": "Identité confirmée par CNI originale et déclaration du patient."
                }
                """.formatted(canonicalPatient.getId());

        mockMvc.perform(post("/api/patient-reconciliations/" + provisionalPatient.getId() + "/decisions")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("LINK_EXISTING_DPU"))
                .andExpect(jsonPath("$.canonicalPatientId").value(canonicalPatient.getId().toString()))
                .andExpect(jsonPath("$.sourceIdentityStatus").value("MERGED"))
                .andExpect(jsonPath("$.temporaryPatientNumber").value("URG-TEMP-20260711-700001"))
                .andExpect(jsonPath("$.contributingPatientIds.length()").value(2))
                .andExpect(jsonPath("$.replayed").value(false));

        mockMvc.perform(post("/api/patient-reconciliations/" + provisionalPatient.getId() + "/decisions")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.replayed").value(true));

        mockMvc.perform(get("/api/emergencies/patient/" + canonicalPatient.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(emergency.getId().toString()))
                .andExpect(jsonPath("$[0].temporaryPatientNumber").value("URG-TEMP-20260711-700001"));

        TenantContext.setTenantId(organization.getId());
        PatientEntity sourceAfterLink = patientRepository.findById(provisionalPatient.getId()).orElseThrow();
        EmergencyEntity emergencyAfterLink = emergencyRepository.findByIdWithPatientAndLogs(emergency.getId()).orElseThrow();
        TenantContext.clear();

        org.junit.jupiter.api.Assertions.assertEquals(PatientIdentityStatus.MERGED, sourceAfterLink.getIdentityStatus());
        org.junit.jupiter.api.Assertions.assertEquals(provisionalPatient.getId(), emergencyAfterLink.getPatient().getId());
        org.junit.jupiter.api.Assertions.assertTrue(canonicalLinkRepository.findBySourcePatient_Id(provisionalPatient.getId()).isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(3, aliasRepository.findAllByOriginPatient_Id(provisionalPatient.getId()).size());
        org.junit.jupiter.api.Assertions.assertEquals(1, eventRepository.findAllBySourcePatient_IdOrderByCreatedAtDesc(provisionalPatient.getId()).size());
    }

    @Test
    void shouldPresentCandidatesWithoutChangingPatientState() throws Exception {
        mockMvc.perform(get("/api/patient-reconciliations/" + provisionalPatient.getId() + "/candidates")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(canonicalPatient.getId().toString()))
                .andExpect(jsonPath("$[0].score").isNumber())
                .andExpect(jsonPath("$[0].reasons").isArray());

        TenantContext.setTenantId(organization.getId());
        PatientEntity unchanged = patientRepository.findById(provisionalPatient.getId()).orElseThrow();
        TenantContext.clear();
        org.junit.jupiter.api.Assertions.assertEquals(PatientIdentityStatus.VERIFIED, unchanged.getIdentityStatus());
        org.junit.jupiter.api.Assertions.assertTrue(canonicalLinkRepository.findBySourcePatient_Id(provisionalPatient.getId()).isEmpty());
    }

    @Test
    void shouldRejectLegacyDestructiveMergeForUrgTempOrigin() throws Exception {
        String body = """
                {
                  "primaryId": "%s",
                  "secondaryId": "%s"
                }
                """.formatted(canonicalPatient.getId(), provisionalPatient.getId());

        mockMvc.perform(post("/api/patients/merge")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM patient_canonical_links");
        jdbcTemplate.update("DELETE FROM patient_identity_aliases");
        jdbcTemplate.update("DELETE FROM patient_reconciliation_events");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM emergency_belonging_transfers");
        jdbcTemplate.update("DELETE FROM emergency_belongings");
        jdbcTemplate.update("DELETE FROM emergency_legal_basis_acts");
        jdbcTemplate.update("DELETE FROM emergency_legal_bases");
        jdbcTemplate.update("DELETE FROM emergency_capacity_events");
        jdbcTemplate.update("DELETE FROM emergency_identity_statements");
        jdbcTemplate.update("DELETE FROM emergency_third_party_qualities");
        jdbcTemplate.update("DELETE FROM emergency_third_parties");
        jdbcTemplate.update("DELETE FROM emergency_admission_requests");
        jdbcTemplate.update("DELETE FROM resuscitation_logs");
        jdbcTemplate.update("DELETE FROM emergencies");
        jdbcTemplate.update("DELETE FROM patient_identity_declarations");
        jdbcTemplate.update("DELETE FROM patient_identity_status_history");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private String bearer(String value) {
        return "Bearer " + value;
    }
}
