package com.joprelys.backend.eco.application;

import com.joprelys.backend.eco.domain.ConsentScope;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Application de l'écosystème reliée au dossier du patient, telle que le portail l'affiche.
 *
 * @param id          identifiant de la liaison ({@code PatientLink})
 * @param application code de l'application (ex. {@code allopharma})
 * @param validUntil  fin du consentement actif, {@code null} s'il n'y en a plus
 */
public record LinkedAppView(UUID id, String application, Set<ConsentScope> scopes, Instant linkedAt,
                            Instant validUntil) {

    public LinkedAppView {
        scopes = Set.copyOf(scopes);
    }
}
