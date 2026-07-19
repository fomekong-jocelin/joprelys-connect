package com.joprelys.backend.auth.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.config.SeedAdminProperties;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
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

    @Test
    void shouldNotSeedWhenPropertiesAreIncomplete() {
        when(properties.isComplete()).thenReturn(false);
        AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder);

        seeder.run();

        verify(userAccountRepository, never()).existsByEmail(any());
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void shouldNotSeedWhenAdminAlreadyExists() {
        when(properties.isComplete()).thenReturn(true);
        when(properties.email()).thenReturn("admin@joprelys.local");
        when(userAccountRepository.existsByEmail("admin@joprelys.local")).thenReturn(true);
        AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder);

        seeder.run();

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void shouldSeedAdminWhenMissing() {
        when(properties.isComplete()).thenReturn(true);
        when(properties.email()).thenReturn("admin@joprelys.local");
        when(properties.name()).thenReturn("Administrateur Joprelys");
        when(properties.password()).thenReturn("Password");
        when(userAccountRepository.existsByEmail("admin@joprelys.local")).thenReturn(false);
        when(passwordEncoder.encode("Password")).thenReturn("hashed_password");
        AdminUserSeeder seeder = new AdminUserSeeder(properties, userAccountRepository, passwordEncoder);

        seeder.run();

        verify(userAccountRepository, times(1)).save(any(UserAccountEntity.class));
    }
}
