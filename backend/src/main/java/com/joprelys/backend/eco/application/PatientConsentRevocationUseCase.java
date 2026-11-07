package com.joprelys.backend.eco.application;

import java.util.UUID;

/** Révocation, par le patient depuis le portail Joprelys, d'une application reliée à son dossier. */
public interface PatientConsentRevocationUseCase {

    /**
     * Révoque la liaison et son consentement actif, puis notifie l'application ({@code consent.revoked}).
     * Idempotent : une liaison déjà révoquée ne produit ni erreur ni nouvel événement.
     *
     * @throws EcoBusinessException {@code PATIENT_LINK_NOT_FOUND} si la liaison n'appartient pas à ce patient
     */
    void revokeByPatient(UUID patientId, UUID patientLinkId);
}
