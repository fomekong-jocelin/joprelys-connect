package com.joprelys.backend.patient.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientConsentRepository extends JpaRepository<PatientConsentEntity, UUID> {

    /** Rétrocompatibilité : récupère le consentement ETABLISSEMENT pour une paire (patient, org). */
    @Query("SELECT c FROM PatientConsentEntity c WHERE c.patientId = :patientId AND c.organizationId = :organizationId AND (c.consentType = 'ETABLISSEMENT' OR c.consentType IS NULL) ORDER BY c.createdAt DESC")
    Optional<PatientConsentEntity> findByPatientIdAndOrganizationId(UUID patientId, UUID organizationId);

    /** Récupère tous les consentements d'un patient (historique complet). */
    List<PatientConsentEntity> findByPatientId(UUID patientId);

    /** Récupère tous les consentements APPROVED non expirés d'un patient pour une organisation. */
    @Query("SELECT c FROM PatientConsentEntity c WHERE c.patientId = :patientId AND c.organizationId = :organizationId AND c.status = 'APPROVED' AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    List<PatientConsentEntity> findActiveConsents(UUID patientId, UUID organizationId, Instant now);

    /** Marque comme EXPIRED tous les consentements dont expires_at est dépassé (FR-CONSENT-003). */
    @Modifying
    @Query("UPDATE PatientConsentEntity c SET c.status = 'EXPIRED' WHERE c.status = 'APPROVED' AND c.expiresAt IS NOT NULL AND c.expiresAt <= :now")
    int expireOutdatedConsents(Instant now);

    /** Récupère les consentements en attente (REQUESTED) pour un patient. */
    List<PatientConsentEntity> findByPatientIdAndStatus(UUID patientId, String status);
}
