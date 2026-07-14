package com.joprelys.backend.auth.session.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "joprelys.security.sessions")
public record AuthSessionProperties(
        @Min(1) long absoluteTtlHours,
        @Min(5) long inactivityTtlMinutes,
        @Min(1) long retentionHours,
        @NotBlank String refreshCookieName,
        boolean refreshCookieSecure,
        @NotBlank @Pattern(regexp = "Strict|Lax|None") String refreshCookieSameSite,
        @NotBlank String refreshCookiePath,
        @NotBlank String cleanupCron) {
}
