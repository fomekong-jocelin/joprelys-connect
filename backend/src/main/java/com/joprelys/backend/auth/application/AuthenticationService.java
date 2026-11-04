package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.api.LoginRequest;
import com.joprelys.backend.auth.api.VerifyStaffOtpRequest;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventEntity;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.OtpChallengeStore;
import com.joprelys.backend.auth.session.application.IssueAuthSessionUseCase;
import com.joprelys.backend.auth.session.application.IssuedAuthSession;
import com.joprelys.backend.auth.session.application.SessionClientMetadata;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.notification.application.AccountMailService;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final String GENERIC_LOGIN_FAILURE = "Invalid email or password";
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final String PATIENT_ROLE = "PATIENT";
    /** Mot de passe factice : son hachage égalise le temps de réponse d'un e-mail inconnu (SEC-05). */
    private static final String TIMING_EQUALIZER_SECRET = "joprelys-unknown-account-timing-equalizer";

    private final UserAccountRepository userAccountRepository;
    private final AuthAuditEventRepository authAuditEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final IssueAuthSessionUseCase issueAuthSessionUseCase;
    private final Clock clock;
    private final OrganizationRepository organizationRepository;
    private final SecureRandom secureRandom;
    private final OtpChallengeStore staffOtpChallenges;
    private final AccountMailService accountMailService;
    private volatile String timingEqualizerHash;

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
        this.staffOtpChallenges = new OtpChallengeStore(clock, OTP_TTL, MAX_OTP_ATTEMPTS, Duration.ZERO);
    }

    @Transactional
    public AuthenticationOutcome login(
            LoginRequest request,
            String auditIpAddress,
            SessionClientMetadata metadata) {
        String email = normalizeEmail(request.email());
        UserAccountEntity user = findValidUser(email, request.password(), auditIpAddress);
        assertOrganizationActive(user, email, auditIpAddress);
        assertProfessionalAccount(user, email, auditIpAddress);

        staffOtpChallenges.invalidate(email);
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        accountMailService.sendLoginCode(user.getEmail(), user.getDisplayName(), code);
        staffOtpChallenges.issue(email, code);
        return AuthenticationOutcome.otpChallenge(
                user.getEmail(),
                user.getDisplayName(),
                user.getRole());
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
        assertProfessionalAccount(user, email, auditIpAddress);
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
        Optional<UserAccountEntity> candidate = userAccountRepository.findByEmail(email)
                .filter(UserAccountEntity::isEnabled);
        if (candidate.isPresent() && passwordEncoder.matches(password, candidate.get().getPasswordHash())) {
            return candidate.get();
        }
        if (candidate.isEmpty()) {
            // Même coût BCrypt qu'un compte existant : pas d'énumération par temps de réponse.
            passwordEncoder.matches(password, timingEqualizerHash());
        }
        audit(email, auditIpAddress, false, "BAD_CREDENTIALS");
        throw new BadCredentialsException(GENERIC_LOGIN_FAILURE);
    }

    private String timingEqualizerHash() {
        String hash = timingEqualizerHash;
        if (hash == null) {
            hash = passwordEncoder.encode(TIMING_EQUALIZER_SECRET);
            timingEqualizerHash = hash;
        }
        return hash;
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

    private void assertProfessionalAccount(
            UserAccountEntity user,
            String email,
            String auditIpAddress) {
        if (!hasRole(user, PATIENT_ROLE)) {
            return;
        }
        staffOtpChallenges.invalidate(email);
        audit(email, auditIpAddress, false, "PATIENT_LOGIN_CHANNEL_REQUIRED");
        throw new BadCredentialsException(GENERIC_LOGIN_FAILURE);
    }

    private void validateOtp(String email, String submittedCode) {
        OtpChallengeStore.Verification verification = staffOtpChallenges.verify(email, submittedCode);
        switch (verification.outcome()) {
            case VERIFIED -> {
                // Code valide et consommé atomiquement.
            }
            case NO_ACTIVE_CHALLENGE -> throw new StaffOtpRejectedException(
                    StaffOtpRejectedException.Reason.OTP_NO_ACTIVE_REQUEST,
                    "Aucune demande de connexion active pour cet e-mail.", 0);
            case EXPIRED -> throw new StaffOtpRejectedException(
                    StaffOtpRejectedException.Reason.OTP_EXPIRED,
                    "Le code de sécurité a expiré.", 0);
            case TOO_MANY_ATTEMPTS -> throw new StaffOtpRejectedException(
                    StaffOtpRejectedException.Reason.OTP_TOO_MANY_ATTEMPTS,
                    "Trop de tentatives infructueuses. Veuillez régénérer un code.", 0);
            case INCORRECT -> throw new StaffOtpRejectedException(
                    StaffOtpRejectedException.Reason.OTP_INCORRECT,
                    "Code de sécurité incorrect.", verification.remainingAttempts());
        }
    }

    private static boolean hasRole(UserAccountEntity user, String roleCode) {
        return Arrays.stream(user.getRole().split(","))
                .map(String::trim)
                .anyMatch(roleCode::equals);
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
        return staffOtpChallenges.activeCodeForTesting(normalizeEmail(email)).orElse(null);
    }
}
