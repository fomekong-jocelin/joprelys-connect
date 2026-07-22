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
@ConditionalOnProperty(prefix = "joprelys.seed.admin", name = "enabled", havingValue = "true", matchIfMissing = false)
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
            throw new IllegalStateException(
                    "Configuration du bootstrap administrateur incomplète : email, nom et mot de passe sont obligatoires.");
        }

        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        String name = properties.name().trim();
        String password = properties.password();

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
}
