package com.joprelys.backend.patient.reconciliation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.IdentitySourceType;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationDecisionRequest;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PatientReconciliationConcurrencyTest {

    @Autowired PatientReconciliationWorkflowService workflowService;
    @Autowired LegacyPatientMergeCoordinator legacyMergeCoordinator;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private OrganizationEntity organization;
    private UserAccountEntity actor;
    private PatientEntity source;
    private PatientEntity canonicalCandidate;
    private PatientEntity otherLegacyPatient;

    @BeforeEach
    void setUp() {
        cleanup();
        organization = organizationRepository.save(new OrganizationEntity(
                "Clinique concurrence",
                "concurrency@joprelys.local",
                "237600000000",
                "Douala",
                "Douala"));
        actor = new UserAccountEntity(
                "concurrency.reviewer@joprelys.local",
                "Agent concurrence",
                "ADMIN_CLINIQUE",
                "hash");
        actor.setOrganizationId(organization.getId());
        actor = userAccountRepository.save(actor);

        TenantContext.setTenantId(organization.getId());
        source = createVerifiedUrgTemp();
        canonicalCandidate = createCanonicalPatient(
                "DPU-CONCURRENCY-0001",
                "PAT-CONCURRENCY-0001",
                "Nadège Maffock");
        otherLegacyPatient = createCanonicalPatient(
                "DPU-CONCURRENCY-0002",
                "PAT-CONCURRENCY-0002",
                "Autre patient");
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        cleanup();
    }

    @Test
    void shouldAllowOnlyOneOfLegacyMergeOrCanonicalReconciliationToWin() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<OperationOutcome> reconciliation = executor.submit(operation(ready, start, () -> {
                authenticateActor();
                workflowService.decide(
                        source.getId(),
                        new PatientReconciliationDecisionRequest(
                                PatientReconciliationDecision.LINK_EXISTING_DPU,
                                canonicalCandidate.getId(),
                                IdentitySourceType.DOCUMENT,
                                "CNI-CONCURRENCY",
                                "Identité confirmée pendant le test concurrent."),
                        "concurrent-reconciliation");
            }));

            Future<OperationOutcome> legacyMerge = executor.submit(operation(ready, start, () ->
                    legacyMergeCoordinator.merge(
                            otherLegacyPatient.getId(),
                            canonicalCandidate.getId(),
                            actor.getId())));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<OperationOutcome> outcomes = List.of(
                    reconciliation.get(15, TimeUnit.SECONDS),
                    legacyMerge.get(15, TimeUnit.SECONDS));
            long successes = outcomes.stream().filter(OperationOutcome::success).count();
            long rejected = outcomes.stream().filter(outcome -> !outcome.success()).count();

            assertEquals(1, successes, () -> "Une seule opération doit gagner : " + outcomes);
            assertEquals(1, rejected, () -> "L’opération concurrente doit être rejetée : " + outcomes);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<OperationOutcome> operation(
            CountDownLatch ready,
            CountDownLatch start,
            CheckedOperation operation) {
        return () -> {
            TenantContext.setTenantId(organization.getId());
            ready.countDown();
            try {
                start.await(5, TimeUnit.SECONDS);
                operation.run();
                return new OperationOutcome(true, null);
            } catch (Exception exception) {
                return new OperationOutcome(false, rootMessage(exception));
            } finally {
                SecurityContextHolder.clearContext();
                TenantContext.clear();
            }
        };
    }

    private void authenticateActor() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor.getEmail(), "n/a", List.of()));
    }

    private PatientEntity createVerifiedUrgTemp() {
        PatientEntity patient = PatientEntity.provisionalEmergency(
                "DPU-CONCURRENCY-SOURCE",
                "PAT-CONCURRENCY-SOURCE",
                "URG-TEMP-20260712-CONCURRENCY",
                "FEMININ",
                "25-35",
                "Patiente consciente",
                Instant.parse("2026-07-12T08:00:00Z"),
                "Douala",
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
            String fullName) {
        return patientRepository.save(new PatientEntity(
                globalNumber,
                localNumber,
                fullName,
                "FEMININ",
                LocalDate.of(1994, 5, 10),
                "+237699000111",
                "Douala",
                "Akwa",
                "Rue 1",
                "Contact",
                "+237699000222",
                "",
                ""));
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM patient_canonical_links");
        jdbcTemplate.update("DELETE FROM patient_identity_aliases");
        jdbcTemplate.update("DELETE FROM patient_reconciliation_events");
        jdbcTemplate.update("DELETE FROM patient_merged_history");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM patient_duplicate_candidates");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }

    @FunctionalInterface
    private interface CheckedOperation {
        void run() throws Exception;
    }

    private record OperationOutcome(boolean success, String error) {
    }
}
