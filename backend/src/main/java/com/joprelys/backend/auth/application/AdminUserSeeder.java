package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.config.SeedAdminProperties;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "joprelys.seed", name = "admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);
	private static final String ADMIN_ROLE = "ADMIN_JOPRELYS";

	private final SeedAdminProperties properties;
	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final OrganizationRepository organizationRepository;

	public AdminUserSeeder(
			SeedAdminProperties properties,
			UserAccountRepository userAccountRepository,
			PasswordEncoder passwordEncoder,
			OrganizationRepository organizationRepository) {
		this.properties = properties;
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.organizationRepository = organizationRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (!properties.isComplete()) {
			log.info("Propriétés d'initialisation de l'administrateur désactivées. Initialisation ignorée.");
			return;
		}

		String email = (properties.email() != null && !properties.email().isBlank())
				? properties.email().trim().toLowerCase(Locale.ROOT)
				: "admin@joprelys.local";

		String name = (properties.name() != null && !properties.name().isBlank())
				? properties.name().trim()
				: "Administrateur Joprelys";

		String password = (properties.password() != null && !properties.password().isBlank())
				? properties.password()
				: "Re12#He10@2021!";

		// 1. Initialisation de l'Administrateur
		if (userAccountRepository.existsByEmail(email)) {
			log.info("L'administrateur avec l'email '{}' existe déjà en base de données.", email);
		} else {
			log.info("Création de l'administrateur '{}' ({}) en base de données...", name, email);
			userAccountRepository.save(new UserAccountEntity(
					email,
					name,
					ADMIN_ROLE,
					passwordEncoder.encode(password)));
			log.info("Administrateur créé avec succès en base de données.");
		}
	}
}
