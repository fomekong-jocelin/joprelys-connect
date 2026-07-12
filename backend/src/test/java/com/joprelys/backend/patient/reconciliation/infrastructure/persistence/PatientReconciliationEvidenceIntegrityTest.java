package com.joprelys.backend.patient.reconciliation.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PatientReconciliationEvidenceIntegrityTest {

    @Autowired OrganizationRepository organizationRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUpFixtures() {
        TenantContext.clear();
        jdbcTemplate.update("""
                DELETE FROM patient_reconciliation_events
                WHERE idempotency_key IN ('cross-tenant-source', 'cross-tenant-actor')
                """);
        jdbcTemplate.update("""
                DELETE FROM patients
                WHERE global_patient_number IN ('DPU-A-001', 'DPU-B-001')
                """);
        jdbcTemplate.update("""
                DELETE FROM users
                WHERE email IN ('actor.a@joprelys.local', 'actor.b@joprelys.local')
                """);
        jdbcTemplate.update("""
                DELETE FROM organizations
                WHERE email IN (
                    'a@joprelys.local',
                    'b@joprelys.local',
                    'c@joprelys.local',
                    'd@joprelys.local'
                )
                """);
    }

    @Test
    void shouldRejectAnEventWhoseSourcePatientBelongsToAnotherTenant() {
        OrganizationEntity organizationA = createOrganization("Clinique A", "a@joprelys.local");
        OrganizationEntity organizationB = createOrganization("Clinique B", "b@joprelys.local");
        UserAccountEntity actorA = createActor(organizationA, "actor.a@joprelys.local");
        PatientEntity patientB = createPatient(organizationB, "DPU-B-001", "PAT-B-001", "Patient B");

        TenantContext.setTenantId(organizationA.getId());
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertEvent(
                        organizationA.getId(),
                        patientB.getId(),
                        actorA.getId(),
                        "cross-tenant-source"));
    }

    @Test
    void shouldRejectAnEventWhoseActorBelongsToAnotherTenant() {
        OrganizationEntity organizationA = createOrganization("Clinique C", "c@joprelys.local");
        OrganizationEntity organizationB = createOrganization("Clinique D", "d@joprelys.local");
        PatientEntity patientA = createPatient(organizationA, "DPU-A-001", "PAT-A-001", "Patient A");
        UserAccountEntity actorB = createActor(organizationB, "actor.b@joprelys.local");

        TenantContext.setTenantId(organizationA.getId());
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertEvent(
                        organizationA.getId(),
                        patientA.getId(),
                        actorB.getId(),
                        "cross-tenant-actor"));
    }

    private OrganizationEntity createOrganization(String name, String email) {
        return organizationRepository.saveAndFlush(new OrganizationEntity(
                name,
                email,
                "237600000000",
                "Douala",
                "Douala"));
    }

    private UserAccountEntity createActor(OrganizationEntity organization, String email) {
        UserAccountEntity actor = new UserAccountEntity(
                email,
                "Agent de rapprochement",
                "ADMIN_CLINIQUE",
                "hash");
        actor.setOrganizationId(organization.getId());
        return userAccountRepository.saveAndFlush(actor);
    }

    private PatientEntity createPatient(
            OrganizationEntity organization,
            String globalNumber,
            String localNumber,
            String fullName) {
        TenantContext.setTenantId(organization.getId());
        PatientEntity patient = new PatientEntity(
                globalNumber,
                localNumber,
                fullName,
                "FEMININ",
                LocalDate.of(1990, 1, 1),
                "+237600000001",
                "Douala",
                "Akwa",
                "Rue 1",
                "Contact",
                "+237600000002",
                "",
                "");
        return patientRepository.saveAndFlush(patient);
    }

    private void insertEvent(
            UUID organizationId,
            UUID sourcePatientId,
            UUID actorId,
            String idempotencyKey) {
        jdbcTemplate.update("""
                INSERT INTO patient_reconciliation_events (
                    id,
                    organization_id,
                    source_patient_id,
                    candidate_patient_id,
                    decision,
                    previous_identity_status,
                    resulting_identity_status,
                    similarity_score,
                    match_reasons,
                    evidence_source_type,
                    evidence_reference,
                    justification,
                    corrected_event_id,
                    idempotency_key,
                    created_by_user_id,
                    created_at
                ) VALUES (?, ?, ?, NULL, 'DEFER', 'VERIFIED', 'VERIFIED', NULL, NULL,
                          'DOCUMENT', 'TEST-REF', 'Test de contrainte tenant', NULL, ?, ?, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(),
                organizationId,
                sourcePatientId,
                idempotencyKey,
                actorId);
    }
}
