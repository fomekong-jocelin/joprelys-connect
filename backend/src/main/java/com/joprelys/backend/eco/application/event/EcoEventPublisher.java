package com.joprelys.backend.eco.application.event;

/**
 * Publication des événements de l'écosystème vers les abonnés.
 * Doit être appelé dans la transaction du changement métier : l'événement est validé ou annulé avec lui.
 */
public interface EcoEventPublisher {

    void publishConsentRevoked(ConsentRevokedEvent event);
}
