package com.joprelys.backend.clinic.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.notification.application.AccountMailService;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateClinicAdminService {

    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TEMPORARY_PASSWORD_LENGTH = 6;

    private final OrganizationRepository organizationRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountMailService accountMailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public CreateClinicAdminService(
            OrganizationRepository organizationRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AccountMailService accountMailService) {
        this.organizationRepository = organizationRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountMailService = accountMailService;
    }

    @Transactional
    public CreatedClinicAdmin create(UUID organizationId, String displayName, String requestedEmail) {
        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée."));
        String email = requestedEmail.trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur avec cet e-mail existe déjà.");
        }

        String temporaryPassword = generateTemporaryPassword();
        UserAccountEntity admin = new UserAccountEntity(
                email,
                displayName.trim(),
                "ADMIN_CLINIQUE",
                passwordEncoder.encode(temporaryPassword));
        admin.setOrganizationId(organization.getId());
        UserAccountEntity saved = userAccountRepository.save(admin);

        accountMailService.sendTemporaryPassword(saved.getEmail(), saved.getDisplayName(), temporaryPassword);
        return new CreatedClinicAdmin(saved, organization.getId());
    }

    private String generateTemporaryPassword() {
        StringBuilder suffix = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (int index = 0; index < TEMPORARY_PASSWORD_LENGTH; index++) {
            suffix.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
        }
        return "Jop-" + suffix;
    }

    public record CreatedClinicAdmin(UserAccountEntity account, UUID organizationId) {
    }
}
