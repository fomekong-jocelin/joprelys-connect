package com.joprelys.backend.patient.infrastructure.persistence;

/**
 * Canaux de validation du consentement — CDC Module 12.
 *
 * - PORTAL          : validation via le portail patient (défaut actuel)
 * - OTP_SMS         : code OTP envoyé par SMS
 * - OTP_EMAIL       : code OTP envoyé par email
 * - AGENT_HABILITE  : validation par un agent habilité (agent d'accueil, etc.)
 */
public enum ValidationChannel {
    PORTAL,
    OTP_SMS,
    OTP_EMAIL,
    AGENT_HABILITE
}
