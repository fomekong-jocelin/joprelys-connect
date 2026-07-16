package com.joprelys.backend.auth.application;

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
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final String GENERIC_LOGIN_FAILURE = "Invalid email or password";
    private static final Set<String> SENSITIVE_ROLES = Set.of(
            "ADMIN_JOPRELYS",
            "ADMIN_CLINIQUE",
            "MEDECIN",
            "BIOLOGISTE",
            "PHARMACIEN");

    private final UserAccountRepository userAccountRepository;
    private final AuthAuditEventRepository authAuditEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final IssueAuthSessionUseCase issueAuthSessionUseCase;
    private final Clock clock;
    private final OrganizationRepository organizationRepository;
    private final SecureRandom secureRandom;
    private final Map<String, StaffOtpData> staffOtpMap = new ConcurrentHashMap<>();
    private final AccountMailService accountMailService;

    public AuthenticationService(
            UserAccountRepository userAccountRepository,
            AuthAuditEventRepository authAuditEventRepository,
            PasswordEncoder passwordEncoder,
            IssueAuthSessionUseCase issueAuthSessionUseCase,
            Clock clock,
            OrganizationRepository organizationRepository,
            AccountMailService accountMailService) {
        this.userAccountRepository = userAccountRepository;
        this.authAuditEventRepository = authAuditEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.issueAuthSessionUseCase = issueAuthSessionUseCase;
        this.clock = clock;
        this.organizationRepository = organizationRepository;
        this.accountMailService = accountMailService;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public AuthenticationOutcome login(
            LoginRequest request,
            String auditIpAddress,
            SessionClientMetadata metadata) {
        String email = normalizeEmail(request.email());
        UserAccountEntity user = findValidUser(email, request.password(), auditIpAddress);
        assertOrganizationActive(user, email, auditIpAddress);

        if (hasSensitiveRole(user)) {
            String code = String.format("%06d", secureRandom.nextInt(1_000_000));
            accountMailService.sendLoginCode(user.getEmail(), user.getDisplayName(), code);
            staffOtpMap.put(email, new StaffOtpData(code, clock.instant(), 0));
            return AuthenticationOutcome.otpChallenge(
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole());
        }

        return completeAuthentication(user, auditIpAddress, metadata);
    }

    @Transactional
    public AuthenticationOutcome verifyStaffOtp(
            VerifyStaffOtpRequest request,
            String auditIpAddress,
            SessionClientMetadata metadata) {
        String email = normalizeEmail(request.email());
        validateOtp(email, request.otpCode());
        UserAccountEntity user = userAccountRepository.findByEmail(email)
                .filter(UserAccountEntity::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("Utilisateur introuvable."));
        assertOrganizationActive(user, email, auditIpAddress);
        return completeAuthentication(user, auditIpAddress, metadata);
    }

    private AuthenticationOutcome completeAuthentication(
            UserAccountEntity user,
            String auditIpAddress,
            SessionClientMetadata metadata) {
        user.setLastLoginAt(clock.instant());
        userAccountRepository.save(user);
        IssuedAuthSession session = issueAuthSessionUseCase.issue(user, metadata);
        audit(user.getEmail(), auditIpAddress, true, null);
        return AuthenticationOutcome.authenticated(session);
    }

    private UserAccountEntity findValidUser(
            String email,
            String password,
            String auditIpAddress) {
        UserAccountEntity user = userAccountRepository.findByEmail(email)
                .filter(UserAccountEntity::isEnabled)
                .filter(account -> passwordEncoder.matches(password, account.getPasswordHash()))
                .orElse(null);
        if (user != null) {
            return user;
        }
        audit(email, auditIpAddress, false, "BAD_CREDENTIALS");
        throw new BadCredentialsException(GENERIC_LOGIN_FAILURE);
    }

    private void assertOrganizationActive(
            UserAccountEntity user,
            String email,
            String auditIpAddress) {
        if (user.getOrganizationId() == null) {
            return;
        }
        OrganizationEntity organization = organizationRepository.findById(user.getOrganizationId())
                .orElse(null);
        if (organization != null && "ACTIVE".equals(organization.getStatus())) {
            return;
        }
        audit(email, auditIpAddress, false, "INACTIVE_ORGANIZATION");
        throw new BadCredentialsException("Votre établissement est désactivé.");
    }

    private void validateOtp(String email, String submittedCode) {
        StaffOtpData otpData = staffOtpMap.get(email);
        if (otpData == null) {
            throw new BadCredentialsException("Aucune demande de connexion active pour cet e-mail.");
        }
        if (otpData.isExpired(clock.instant())) {
            staffOtpMap.remove(email);
            throw new BadCredentialsException("Le code de sécurité a expiré.");
        }
        if (otpData.code().equals(submittedCode)) {
            staffOtpMap.remove(email);
            return;
        }
        registerFailedOtpAttempt(email, otpData);
    }

    private void registerFailedOtpAttempt(String email, StaffOtpData otpData) {
        int newAttempts = otpData.attempts() + 1;
        if (newAttempts >= 3) {
            staffOtpMap.remove(email);
            throw new BadCredentialsException("Trop de tentatives infructueuses. Veuillez régénérer un code.");
        }
        staffOtpMap.put(email, new StaffOtpData(otpData.code(), otpData.createdAt(), newAttempts));
        throw new BadCredentialsException("Code de sécurité incorrect.");
    }

    private static boolean hasSensitiveRole(UserAccountEntity user) {
        return java.util.Arrays.stream(user.getRole().split(","))
                .map(String::trim)
                .anyMatch(SENSITIVE_ROLES::contains);
    }

    private void audit(String email, String ipAddress, boolean success, String failureReason) {
        authAuditEventRepository.save(new AuthAuditEventEntity(
                clock.instant(),
                email,
                ipAddress,
                success,
                failureReason));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    String getStaffOtpCodeForTesting(String email) {
        StaffOtpData data = staffOtpMap.get(normalizeEmail(email));
        return data != null ? data.code() : null;
    }

    private record StaffOtpData(String code, Instant createdAt, int attempts) {
        private boolean isExpired(Instant now) {
            return createdAt.plusSeconds(300).isBefore(now);
        }
    }
}
