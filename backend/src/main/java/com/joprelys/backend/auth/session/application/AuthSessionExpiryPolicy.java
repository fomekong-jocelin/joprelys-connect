package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.session.config.AuthSessionProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionExpiryPolicy {

    private final AuthSessionProperties properties;

    public AuthSessionExpiryPolicy(AuthSessionProperties properties) {
        this.properties = properties;
    }

    public Instant absoluteExpiryFrom(Instant issuedAt) {
        return issuedAt.plus(properties.absoluteTtlHours(), ChronoUnit.HOURS);
    }

    public Instant idleExpiryFrom(Instant activityAt, Instant absoluteExpiry) {
        Instant idleExpiry = activityAt.plus(properties.inactivityTtlMinutes(), ChronoUnit.MINUTES);
        return idleExpiry.isBefore(absoluteExpiry) ? idleExpiry : absoluteExpiry;
    }

    public Instant retentionCutoffFrom(Instant now) {
        return now.minus(properties.retentionHours(), ChronoUnit.HOURS);
    }
}
