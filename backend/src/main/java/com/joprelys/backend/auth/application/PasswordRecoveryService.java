package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordRecoveryService {

    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Map<String, OtpData> otpMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public PasswordRecoveryService(
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public String generateAndSendOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserAccountEntity user = userAccountRepository.findByEmail(normalizedEmail)
                .filter(UserAccountEntity::isEnabled)
                .orElse(null);

        if (user != null) {
            // Vérification de l'organisation active
            if (user.getOrganizationId() != null) {
                OrganizationEntity org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
                if (org != null && !"ACTIVE".equals(org.getStatus())) {
                    // Organisation inactive, on ne génère pas d'OTP
                    System.out.println("[PASSWORD RECOVERY] Tentative pour e-mail avec organisation inactive : " + normalizedEmail);
                    return null;
                }
            }

            String code = String.format("%06d", random.nextInt(1000000));
            otpMap.put(normalizedEmail, new OtpData(code, clock.instant(), 0));

            // Impression en console pour la simulation
            System.out.println("[PASSWORD RECOVERY] Code de réinitialisation pour " + normalizedEmail + " : " + code);
            return code;
        } else {
            System.out.println("[PASSWORD RECOVERY] Tentative pour e-mail inconnu ou inactif : " + normalizedEmail);
            return null;
        }
    }

    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        OtpData otpData = otpMap.get(normalizedEmail);

        if (otpData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune demande de réinitialisation active.");
        }

        if (otpData.isExpired(clock.instant())) {
            otpMap.remove(normalizedEmail);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code de réinitialisation a expiré.");
        }

        if (otpData.code().equals(otpCode)) {
            UserAccountEntity user = userAccountRepository.findByEmail(normalizedEmail)
                    .filter(UserAccountEntity::isEnabled)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur introuvable ou désactivé."));

            // Validation de l'organisation active au moment de la réinitialisation
            if (user.getOrganizationId() != null) {
                OrganizationEntity org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
                if (org != null && !"ACTIVE".equals(org.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre établissement est désactivé.");
                }
            }

            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userAccountRepository.save(user);
            otpMap.remove(normalizedEmail);
        } else {
            int newAttempts = otpData.attempts() + 1;
            if (newAttempts >= 3) {
                otpMap.remove(normalizedEmail);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trop de tentatives infructueuses. Veuillez régénérer un code.");
            } else {
                otpMap.put(normalizedEmail, new OtpData(otpData.code(), otpData.createdAt(), newAttempts));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code de réinitialisation incorrect.");
            }
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record OtpData(String code, Instant createdAt, int attempts) {
        public boolean isExpired(Instant now) {
            return createdAt.plusSeconds(300).isBefore(now); // 5 minutes
        }
    }
}
