package com.joprelys.backend.auth.session.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.JwtRevocationService;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientLegacyLogoutIsolationTest {

    private static final Instant NOW = Instant.parse("2026-07-26T10:00:00Z");

    @Mock AuthSessionRepository sessionRepository;
    @Mock UserAccountRepository userRepository;
    @Mock AuthSessionAuditPort auditPort;
    @Mock JwtRevocationService jwtRevocationService;

    private AuthSessionManagementService service;

    @BeforeEach
    void setUp() {
        service = new AuthSessionManagementService(
                sessionRepository,
                userRepository,
                auditPort,
                jwtRevocationService,
                new AuthSessionViewMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRevokePatientJtiWithoutTreatingPatientNumberAsStaffUserId() {
        JwtClaims patientClaims = new JwtClaims(
                "DPU-2026-000001",
                "DPU-2026-000001",
                "Patient Test",
                "PATIENT",
                UUID.randomUUID().toString(),
                "patient-jti",
                "",
                NOW.plusSeconds(900));
        when(jwtRevocationService.revoke(patientClaims, null, "LOGOUT")).thenReturn(true);

        service.logout(patientClaims);

        verify(jwtRevocationService).revoke(patientClaims, null, "LOGOUT");
        verify(auditPort, never()).append(org.mockito.ArgumentMatchers.any());
    }
}
