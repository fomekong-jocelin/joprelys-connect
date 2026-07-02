package com.joprelys.backend.visit.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * STORY-0603 — Requête de révocation ou d'annulation d'un document médical.
 */
public record RevokeDocumentRequest(
        @NotBlank(message = "Le motif de révocation est obligatoire.")
        @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères.")
        String reason
) {}
