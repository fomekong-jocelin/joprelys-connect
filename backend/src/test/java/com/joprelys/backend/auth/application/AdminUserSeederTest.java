package com.joprelys.backend.auth.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.joprelys.backend.auth.config.SeedAdminProperties;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUserSeederTest {

	@Mock
	private SeedAdminProperties properties;

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private OrganizationRepository organizationRepository;

	@Test
	void shouldNotSeedWhenPropertiesAreIncomplete() {
		when(properties.isComplete()).thenReturn(false);
		AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder, organizationRepository);

		seeder.run();

		verify(userAccountRepository, never()).existsByEmail(any());
		verify(userAccountRepository, never()).save(any());
	}

	@Test
	void shouldNotSeedWhenUsersAlreadyExist() {
		OrganizationEntity org = new OrganizationEntity("Espoir", "espoir@joprelys.local", "123", "street", "Douala");
		when(properties.isComplete()).thenReturn(true);
		when(properties.email()).thenReturn("admin@joprelys.local");
		when(organizationRepository.findByEmail("clinique@joprelys.local")).thenReturn(Optional.of(org));
		when(userAccountRepository.existsByEmail("admin@joprelys.local")).thenReturn(true);
		when(userAccountRepository.existsByEmail("medecin@joprelys.local")).thenReturn(true);
		when(userAccountRepository.existsByEmail("pharmacien@joprelys.local")).thenReturn(true);
		AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder, organizationRepository);

		seeder.run();

		verify(userAccountRepository, never()).save(any());
	}

	@Test
	void shouldSeedAllUsersWhenNoneExist() {
		OrganizationEntity org = new OrganizationEntity("Clinique Joprelys", "clinique@joprelys.local", "+237 600 000 000", "Avenue de la Liberté", "Douala");
		when(properties.isComplete()).thenReturn(true);
		when(properties.email()).thenReturn("admin@joprelys.local");
		when(properties.name()).thenReturn("Administrateur Joprelys");
		when(properties.password()).thenReturn("Password");
		when(organizationRepository.findByEmail("clinique@joprelys.local")).thenReturn(Optional.empty());
		when(organizationRepository.save(any(OrganizationEntity.class))).thenReturn(org);
		when(userAccountRepository.existsByEmail("admin@joprelys.local")).thenReturn(false);
		when(userAccountRepository.existsByEmail("medecin@joprelys.local")).thenReturn(false);
		when(userAccountRepository.existsByEmail("pharmacien@joprelys.local")).thenReturn(false);
		when(passwordEncoder.encode("Password")).thenReturn("hashed_password");
		AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder, organizationRepository);

		seeder.run();

		verify(organizationRepository).save(any(OrganizationEntity.class));
		verify(userAccountRepository, times(3)).save(any(UserAccountEntity.class));
	}
}
