package com.joprelys.backend.visit.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.UUID;

/**
 * Périmètre des écritures cliniques (SEC-09) : un consentement ou un bris de glace ouvre la
 * <em>lecture</em> d'un dossier d'un autre établissement, jamais l'écriture dans une visite qui
 * ne relève pas de l'établissement de l'acteur. Tout refus est un 403 {@code CLINICAL_WRITE_OUT_OF_SCOPE}.
 */
public interface ClinicalWriteScopePolicy {

    String OUT_OF_SCOPE_CODE = "CLINICAL_WRITE_OUT_OF_SCOPE";

    /**
     * Vérifie, avant tout changement de tenant, que l'acteur appartient à l'établissement
     * propriétaire de la visite.
     *
     * @throws org.springframework.web.server.ResponseStatusException 404 si la visite n'existe pas,
     *         401 si l'acteur est inconnu, 403 si l'établissement diffère
     */
    void requireActorInVisitOrganization(UUID visitId, String actorEmail);

    /** Même règle lorsque la visite et l'acteur sont déjà chargés. */
    void requireSameOrganization(VisitEntity visit, UserAccountEntity actor);
}
