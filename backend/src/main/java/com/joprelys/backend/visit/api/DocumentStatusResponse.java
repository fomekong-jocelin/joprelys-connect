package com.joprelys.backend.visit.api;

import java.time.Instant;
import java.util.UUID;

/**
 * STORY-0603 — Réponse retournée après une révocation/annulation de document.
 * Contient les métadonnées de traçabilité sans exposer de données médicales.
 */
public record DocumentStatusResponse(
        UUID documentId,
        String documentNumber,
        String status,
        Instant revokedAt,
        UUID revokedByUserId,
        String revocationReason
) {}
