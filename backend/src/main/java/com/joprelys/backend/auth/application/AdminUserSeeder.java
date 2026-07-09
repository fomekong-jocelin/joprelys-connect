package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.config.SeedAdminProperties;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.auth.security.TenantContext;
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
	private final com.joprelys.backend.spatial.infrastructure.persistence.WardRepository wardRepository;
	private final com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository roomRepository;
	private final com.joprelys.backend.spatial.infrastructure.persistence.BedRepository bedRepository;

	public AdminUserSeeder(
			SeedAdminProperties properties,
			UserAccountRepository userAccountRepository,
			PasswordEncoder passwordEncoder,
			OrganizationRepository organizationRepository,
			com.joprelys.backend.spatial.infrastructure.persistence.WardRepository wardRepository,
			com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository roomRepository,
			com.joprelys.backend.spatial.infrastructure.persistence.BedRepository bedRepository) {
		this.properties = properties;
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.organizationRepository = organizationRepository;
		this.wardRepository = wardRepository;
		this.roomRepository = roomRepository;
		this.bedRepository = bedRepository;
	}

	@Override
	public void run(String... args) {
		if (properties.isComplete()) {
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
		} else {
			log.info("Propriétés d'initialisation de l'administrateur désactivées. Initialisation de l'administrateur ignorée.");
		}

		// 2. Initialisation des services spatiaux par défaut si vide pour chaque clinique
		seedSpatialData();
	}

	private void seedSpatialData() {
		for (OrganizationEntity org : organizationRepository.findAll()) {
			try {
				TenantContext.setTenantId(org.getId());
				long count = wardRepository.findAll().stream()
						.filter(w -> org.getId().equals(w.getOrganizationId()))
						.count();
				if (count == 0) {
					log.info("Initialisation des lits et services pour la clinique '{}'...", org.getName());

					// Service Urgences
					com.joprelys.backend.spatial.infrastructure.persistence.WardEntity urg = new com.joprelys.backend.spatial.infrastructure.persistence.WardEntity("Urgences");
					urg.setOrganizationId(org.getId());
					urg = wardRepository.save(urg);

					com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity boxTri = new com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity(urg, "Box Tri", 2, "STANDARD");
					boxTri.setOrganizationId(org.getId());
					boxTri = roomRepository.save(boxTri);

					com.joprelys.backend.spatial.infrastructure.persistence.BedEntity litTriA = new com.joprelys.backend.spatial.infrastructure.persistence.BedEntity(boxTri, "Lit Tri A");
					litTriA.setOrganizationId(org.getId());
					bedRepository.save(litTriA);

					com.joprelys.backend.spatial.infrastructure.persistence.BedEntity litTriB = new com.joprelys.backend.spatial.infrastructure.persistence.BedEntity(boxTri, "Lit Tri B");
					litTriB.setOrganizationId(org.getId());
					bedRepository.save(litTriB);

					com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity dechoc = new com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity(urg, "Salle Déchoquage", 2, "STANDARD");
					dechoc.setOrganizationId(org.getId());
					dechoc = roomRepository.save(dechoc);

					com.joprelys.backend.spatial.infrastructure.persistence.BedEntity litChocA = new com.joprelys.backend.spatial.infrastructure.persistence.BedEntity(dechoc, "Lit Choc A");
					litChocA.setOrganizationId(org.getId());
					bedRepository.save(litChocA);

					com.joprelys.backend.spatial.infrastructure.persistence.BedEntity litChocB = new com.joprelys.backend.spatial.infrastructure.persistence.BedEntity(dechoc, "Lit Choc B");
					litChocB.setOrganizationId(org.getId());
					bedRepository.save(litChocB);

					// Service Médecine Hommes
					com.joprelys.backend.spatial.infrastructure.persistence.WardEntity medH = new com.joprelys.backend.spatial.infrastructure.persistence.WardEntity("Médecine Hommes");
					medH.setOrganizationId(org.getId());
					medH = wardRepository.save(medH);

					com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity ch10 = new com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity(medH, "Chambre 10", 2, "STANDARD");
					ch10.setOrganizationId(org.getId());
					ch10 = roomRepository.save(ch10);

					com.joprelys.backend.spatial.infrastructure.persistence.BedEntity lit10A = new com.joprelys.backend.spatial.infrastructure.persistence.BedEntity(ch10, "Lit 10-A");
					lit10A.setOrganizationId(org.getId());
					bedRepository.save(lit10A);

					com.joprelys.backend.spatial.infrastructure.persistence.BedEntity lit10B = new com.joprelys.backend.spatial.infrastructure.persistence.BedEntity(ch10, "Lit 10-B");
					lit10B.setOrganizationId(org.getId());
					bedRepository.save(lit10B);

					// Service Médecine Femmes
					com.joprelys.backend.spatial.infrastructure.persistence.WardEntity medF = new com.joprelys.backend.spatial.infrastructure.persistence.WardEntity("Médecine Femmes");
					medF.setOrganizationId(org.getId());
					medF = wardRepository.save(medF);

					com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity ch20 = new com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity(medF, "Chambre 20", 2, "STANDARD");
					ch20.setOrganizationId(org.getId());
					ch20 = roomRepository.save(ch20);

					com.joprelys.backend.spatial.infrastructure.persistence.BedEntity lit20A = new com.joprelys.backend.spatial.infrastructure.persistence.BedEntity(ch20, "Lit 20-A");
					lit20A.setOrganizationId(org.getId());
					bedRepository.save(lit20A);

					com.joprelys.backend.spatial.infrastructure.persistence.BedEntity lit20B = new com.joprelys.backend.spatial.infrastructure.persistence.BedEntity(ch20, "Lit 20-B");
					lit20B.setOrganizationId(org.getId());
					bedRepository.save(lit20B);

					// Service Pédiatrie
					com.joprelys.backend.spatial.infrastructure.persistence.WardEntity ped = new com.joprelys.backend.spatial.infrastructure.persistence.WardEntity("Pédiatrie");
					ped.setOrganizationId(org.getId());
					ped = wardRepository.save(ped);

					com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity ch30 = new com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity(ped, "Chambre 30", 2, "STANDARD");
					ch30.setOrganizationId(org.getId());
					ch30 = roomRepository.save(ch30);

					com.joprelys.backend.spatial.infrastructure.persistence.BedEntity lit30A = new com.joprelys.backend.spatial.infrastructure.persistence.BedEntity(ch30, "Lit 30-A");
					lit30A.setOrganizationId(org.getId());
					bedRepository.save(lit30A);

					com.joprelys.backend.spatial.infrastructure.persistence.BedEntity lit30B = new com.joprelys.backend.spatial.infrastructure.persistence.BedEntity(ch30, "Lit 30-B");
					lit30B.setOrganizationId(org.getId());
					bedRepository.save(lit30B);

					log.info("Lits et services par défaut créés avec succès pour '{}'.", org.getName());
				}
			} finally {
				TenantContext.clear();
			}
		}
	}
}
