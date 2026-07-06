package com.joprelys.backend.patient.api;

import java.time.Instant;

/**
 * DTO de création d'un consentement CDC complet — STORY-1909.
 */
public record ConsentRequest(
        /** Type CDC : PONCTUEL, TEMPORAIRE, ETABLISSEMENT, PROFESSIONNEL, LIMITE, URGENCE */
        String consentType,
        /** Motif du consentement (obligatoire) */
        String reason,
        /** Date d'expiration (FR-CONSENT-003, obligatoire pour types temporaires) */
        Instant expiresAt,
        /** Scopes d'accès granulaires (ex: medical_records,prescriptions) */
        String scopes,
        /** Canal de validation : PORTAL, OTP_SMS, OTP_EMAIL, AGENT_HABILITE */
        String validationChannel
) {}
