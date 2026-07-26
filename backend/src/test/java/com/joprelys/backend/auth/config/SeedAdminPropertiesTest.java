package com.joprelys.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SeedAdminPropertiesTest {

    @Test
    void shouldBeIncompleteWhenSeedIsDisabled() {
        SeedAdminProperties properties = new SeedAdminProperties(
                false,
                "admin@example.test",
                "Administrateur",
                "test-password");

        assertThat(properties.isComplete()).isFalse();
    }

    @Test
    void shouldBeIncompleteWhenAnyRequiredFieldIsMissingOrBlank() {
        assertThat(new SeedAdminProperties(true, null, "Administrateur", "test-password").isComplete()).isFalse();
        assertThat(new SeedAdminProperties(true, " ", "Administrateur", "test-password").isComplete()).isFalse();
        assertThat(new SeedAdminProperties(true, "admin@example.test", null, "test-password").isComplete()).isFalse();
        assertThat(new SeedAdminProperties(true, "admin@example.test", " ", "test-password").isComplete()).isFalse();
        assertThat(new SeedAdminProperties(true, "admin@example.test", "Administrateur", null).isComplete()).isFalse();
        assertThat(new SeedAdminProperties(true, "admin@example.test", "Administrateur", " ").isComplete()).isFalse();
    }

    @Test
    void shouldBeCompleteOnlyWhenEnabledAndAllRequiredFieldsHaveText() {
        SeedAdminProperties properties = new SeedAdminProperties(
                true,
                "admin@example.test",
                "Administrateur",
                "test-password");

        assertThat(properties.isComplete()).isTrue();
    }
}
