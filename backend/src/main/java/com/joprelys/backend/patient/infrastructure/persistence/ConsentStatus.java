package com.joprelys.backend.patient.infrastructure.persistence;

/**
 * Statuts de consentement conformes au CDC — Module 12.
 *
 * - REQUESTED : consentement demandé, en attente de validation patient
 * - APPROVED  : consentement approuvé et actif
 * - REJECTED  : consentement refusé par le patient
 * - EXPIRED   : consentement arrivé à expiration (expires_at dépassé)
 * - REVOKED   : consentement révoqué explicitement par le patient (FR-CONSENT-004)
 *
 * Note : ACTIVE est conservé temporairement pour compatibilité avec l'existant
 * (migration V38 renomme ACTIVE → APPROVED en base).
 */
public enum ConsentStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    EXPIRED,
    REVOKED
}
