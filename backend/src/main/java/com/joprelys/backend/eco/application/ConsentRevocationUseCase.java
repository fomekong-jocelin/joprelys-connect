package com.joprelys.backend.eco.application;

/** Révoquer un consentement à la demande de l'application bénéficiaire (serveur à serveur). */
public interface ConsentRevocationUseCase {

    /**
     * Révoque le consentement et la liaison associée ; idempotent.
     *
     * @throws EcoBusinessException si le consentement n'existe pas ou n'appartient pas à l'application appelante
     */
    PatientLinkView revoke(RevokeConsentCommand command);
}
