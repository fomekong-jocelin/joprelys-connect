package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.config.SeedAdminProperties;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "joprelys.seed", name = "admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);
    private static final String ADMIN_ROLE = "ADMIN_JOPRELYS";

    private final SeedAdminProperties properties;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserSeeder(
            SeedAdminProperties properties,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!properties.isComplete()) {
            log.info("Propriétés d'initialisation de l'administrateur désactivées. Initialisation ignorée.");
            return;
        }

        String email = valueOrDefault(properties.email(), "admin@joprelys.local").toLowerCase(Locale.ROOT);
        String name = valueOrDefault(properties.name(), "Administrateur Joprelys");
        String password = passwordOrDefault(properties.password());

        if (userAccountRepository.existsByEmail(email)) {
            log.info("L'administrateur avec l'email '{}' existe déjà en base de données.", email);
            return;
        }

        log.info("Création de l'administrateur '{}' ({}) en base de données...", name, email);
        userAccountRepository.save(new UserAccountEntity(
                email,
                name,
                ADMIN_ROLE,
                passwordEncoder.encode(password)));
        log.info("Administrateur créé avec succès en base de données.");
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value.trim() : defaultValue;
    }

    private String passwordOrDefault(String value) {
        return value != null && !value.isBlank() ? value : "Re12#He10@2021!";
    }
}
