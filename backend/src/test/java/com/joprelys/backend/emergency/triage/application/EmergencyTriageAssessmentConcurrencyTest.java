package com.joprelys.backend.emergency.triage.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.emergency.api.CreateEmergencyRequest;
import com.joprelys.backend.emergency.application.EmergencyService;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.AirwayStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.BreathingStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.CirculationStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.DisabilityStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.ExposureStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.RecommendedOrientation;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.math.BigDecimal;
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
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EmergencyTriageAssessmentConcurrencyTest {

    @Autowired EmergencyService emergencyService;
    @Autowired EmergencyTriageAssessmentUseCase triageUseCase;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private OrganizationEntity organization;
    private UserAccountEntity actor;
    private EmergencyEntity emergency;

    @BeforeEach
    void setUp() {
        cleanup();
        organization = organizationRepository.save(new OrganizationEntity(
                "Clinique triage concurrence",
                "triage-concurrency@joprelys.local",
                "237600000001",
                "Douala",
                "Douala"));
        actor = new UserAccountEntity(
                "triage.concurrent@joprelys.local",
                "Médecin concurrence",
                "MEDECIN",
                "hash");
        actor.setOrganizationId(organization.getId());
        actor = userAccountRepository.save(actor);

        TenantContext.setTenantId(organization.getId());
        PatientEntity patient = patientRepository.save(new PatientEntity(
                "DPU-TRIAGE-CONCURRENT",
                "PAT-TRIAGE-CONCURRENT",
                "Patient concurrence",
                "FEMININ",
                LocalDate.of(1992, 7, 10),
                "+237699000300",
                "Douala",
                "",
                "",
                "",
                "",
                "",
                ""));
        emergency = emergencyService.createEmergency(
                new CreateEmergencyRequest(
                        patient.getId(),
                        "AMBULANCE",
                        "RED",
                        "SHOCK",
                        "Détresse respiratoire",
                        86,
                        52,
                        130,
                        BigDecimal.valueOf(37.6)),
                actor.getId());
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        cleanup();
    }

    @Test
    void shouldKeepBothConcurrentReassessmentsWithDistinctSequences() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(reassessment(ready, start, "Première observation"));
            Future<Boolean> second = executor.submit(reassessment(ready, start, "Seconde observation"));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(first.get(15, TimeUnit.SECONDS));
            assertTrue(second.get(15, TimeUnit.SECONDS));

            TenantContext.setTenantId(organization.getId());
            List<EmergencyTriageAssessmentResult> history = triageUseCase.getHistory(
                    emergency.getId(),
                    actor.getId());
            assertEquals(3, history.size());
            assertEquals(List.of(1, 2, 3), history.stream()
                    .map(EmergencyTriageAssessmentResult::sequenceNumber)
                    .toList());
            assertEquals(2, history.stream()
                    .filter(item -> item.assessmentType().name().equals("REASSESSMENT"))
                    .count());
        } finally {
            TenantContext.clear();
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> reassessment(
            CountDownLatch ready,
            CountDownLatch start,
            String notes) {
        return () -> {
            TenantContext.setTenantId(organization.getId());
            ready.countDown();
            try {
                start.await(5, TimeUnit.SECONDS);
                triageUseCase.addReassessment(
                        emergency.getId(),
                        command(notes),
                        actor.getId());
                return true;
            } finally {
                TenantContext.clear();
            }
        };
    }

    private EmergencyTriageAssessmentCommand command(String notes) {
        return new EmergencyTriageAssessmentCommand(
                "ORANGE",
                "UNSTABLE",
                AirwayStatus.PATENT,
                BreathingStatus.DISTRESS,
                CirculationStatus.COMPROMISED,
                DisabilityStatus.ALERT,
                ExposureStatus.NO_CRITICAL_FINDING,
                94,
                60,
                116,
                27,
                95,
                BigDecimal.valueOf(37.3),
                15,
                5,
                RecommendedOrientation.RESUSCITATION,
                notes,
                null);
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM emergency_admission_requests");
        jdbcTemplate.update("DELETE FROM resuscitation_logs");
        jdbcTemplate.update("DELETE FROM emergencies");
        jdbcTemplate.update("DELETE FROM patient_identity_declarations");
        jdbcTemplate.update("DELETE FROM patient_identity_status_history");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();
    }
}