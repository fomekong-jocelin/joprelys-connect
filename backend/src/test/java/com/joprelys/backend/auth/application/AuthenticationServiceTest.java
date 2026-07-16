package com.joprelys.backend.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.api.LoginRequest;
import com.joprelys.backend.auth.api.VerifyStaffOtpRequest;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventEntity;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.session.application.IssueAuthSessionUseCase;
import com.joprelys.backend.auth.session.application.IssuedAuthSession;
import com.joprelys.backend.auth.session.application.SessionClientMetadata;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.notification.application.AccountMailService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-02T10:00:00Z"), ZoneOffset.UTC);
    private static final SessionClientMetadata METADATA = new SessionClientMetadata(
            "WEB", "JUnit", "127.0.0.0/24");

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AuthAuditEventRepository authAuditEventRepository;
    @Mock
    private AccountMailService accountMailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IssueAuthSessionUseCase issueAuthSessionUseCase;
    @Mock
    private OrganizationRepository organizationRepository;

    @Test
    void shouldReturnSessionAndAuditSuccessWhenCredentialsAreValid() {
        UserAccountEntity user = user("agent@example.com", "Agent Accueil", "AGENT_ACCUEIL");
        AuthenticationService service = service();
        when(userAccountRepository.findByEmail("agent@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
        when(issueAuthSessionUseCase.issue(eq(user), any())).thenReturn(issued(user));

        AuthenticationOutcome outcome = service.login(
                new LoginRequest("Agent@Example.com", "Password123!"),
                "127.0.0.1",
                METADATA);

        assertEquals("jwt-token", outcome.session().accessToken());
        assertEquals("agent@example.com", outcome.email());
        assertEquals("Agent Accueil", outcome.displayName());
        assertEquals("AGENT_ACCUEIL", outcome.role());
        verify(authAuditEventRepository).save(any(AuthAuditEventEntity.class));
    }

    @Test
    void shouldAuditAndReturnGenericErrorWhenCredentialsAreInvalid() {
        AuthenticationService service = service();
        when(userAccountRepository.findByEmail("agent@example.com")).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> service.login(
                        new LoginRequest("agent@example.com", "wrong"),
                        "127.0.0.1",
                        METADATA));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(authAuditEventRepository).save(any(AuthAuditEventEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenOrganizationIsInactive() {
        UserAccountEntity user = user("agent@example.com", "Agent Accueil", "AGENT_ACCUEIL");
        UUID orgId = UUID.randomUUID();
        user.setOrganizationId(orgId);
        OrganizationEntity organization = new OrganizationEntity(
                "Espoir", "espoir@joprelys.local", "123", "street", "Douala");
        organization.setStatus("INACTIVE");
        AuthenticationService service = service();
        when(userAccountRepository.findByEmail("agent@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> service.login(
                        new LoginRequest("agent@example.com", "Password123!"),
                        "127.0.0.1",
                        METADATA));

        assertEquals("Votre établissement est désactivé.", exception.getMessage());
        verify(authAuditEventRepository).save(any(AuthAuditEventEntity.class));
    }

    @Test
    void shouldRequireOtpWithoutCreatingSessionWhenUserIsSensitive() {
        UserAccountEntity user = user("medecin@example.com", "Medecin Test", "MEDECIN");
        AuthenticationService service = service();
        when(userAccountRepository.findByEmail("medecin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);

        AuthenticationOutcome outcome = service.login(
                new LoginRequest("medecin@example.com", "Password123!"),
                "127.0.0.1",
                METADATA);

        assertEquals(true, outcome.requiresOtp());
        assertNull(outcome.session());
    }

    @Test
    void shouldCreateSessionAfterSuccessfulOtpVerification() {
        UserAccountEntity user = user("medecin@example.com", "Medecin Test", "MEDECIN");
        AuthenticationService service = service();
        when(userAccountRepository.findByEmail("medecin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
        when(issueAuthSessionUseCase.issue(eq(user), any())).thenReturn(issued(user));

        AuthenticationOutcome initial = service.login(
                new LoginRequest("medecin@example.com", "Password123!"),
                "127.0.0.1",
                METADATA);
        assertEquals(true, initial.requiresOtp());
        String otpCode = service.getStaffOtpCodeForTesting("medecin@example.com");

        AuthenticationOutcome authenticated = service.verifyStaffOtp(
                new VerifyStaffOtpRequest("medecin@example.com", otpCode),
                "127.0.0.1",
                METADATA);

        assertEquals("jwt-token", authenticated.session().accessToken());
        assertEquals("medecin@example.com", authenticated.email());
    }

    @Test
    void shouldFailOtpVerificationWhenOtpIsIncorrect() {
        UserAccountEntity user = user("medecin@example.com", "Medecin Test", "MEDECIN");
        AuthenticationService service = service();
        when(userAccountRepository.findByEmail("medecin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
        service.login(
                new LoginRequest("medecin@example.com", "Password123!"),
                "127.0.0.1",
                METADATA);

        assertThrows(
                BadCredentialsException.class,
                () -> service.verifyStaffOtp(
                        new VerifyStaffOtpRequest("medecin@example.com", "000000"),
                        "127.0.0.1",
                        METADATA));
    }

    private AuthenticationService service() {
        return new AuthenticationService(
                userAccountRepository,
                authAuditEventRepository,
                passwordEncoder,
                issueAuthSessionUseCase,
                FIXED_CLOCK,
                organizationRepository,
                accountMailService);
    }

    private static UserAccountEntity user(String email, String name, String role) {
        return new UserAccountEntity(email, name, role, "hash");
    }

    private static IssuedAuthSession issued(UserAccountEntity user) {
        return new IssuedAuthSession(
                "jwt-token",
                Instant.parse("2026-07-02T10:15:00Z"),
                UUID.randomUUID(),
                Instant.parse("2026-07-02T10:30:00Z"),
                "refresh-token",
                user.getEmail(),
                user.getDisplayName(),
                user.getRole());
    }
}
