package com.joprelys.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.config.SeedAdminProperties;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUserSeederTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldFailFastWhenEnabledConfigurationIsIncomplete() {
        SeedAdminProperties properties = new SeedAdminProperties(true, "admin@example.test", "Admin", " ");
        AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder);

        assertThatThrownBy(seeder::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configuration du bootstrap administrateur incomplète")
                .hasMessageNotContaining("admin@example.test");

        verify(userAccountRepository, never()).existsByEmail(any());
        verify(userAccountRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldNotSeedWhenAdminAlreadyExists() {
        SeedAdminProperties properties = new SeedAdminProperties(
                true,
                "  Admin@Example.Test  ",
                "Administrateur Joprelys",
                "test-password");
        when(userAccountRepository.existsByEmail("admin@example.test")).thenReturn(true);
        AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder);

        seeder.run();

        verify(userAccountRepository).existsByEmail("admin@example.test");
        verify(userAccountRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldSeedAdminWithEncodedPasswordWhenConfigurationIsComplete() {
        SeedAdminProperties properties = new SeedAdminProperties(
                true,
                "  Admin@Example.Test  ",
                "  Administrateur Joprelys  ",
                "test-password");
        when(userAccountRepository.existsByEmail("admin@example.test")).thenReturn(false);
        when(passwordEncoder.encode("test-password")).thenReturn("hashed_password");
        AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder);

        seeder.run();

        verify(passwordEncoder).encode("test-password");
        ArgumentCaptor<UserAccountEntity> captor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userAccountRepository).save(captor.capture());
        UserAccountEntity saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.test");
        assertThat(saved.getDisplayName()).isEqualTo("Administrateur Joprelys");
        assertThat(saved.getRole()).isEqualTo("ADMIN_JOPRELYS");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed_password");
        assertThat(saved.getOrganizationId()).isNull();
    }
}
