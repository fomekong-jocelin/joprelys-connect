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
			log.info("Propriétés d'initialisation de l'administrateur incomplètes ou désactivées. Initialisation ignorée.");
			return;
		}
		String email = properties.email().trim().toLowerCase(Locale.ROOT);
		
		// 0. Initialisation de l'organisation de test
		OrganizationEntity defaultOrg = organizationRepository.findByEmail("clinique@joprelys.local").orElse(null);
		if (defaultOrg == null) {
			log.info("Création de l'organisation de test 'Clinique Joprelys'...");
			defaultOrg = new OrganizationEntity(
					"Clinique Joprelys",
					"clinique@joprelys.local",
					"+237 600 000 000",
					"Avenue de la Liberté",
					"Douala"
			);
			defaultOrg = organizationRepository.save(defaultOrg);
		}
		final OrganizationEntity orgToUse = defaultOrg;

		// 1. Initialisation de l'Administrateur
		if (userAccountRepository.existsByEmail(email)) {
			log.info("L'administrateur avec l'email '{}' existe déjà en base de données.", email);
		} else {
			log.info("Création de l'administrateur '{}' ({}) en base de données...", properties.name(), email);
			userAccountRepository.save(new UserAccountEntity(
					email,
					properties.name().trim(),
					ADMIN_ROLE,
					passwordEncoder.encode(properties.password())));
			log.info("Administrateur créé avec succès en base de données.");
		}

		// 2. Initialisation du Médecin de test (associé à la clinique de test)
		String medecinEmail = email.replace("admin", "medecin");
		if (userAccountRepository.existsByEmail(medecinEmail)) {
			log.info("Le médecin avec l'email '{}' existe déjà en base de données.", medecinEmail);
			userAccountRepository.findByEmail(medecinEmail).ifPresent(med -> {
				if (med.getOrganizationId() == null) {
					log.info("Mise à jour de l'organisation du médecin existant '{}'...", medecinEmail);
					med.setOrganizationId(orgToUse.getId());
					userAccountRepository.save(med);
				}
			});
		} else {
			String medecinName = (properties.name() != null)
					? properties.name().replace("Administrateur", "Médecin").trim()
					: "Médecin Joprelys";
			log.info("Création du médecin '{}' ({}) en base de données...", medecinName, medecinEmail);
			UserAccountEntity newMedecin = new UserAccountEntity(
					medecinEmail,
					medecinName,
					"MEDECIN",
					passwordEncoder.encode(properties.password()));
			newMedecin.setOrganizationId(orgToUse.getId());
			userAccountRepository.save(newMedecin);
			log.info("Médecin créé avec succès en base de données.");
		}

		// 3. Initialisation du Pharmacien de test (associé à la clinique de test)
		String pharmacienEmail = email.replace("admin", "pharmacien");
		if (userAccountRepository.existsByEmail(pharmacienEmail)) {
			log.info("Le pharmacien avec l'email '{}' existe déjà en base de données.", pharmacienEmail);
			userAccountRepository.findByEmail(pharmacienEmail).ifPresent(pharm -> {
				if (pharm.getOrganizationId() == null) {
					log.info("Mise à jour de l'organisation du pharmacien existant '{}'...", pharmacienEmail);
					pharm.setOrganizationId(orgToUse.getId());
					userAccountRepository.save(pharm);
				}
			});
		} else {
			String pharmacienName = (properties.name() != null)
					? properties.name().replace("Administrateur", "Pharmacien").trim()
					: "Pharmacien Joprelys";
			log.info("Création du pharmacien '{}' ({}) en base de données...", pharmacienName, pharmacienEmail);
			UserAccountEntity newPharmacien = new UserAccountEntity(
					pharmacienEmail,
					pharmacienName,
					"PHARMACIEN",
					passwordEncoder.encode(properties.password()));
			newPharmacien.setOrganizationId(orgToUse.getId());
			userAccountRepository.save(newPharmacien);
			log.info("Pharmacien créé avec succès en base de données.");
		}
	}
}
