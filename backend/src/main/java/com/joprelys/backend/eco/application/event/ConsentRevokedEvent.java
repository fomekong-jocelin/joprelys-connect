package com.joprelys.backend.eco.application.event;

import com.joprelys.backend.eco.domain.RevokedBy;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Révocation à notifier à l'application bénéficiaire ({@code consent.revoked}).
 * Le motif libre saisi par le patient n'est volontairement pas transmis (minimisation).
 */
public record ConsentRevokedEvent(String application, UUID consentId, UUID patientLinkId, Instant revokedAt,
                                  RevokedBy revokedBy) {

    public ConsentRevokedEvent {
        Objects.requireNonNull(application, "application");
        Objects.requireNonNull(consentId, "consentId");
        Objects.requireNonNull(patientLinkId, "patientLinkId");
        Objects.requireNonNull(revokedAt, "revokedAt");
        Objects.requireNonNull(revokedBy, "revokedBy");
    }
}
