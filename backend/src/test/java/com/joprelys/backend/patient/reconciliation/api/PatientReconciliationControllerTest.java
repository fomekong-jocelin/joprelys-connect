package com.joprelys.backend.patient.reconciliation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.joprelys.backend.patient.reconciliation.domain.PatientAliasType;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientIdentityAliasRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
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
        provisionalPatient = createVerifiedProvisionalPatient();
        canonicalPatient = createCanonicalPatient(
                "DPU-JOP-20240101-000010",
                "PAT-20240101-000010",
                "Nadège Maffock",
                "+237699000111");
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
        linkToCanonical("reconciliation-link-700001", canonicalPatient.getId());

        mockMvc.perform(get("/api/emergencies/patient/" + canonicalPatient.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(emergency.getId().toString()))
                .andExpect(jsonPath("$[0].temporaryPatientNumber").value("URG-TEMP-20260711-700001"));

        TenantContext.setTenantId(organization.getId());
        PatientEntity sourceAfterLink = patientRepository.findById(provisionalPatient.getId()).orElseThrow();
        EmergencyEntity emergencyAfterLink = emergencyRepository.findByIdWithPatientAndLogs(emergency.getId()).orElseThrow();
        assertEquals(PatientIdentityStatus.MERGED, sourceAfterLink.getIdentityStatus());
        assertEquals(provisionalPatient.getId(), emergencyAfterLink.getPatient().getId());
        assertTrue(canonicalLinkRepository.findBySourcePatient_Id(provisionalPatient.getId()).isPresent());
        assertEquals(3, aliasRepository.findAllByOriginPatient_Id(provisionalPatient.getId()).size());
        assertEquals(1, eventRepository.findAllBySourcePatient_IdOrderByCreatedAtDesc(provisionalPatient.getId()).size());
        TenantContext.clear();
    }

    @Test
    void shouldReplayTheSameDecisionIdempotently() throws Exception {
        String key = "reconciliation-replay-700001";
        String body = linkRequest(canonicalPatient.getId());

        mockMvc.perform(post(decisionUrl())
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(false));

        mockMvc.perform(post(decisionUrl())
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));

        TenantContext.setTenantId(organization.getId());
        assertEquals(1, eventRepository.findAllBySourcePatient_IdOrderByCreatedAtDesc(provisionalPatient.getId()).size());
        TenantContext.clear();
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
        assertEquals(PatientIdentityStatus.VERIFIED, unchanged.getIdentityStatus());
        assertTrue(canonicalLinkRepository.findBySourcePatient_Id(provisionalPatient.getId()).isEmpty());
        TenantContext.clear();
    }

    @Test
    void shouldConfirmUrgTempAsNewDpuWithoutCreatingCanonicalLink() throws Exception {
        mockMvc.perform(post(decisionUrl())
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "reconciliation-new-dpu-700001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "CREATE_NEW_DPU",
                                  "evidenceSourceType": "DOCUMENT",
                                  "evidenceReference": "CNI-700001",
                                  "justification": "Identité vérifiée, aucun DPU antérieur confirmé."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("CREATE_NEW_DPU"))
                .andExpect(jsonPath("$.canonicalPatientId").value(provisionalPatient.getId().toString()))
                .andExpect(jsonPath("$.sourceIdentityStatus").value("VERIFIED"));

        TenantContext.setTenantId(organization.getId());
        assertTrue(canonicalLinkRepository.findBySourcePatient_Id(provisionalPatient.getId()).isEmpty());
        assertTrue(eventRepository.existsBySourcePatient_IdAndDecision(
                provisionalPatient.getId(), PatientReconciliationDecision.CREATE_NEW_DPU));
        assertEquals(
                provisionalPatient.getId(),
                aliasRepository.findByAliasTypeAndAliasValue(
                                PatientAliasType.URG_TEMP,
                                provisionalPatient.getTemporaryPatientNumber())
                        .orElseThrow()
                        .getCanonicalPatient()
                        .getId());
        TenantContext.clear();
    }

    @Test
    void shouldDeferDecisionWithoutMutatingIdentity() throws Exception {
        mockMvc.perform(post(decisionUrl())
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "reconciliation-defer-700001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "DEFER",
                                  "evidenceSourceType": "WITNESS",
                                  "justification": "Les informations du témoin ne permettent pas encore une confirmation fiable."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("DEFER"))
                .andExpect(jsonPath("$.sourceIdentityStatus").value("VERIFIED"));

        mockMvc.perform(get("/api/patient-reconciliations/queue")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(provisionalPatient.getId().toString()));
    }

    @Test
    void shouldCorrectWrongLinkWithoutMovingHistoricalEmergency() throws Exception {
        linkToCanonical("reconciliation-wrong-link-700001", canonicalPatient.getId());

        UUID linkedEventId;
        TenantContext.setTenantId(organization.getId());
        linkedEventId = eventRepository
                .findAllBySourcePatient_IdOrderByCreatedAtDesc(provisionalPatient.getId())
                .getFirst()
                .getId();
        TenantContext.clear();

        mockMvc.perform(post("/api/patient-reconciliations/" + provisionalPatient.getId() + "/corrections")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "reconciliation-correction-700001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correctedEventId": "%s",
                                  "replacementCanonicalPatientId": null,
                                  "evidenceSourceType": "DOCUMENT",
                                  "evidenceReference": "REVUE-DPO-700001",
                                  "justification": "La pièce utilisée concernait une homonyme. Le rapprochement est annulé."
                                }
                                """.formatted(linkedEventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("CORRECT_LINK"))
                .andExpect(jsonPath("$.canonicalPatientId").value(provisionalPatient.getId().toString()))
                .andExpect(jsonPath("$.sourceIdentityStatus").value("VERIFIED"));

        TenantContext.setTenantId(organization.getId());
        PatientEntity restoredSource = patientRepository.findById(provisionalPatient.getId()).orElseThrow();
        EmergencyEntity untouchedEmergency = emergencyRepository.findByIdWithPatientAndLogs(emergency.getId()).orElseThrow();
        assertEquals(PatientIdentityStatus.VERIFIED, restoredSource.getIdentityStatus());
        assertTrue(canonicalLinkRepository.findBySourcePatient_Id(provisionalPatient.getId()).isEmpty());
        assertEquals(provisionalPatient.getId(), untouchedEmergency.getPatient().getId());
        assertEquals(2, eventRepository.findAllBySourcePatient_IdOrderByCreatedAtDesc(provisionalPatient.getId()).size());
        assertEquals(
                provisionalPatient.getId(),
                aliasRepository.findByAliasTypeAndAliasValue(
                                PatientAliasType.URG_TEMP,
                                provisionalPatient.getTemporaryPatientNumber())
                        .orElseThrow()
                        .getCanonicalPatient()
                        .getId());
        TenantContext.clear();
    }

    @Test
    void shouldRejectCrossTenantCandidate() throws Exception {
        OrganizationEntity otherOrganization = organizationRepository.save(new OrganizationEntity(
                "Clinique B",
                "clinique.b@joprelys.local",
                "237611111111",
                "Rue B",
                "Yaoundé"));
        TenantContext.setTenantId(otherOrganization.getId());
        PatientEntity otherTenantPatient = createCanonicalPatient(
                "DPU-OTHER-000001",
                "PAT-OTHER-000001",
                "Nadège Maffock",
                "+237699000111");
        TenantContext.clear();

        mockMvc.perform(post(decisionUrl())
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "reconciliation-cross-tenant-700001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkRequest(otherTenantPatient.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectLegacyDestructiveMergeForUrgTempOrigin() throws Exception {
        mockMvc.perform(post("/api/patients/merge")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "primaryId": "%s",
                                  "secondaryId": "%s"
                                }
                                """.formatted(canonicalPatient.getId(), provisionalPatient.getId())))
                .andExpect(status().isConflict());
    }

    private PatientEntity createVerifiedProvisionalPatient() {
        PatientEntity patient = PatientEntity.provisionalEmergency(
                "DPU-JOP-20260711-700001",
                "PAT-20260711-700001",
                "URG-TEMP-20260711-700001",
                "FEMININ",
                "25-35",
                "Cicatrice légère au sourcil gauche",
                Instant.parse("2026-07-11T10:00:00Z"),
                "Carrefour Akwa",
                IdentityConfidenceLevel.HIGH);
        patient.setFullName("Nadège Maffock");
        patient.setGender("FEMININ");
        patient.setBirthDate(LocalDate.of(1994, 5, 10));
        patient.setPhone("+237699000111");
        patient.setCity("Douala");
        patient.transitionIdentityStatus(PatientIdentityStatus.VERIFIED);
        return patientRepository.save(patient);
    }

    private PatientEntity createCanonicalPatient(
            String globalNumber,
            String localNumber,
            String fullName,
            String phone) {
        return patientRepository.save(new PatientEntity(
                globalNumber,
                localNumber,
                fullName,
                "FEMININ",
                LocalDate.of(1994, 5, 10),
                phone,
                "Douala",
                "Akwa",
                "Rue 10",
                "Paul Maffock",
                "+237699000222",
                "",
                ""));
    }

    private void linkToCanonical(String idempotencyKey, UUID candidateId) throws Exception {
        mockMvc.perform(post(decisionUrl())
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(linkRequest(candidateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("LINK_EXISTING_DPU"))
                .andExpect(jsonPath("$.canonicalPatientId").value(candidateId.toString()))
                .andExpect(jsonPath("$.sourceIdentityStatus").value("MERGED"))
                .andExpect(jsonPath("$.temporaryPatientNumber").value("URG-TEMP-20260711-700001"))
                .andExpect(jsonPath("$.contributingPatientIds.length()").value(2))
                .andExpect(jsonPath("$.replayed").value(false));
    }

    private String decisionUrl() {
        return "/api/patient-reconciliations/" + provisionalPatient.getId() + "/decisions";
    }

    private String linkRequest(UUID candidateId) {
        return """
                {
                  "decision": "LINK_EXISTING_DPU",
                  "candidatePatientId": "%s",
                  "evidenceSourceType": "DOCUMENT",
                  "evidenceReference": "CNI-700001",
                  "justification": "Identité confirmée par CNI originale et déclaration du patient."
                }
                """.formatted(candidateId);
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
