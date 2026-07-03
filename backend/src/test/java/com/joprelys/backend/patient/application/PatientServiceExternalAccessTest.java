package com.joprelys.backend.patient.application;

import static org.junit.jupiter.api.Assertions.*;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.*;
import com.joprelys.backend.audit.infrastructure.persistence.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
public class PatientServiceExternalAccessTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ExternalAccessRequestRepository externalAccessRequestRepository;

    @Autowired
    private EmergencyAccessAuthorizationRepository emergencyAccessAuthorizationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ExternalAccessExpirationScheduler scheduler;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserAccountEntity doctorA; // belongs to orgA
    private PatientEntity patientB; // patient belonging to orgB

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM emergency_access_authorizations");
        jdbcTemplate.update("DELETE FROM patient_consents");
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM external_access_requests");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Create Organization A
        orgA = new OrganizationEntity("Clinique A", "clinique.a@joprelys.local", "123456", "Street A", "Douala");
        orgA = organizationRepository.save(orgA);

        // 2. Create Organization B
        orgB = new OrganizationEntity("Clinique B", "clinique.b@joprelys.local", "789012", "Street B", "Yaounde");
        orgB = organizationRepository.save(orgB);

        // 3. Create Doctor in Org A
        doctorA = new UserAccountEntity("doctor.a@joprelys.local", "Dr. House", "MEDECIN", "passhash");
        doctorA.setOrganizationId(orgA.getId());
        doctorA = userAccountRepository.save(doctorA);

        // Authenticate SecurityContext with Doctor A
        var auth = new UsernamePasswordAuthenticationToken(doctorA.getEmail(), null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 4. Create Patient in Org B
        TenantContext.setTenantId(orgB.getId());
        patientB = new PatientEntity(
                "DPU-JOP-20260704-22222",
                "PAT-22222",
                "Patient Externe Org B",
                "FEMININ",
                LocalDate.of(1995, 8, 20),
                "+237699999992",
                "Yaounde",
                null, null, null, null, null, null
        );
        patientB.setOrganizationId(orgB.getId());
        patientB = patientRepository.save(patientB);

        TenantContext.clear();
    }

    @Test
    void givenExternalPatient_whenNoConsent_thenThrowConsentRequired() {
        // Doctor A tries to access Patient B's file
        TenantContext.setTenantId(orgA.getId());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            patientService.getPatientById(patientB.getId());
        });
        assertEquals(403, ex.getStatusCode().value());
        assertEquals("CONSENT_REQUIRED", ex.getReason());
    }

    @Test
    void givenExternalPatient_whenApprovedExternalAccessActive_thenAllowAccess() {
        // Create an approved and valid access request from Org A for Patient B
        TenantContext.setTenantId(orgB.getId());
        var request = new ExternalAccessRequestEntity(
                patientB.getId(),
                doctorA.getId(),
                orgA.getId(),
                "Consultation cardiologique externe urgente",
                24
        );
        request.setStatus("APPROUVEE");
        request.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));
        externalAccessRequestRepository.save(request);

        // Doctor A tries to access Patient B's file (should be allowed)
        TenantContext.setTenantId(orgA.getId());
        PatientEntity retrieved = patientService.getPatientById(patientB.getId());
        assertNotNull(retrieved);
        assertEquals(patientB.getId(), retrieved.getId());
    }

    @Test
    void givenExternalPatient_whenApprovedExternalAccessExpired_thenThrowConsentRequired() {
        // Create an expired approved access request from Org A for Patient B
        TenantContext.setTenantId(orgB.getId());
        var request = new ExternalAccessRequestEntity(
                patientB.getId(),
                doctorA.getId(),
                orgA.getId(),
                "Consultation cardiologique externe urgente",
                24
        );
        request.setStatus("APPROUVEE");
        request.setExpiresAt(Instant.now().minus(Duration.ofMinutes(1)));
        externalAccessRequestRepository.save(request);

        // Doctor A tries to access Patient B's file (should fail because expired)
        TenantContext.setTenantId(orgA.getId());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            patientService.getPatientById(patientB.getId());
        });
        assertEquals(403, ex.getStatusCode().value());
        assertEquals("CONSENT_REQUIRED", ex.getReason());
    }

    @Test
    void givenExternalPatient_whenEmergencyAccessTriggered_thenAllowAccessAndLogEmergencyDpuAccess() {
        TenantContext.setTenantId(orgA.getId());

        // Trigger emergency access for Patient B
        patientService.triggerEmergencyAccess(patientB.getId(), "Suspicion d'infarctus");

        // Doctor A should now be able to retrieve patient B details
        PatientEntity retrieved = patientService.getPatientById(patientB.getId());
        assertNotNull(retrieved);
        assertEquals(patientB.getId(), retrieved.getId());

        // Verify that critical EMERGENCY_DPU_ACCESS audit log is generated
        var logs = auditLogRepository.findByPatientIdOrderByCreatedAtDesc(patientB.getId());
        assertFalse(logs.isEmpty());
        var emergencyLog = logs.stream()
                .filter(l -> "EMERGENCY_DPU_ACCESS".equals(l.getAction()))
                .findFirst();
        assertTrue(emergencyLog.isPresent());
        assertEquals("Accès d'urgence Brise-Glace activé. Motif : Suspicion d'infarctus", emergencyLog.get().getReason());
    }

    @Test
    void whenSchedulerRuns_thenExpireRequestsPastExpirationDate() {
        TenantContext.setTenantId(orgB.getId());
        var request = new ExternalAccessRequestEntity(
                patientB.getId(),
                doctorA.getId(),
                orgA.getId(),
                "Consultation cardiologique",
                24
        );
        request.setStatus("APPROUVEE");
        request.setExpiresAt(Instant.now().minus(Duration.ofHours(1)));
        externalAccessRequestRepository.save(request);

        // Run the scheduler
        scheduler.expireAccessRequests();

        // Check request status transitioned to EXPIREE
        var updated = externalAccessRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals("EXPIREE", updated.getStatus());
    }
}
