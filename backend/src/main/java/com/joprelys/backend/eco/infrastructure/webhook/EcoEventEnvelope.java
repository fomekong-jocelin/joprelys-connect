package com.joprelys.backend.eco.infrastructure.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

/**
 * Enveloppe CloudEvents 1.0 des webhooks de l'écosystème (contrat eco-contracts {@code event-envelope.schema.json}).
 * L'ordre des composants fixe l'ordre des champs JSON ; le corps sérialisé est stocké tel quel puis signé.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record EcoEventEnvelope(String specversion, UUID id, String type, String source, String time, String subject,
                        String dataschema, Object data) {

    static final String SPEC_VERSION = "1.0";
    static final String SCHEMA_BASE = "https://contracts.eco.example/v1/";

    /** Données de {@code consent.revoked} (contrat {@code consent-revoked.data.schema.json}). */
    record ConsentRevokedData(UUID consentId, UUID patientLinkId, String revokedAt, String revokedBy) {
    }
}
