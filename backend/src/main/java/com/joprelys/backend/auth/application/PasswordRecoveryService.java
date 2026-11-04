package com.joprelys.backend.auth.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.OtpChallengeStore;
import com.joprelys.backend.auth.session.application.RevokeAllUserSessionsUseCase;
import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.notification.application.AccountMailService;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordRecoveryService {

    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpChallengeStore recoveryChallenges;
    private final SecureRandom random = new SecureRandom();
    private final AccountMailService accountMailService;
    private final RevokeAllUserSessionsUseCase revokeAllUserSessions;
    private final AuditService auditService;

    public PasswordRecoveryService(
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            AccountMailService accountMailService,
            RevokeAllUserSessionsUseCase revokeAllUserSessions,
            AuditService auditService) {
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountMailService = accountMailService;
        this.revokeAllUserSessions = revokeAllUserSessions;
        this.auditService = auditService;
        this.recoveryChallenges = new OtpChallengeStore(clock, OTP_TTL, MAX_OTP_ATTEMPTS, Duration.ZERO);
    }

    public String generateAndSendOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserAccountEntity user = userAccountRepository.findByEmail(normalizedEmail)
                .filter(UserAccountEntity::isEnabled)
                .orElse(null);
        if (user == null || !organizationActive(user)) {
            return null;
        }

        String code = String.format("%06d", random.nextInt(1000000));
        accountMailService.sendPasswordRecoveryCode(user.getEmail(), user.getDisplayName(), code);
        recoveryChallenges.issue(normalizedEmail, code);
        return code;
    }

    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        OtpChallengeStore.Verification verification = recoveryChallenges.verify(normalizedEmail, otpCode);
        switch (verification.outcome()) {
            case VERIFIED -> applyNewPassword(normalizedEmail, newPassword);
            case NO_ACTIVE_CHALLENGE -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Aucune demande de réinitialisation active.");
            case EXPIRED -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Le code de réinitialisation a expiré.");
            case TOO_MANY_ATTEMPTS -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Trop de tentatives infructueuses. Veuillez régénérer un code.");
            case INCORRECT -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Code de réinitialisation incorrect.");
        }
    }

    /**
     * Le changement de mot de passe invalide toutes les sessions existantes : un attaquant
     * déjà connecté ne doit pas survivre à la reprise du compte par son titulaire (SEC-16).
     */
    private void applyNewPassword(String normalizedEmail, String newPassword) {
        UserAccountEntity user = userAccountRepository.findByEmail(normalizedEmail)
                .filter(UserAccountEntity::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur introuvable ou désactivé."));
        if (!organizationActive(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre établissement est désactivé.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountRepository.save(user);
        int revokedSessions = revokeAllUserSessions.revokeAllForUser(
                user.getId(), AuthSessionRevocationReason.PASSWORD_RESET);
        auditService.logSuccess(
                user.getId(),
                user.getOrganizationId(),
                null,
                "USER_ACCOUNT",
                user.getId(),
                "PASSWORD_RESET",
                "Mot de passe réinitialisé par code de récupération ; sessions révoquées : " + revokedSessions);
    }

    private boolean organizationActive(UserAccountEntity user) {
        if (user.getOrganizationId() == null) {
            return true;
        }
        OrganizationEntity org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
        return org == null || "ACTIVE".equals(org.getStatus());
    }

    private static String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
