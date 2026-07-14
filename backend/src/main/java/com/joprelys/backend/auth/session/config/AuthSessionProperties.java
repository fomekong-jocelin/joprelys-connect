package com.joprelys.backend.auth.session.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "joprelys.security.sessions")
public record AuthSessionProperties(
        @DefaultValue("168") @Min(1) long absoluteTtlHours,
        @DefaultValue("30") @Min(5) long inactivityTtlMinutes,
        @DefaultValue("168") @Min(1) long retentionHours,
        @DefaultValue("joprelys_refresh") @NotBlank String refreshCookieName,
        @DefaultValue("false") boolean refreshCookieSecure,
        @DefaultValue("Lax") @NotBlank @Pattern(regexp = "Strict|Lax|None") String refreshCookieSameSite,
        @DefaultValue("/api/auth") @NotBlank String refreshCookiePath,
        @DefaultValue("0 15 * * * *") @NotBlank String cleanupCron) {
}
