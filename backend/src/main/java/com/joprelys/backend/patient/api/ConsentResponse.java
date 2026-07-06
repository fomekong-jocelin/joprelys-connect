package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.infrastructure.persistence.PatientConsentEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de réponse pour un consentement patient — STORY-1909 CDC Module 12.
 */
public record ConsentResponse(
        UUID id,
        UUID patientId,
        UUID organizationId,
        /** Statut CDC : REQUESTED | APPROVED | REJECTED | EXPIRED | REVOKED */
        String status,
        /** Type CDC : PONCTUEL, TEMPORAIRE, ETABLISSEMENT, PROFESSIONNEL, LIMITE, URGENCE */
        String consentType,
        /** Identifiant du demandeur */
        UUID requesterUserId,
        /** Identifiant de l'organisation demandeuse */
        UUID requesterOrganizationId,
        /** Motif du consentement */
        String reason,
        /** Scopes granulaires */
        String scopes,
        /** Canal de validation */
        String validationChannel,
        /** Horodatage de la demande */
        Instant requestedAt,
        /** Horodatage de l'approbation */
        Instant approvedAt,
        /** Date d'expiration (FR-CONSENT-003) */
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConsentResponse fromEntity(PatientConsentEntity e) {
        return new ConsentResponse(
                e.getId(),
                e.getPatientId(),
                e.getOrganizationId(),
                e.getStatus(),
                e.getConsentType(),
                e.getRequesterUserId(),
                e.getRequesterOrganizationId(),
                e.getReason(),
                e.getScopes(),
                e.getValidationChannel(),
                e.getRequestedAt(),
                e.getApprovedAt(),
                e.getExpiresAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
