package com.joprelys.backend.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.api.LoginRequest;
import com.joprelys.backend.auth.api.VerifyStaffOtpRequest;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventEntity;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.rbac.RbacCatalog;
import com.joprelys.backend.auth.session.application.IssueAuthSessionUseCase;
import com.joprelys.backend.auth.session.application.IssuedAuthSession;
import com.joprelys.backend.auth.session.application.SessionClientMetadata;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.notification.application.AccountMailService;
import com.joprelys.backend.notification.application.MailDeliveryUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
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
    void shouldRequireOtpForEverySystemProfessionalRole() {
        AuthenticationService service = service();
        when(passwordEncoder.matches(eq("Password123!"), anyString())).thenReturn(true);

        RbacCatalog.systemRoles().stream()
                .filter(role -> !"PATIENT".equals(role.code()))
                .forEach(role -> {
                    String email = role.code().toLowerCase(Locale.ROOT) + "@example.com";
                    UserAccountEntity user = user(email, role.name(), role.code());
                    when(userAccountRepository.findByEmail(email)).thenReturn(Optional.of(user));

                    AuthenticationOutcome outcome = service.login(
                            new LoginRequest(email, "Password123!"),
                            "127.0.0.1",
                            METADATA);

                    assertTrue(outcome.requiresOtp(), role.code());
                    assertNull(outcome.session(), role.code());
                    assertEquals(role.code(), outcome.role());
                });
    }

    @Test
    void shouldRequireOtpForCustomProfessionalRole() {
        UserAccountEntity user = user("custom@example.com", "Profil personnalisé", "SUPERVISEUR_CAISSE");
        AuthenticationService service = service();
        when(userAccountRepository.findByEmail("custom@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);

        AuthenticationOutcome outcome = service.login(
                new LoginRequest("custom@example.com", "Password123!"),
                "127.0.0.1",
                METADATA);

        assertTrue(outcome.requiresOtp());
        assertNull(outcome.session());
        assertEquals("SUPERVISEUR_CAISSE", outcome.role());
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
    void shouldRejectPatientAndMixedPatientAccountsFromStaffLogin() {
        AuthenticationService service = service();
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);

        for (String role : new String[]{"PATIENT", "PATIENT,MEDECIN"}) {
            String email = role.replace(',', '-').toLowerCase(Locale.ROOT) + "@example.com";
            UserAccountEntity user = user(email, "Patient Test", role);
            when(userAccountRepository.findByEmail(email)).thenReturn(Optional.of(user));

            BadCredentialsException exception = assertThrows(
                    BadCredentialsException.class,
                    () -> service.login(
                            new LoginRequest(email, "Password123!"),
                            "127.0.0.1",
                            METADATA));

            assertEquals("Invalid email or password", exception.getMessage());
            assertNull(service.getStaffOtpCodeForTesting(email));
        }
    }

    @Test
    void shouldCreateSessionAfterSuccessfulOtpVerificationForCashier() {
        UserAccountEntity user = user("cashier@example.com", "Caissier Test", "CAISSIER");
        AuthenticationService service = service();
        when(userAccountRepository.findByEmail("cashier@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
        when(issueAuthSessionUseCase.issue(eq(user), any())).thenReturn(issued(user));

        AuthenticationOutcome initial = service.login(
                new LoginRequest("cashier@example.com", "Password123!"),
                "127.0.0.1",
                METADATA);
        assertTrue(initial.requiresOtp());
        String otpCode = service.getStaffOtpCodeForTesting("cashier@example.com");

        AuthenticationOutcome authenticated = service.verifyStaffOtp(
                new VerifyStaffOtpRequest("cashier@example.com", otpCode),
                "127.0.0.1",
                METADATA);

        assertEquals("jwt-token", authenticated.session().accessToken());
        assertEquals("cashier@example.com", authenticated.email());
    }

    @Test
    void shouldFailOtpVerificationWhenOtpIsIncorrect() {
        UserAccountEntity user = user("cashier@example.com", "Caissier Test", "CAISSIER");
        AuthenticationService service = service();
        when(userAccountRepository.findByEmail("cashier@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
        service.login(
                new LoginRequest("cashier@example.com", "Password123!"),
                "127.0.0.1",
                METADATA);

        assertThrows(
                BadCredentialsException.class,
                () -> service.verifyStaffOtp(
                        new VerifyStaffOtpRequest("cashier@example.com", "000000"),
                        "127.0.0.1",
                        METADATA));
    }

    @Test
    void shouldInvalidatePreviousOtpWhenAReplacementCodeCannotBeDelivered() {
        UserAccountEntity user = user("cashier@example.com", "Caissier Test", "CAISSIER");
        AuthenticationService service = service();
        when(userAccountRepository.findByEmail("cashier@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);

        service.login(
                new LoginRequest("cashier@example.com", "Password123!"),
                "127.0.0.1",
                METADATA);
        assertTrue(service.getStaffOtpCodeForTesting("cashier@example.com") != null);

        doThrow(new MailDeliveryUnavailableException(new RuntimeException("smtp unavailable")))
                .when(accountMailService)
                .sendLoginCode(anyString(), anyString(), anyString());

        assertThrows(
                MailDeliveryUnavailableException.class,
                () -> service.login(
                        new LoginRequest("cashier@example.com", "Password123!"),
                        "127.0.0.1",
                        METADATA));
        assertNull(service.getStaffOtpCodeForTesting("cashier@example.com"));
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
