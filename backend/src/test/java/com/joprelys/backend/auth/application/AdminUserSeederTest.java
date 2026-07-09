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
import java.util.List;
import java.util.UUID;
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

	@Mock
	private com.joprelys.backend.spatial.infrastructure.persistence.WardRepository wardRepository;

	@Mock
	private com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository roomRepository;

	@Mock
	private com.joprelys.backend.spatial.infrastructure.persistence.BedRepository bedRepository;

	@Test
	void shouldNotSeedWhenPropertiesAreIncomplete() {
		when(properties.isComplete()).thenReturn(false);
		AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder, organizationRepository, wardRepository, roomRepository, bedRepository);

		seeder.run();

		verify(userAccountRepository, never()).existsByEmail(any());
		verify(userAccountRepository, never()).save(any());
	}

	@Test
	void shouldNotSeedWhenUsersAlreadyExist() {
		when(properties.isComplete()).thenReturn(true);
		when(properties.email()).thenReturn("admin@joprelys.local");
		when(userAccountRepository.existsByEmail("admin@joprelys.local")).thenReturn(true);
		AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder, organizationRepository, wardRepository, roomRepository, bedRepository);

		seeder.run();

		verify(userAccountRepository, never()).save(any());
	}

	@Test
	void shouldSeedAdminWhenNotExist() {
		when(properties.isComplete()).thenReturn(true);
		when(properties.email()).thenReturn("admin@joprelys.local");
		when(properties.name()).thenReturn("Administrateur Joprelys");
		when(properties.password()).thenReturn("Password");
		when(userAccountRepository.existsByEmail("admin@joprelys.local")).thenReturn(false);
		when(passwordEncoder.encode("Password")).thenReturn("hashed_password");
		AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder, organizationRepository, wardRepository, roomRepository, bedRepository);

		seeder.run();

		verify(organizationRepository, never()).save(any(OrganizationEntity.class));
		verify(userAccountRepository, times(1)).save(any(UserAccountEntity.class));
	}

	@Test
	void shouldSeedSpatialDataWhenOrganizationsExist() {
		when(properties.isComplete()).thenReturn(true);
		when(properties.email()).thenReturn("admin@joprelys.local");
		when(userAccountRepository.existsByEmail("admin@joprelys.local")).thenReturn(true);

		OrganizationEntity org = new OrganizationEntity("Clinique Test", "test@clinique.com", "12345", "Street", "City");

		when(organizationRepository.findAll()).thenReturn(List.of(org));
		when(wardRepository.findAll()).thenReturn(List.of());

		AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder, organizationRepository, wardRepository, roomRepository, bedRepository);

		seeder.run();

		verify(wardRepository, times(4)).save(any());
		verify(roomRepository, times(5)).save(any());
		verify(bedRepository, times(10)).save(any());
	}
}
