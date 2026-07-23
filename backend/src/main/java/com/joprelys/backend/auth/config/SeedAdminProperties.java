package com.joprelys.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joprelys.seed.admin")
public record SeedAdminProperties(boolean enabled, String email, String name, String password) {

    public boolean isComplete() {
        return enabled && hasText(email) && hasText(name) && hasText(password);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
