package com.joprelys.backend.eco.application;

import org.springframework.http.HttpStatus;

/** Erreur métier de l'API écosystème : un code stable, un statut HTTP et un message sans donnée personnelle. */
public final class EcoBusinessException extends RuntimeException {

    private final EcoError error;

    public EcoBusinessException(EcoError error) {
        super(error.message());
        this.error = error;
    }

    public EcoError error() {
        return error;
    }

    public enum EcoError {
        UNKNOWN_CLIENT(HttpStatus.FORBIDDEN, "Client non autorisé dans l'écosystème"),
        MISSING_DPU(HttpStatus.FORBIDDEN, "Le jeton ne porte pas de DPU"),
        UNKNOWN_DPU(HttpStatus.NOT_FOUND, "Aucun dossier patient ne correspond à ce DPU"),
        UNSUPPORTED_RELATIONSHIP(HttpStatus.UNPROCESSABLE_CONTENT,
                "Seule la liaison de son propre dossier est prise en charge pour le moment"),
        INVALID_PERIOD(HttpStatus.UNPROCESSABLE_CONTENT, "Période de consentement invalide"),
        PROFILE_LINKED_ELSEWHERE(HttpStatus.CONFLICT, "Ce profil est déjà relié à un autre dossier"),
        CONSENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Consentement introuvable"),
        PATIENT_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "Application reliée introuvable");

        private final HttpStatus status;
        private final String message;

        EcoError(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public HttpStatus status() {
            return status;
        }

        public String message() {
            return message;
        }
    }
}
