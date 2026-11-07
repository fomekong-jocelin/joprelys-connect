package com.joprelys.backend.eco.infrastructure.webhook;

import com.joprelys.backend.eco.config.EcoProperties;
import java.util.UUID;

/** Envoi d'un webhook signé à un abonné. Ne lève pas d'exception : le résultat dit s'il faut rejouer. */
public interface EcoWebhookSender {

    DeliveryResult send(EcoProperties.Subscriber subscriber, UUID eventId, String body);

    /** @param error statut technique court (jamais le contenu de l'événement), {@code null} si livré */
    record DeliveryResult(boolean delivered, String error) {

        static DeliveryResult ok() {
            return new DeliveryResult(true, null);
        }

        static DeliveryResult failed(String error) {
            return new DeliveryResult(false, error);
        }
    }
}
