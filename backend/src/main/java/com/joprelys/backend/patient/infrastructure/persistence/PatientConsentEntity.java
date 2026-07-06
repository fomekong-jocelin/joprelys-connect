package com.joprelys.backend.patient.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Entité consentement patient conforme au CDC — Module 12 (STORY-1909).
 *
 * Champs CDC ajoutés :
 * - consent_type           : type de consentement (ETABLISSEMENT, TEMPORAIRE, PONCTUEL, ...)
 * - requester_user_id      : identifiant de l'utilisateur demandeur
 * - requester_organization_id : identifiant de l'établissement demandeur
 * - reason                 : motif du consentement
 * - requested_at           : horodatage de la demande
 * - approved_at            : horodatage de l'approbation
 * - expires_at             : expiration (FR-CONSENT-003)
 *
 * Statuts : REQUESTED | APPROVED | REJECTED | EXPIRED | REVOKED (FR-CONSENT-004)
 * Journalisation : déléguée à AuditService (FR-CONSENT-005)
 */
@Entity
@Table(name = "patient_consents")
public class PatientConsentEntity {

	@Id
	private UUID id;

	@Column(name = "patient_id", nullable = false)
	private UUID patientId;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	/** Statut CDC : REQUESTED, APPROVED, REJECTED, EXPIRED, REVOKED */
	@Column(name = "status", nullable = false, length = 20)
	private String status;

	// WT1 (SCOPES): Granular access scopes
	@Column(name = "scopes", length = 500)
	private String scopes = "medical_records,prescriptions,lab_results,allergies_history";

	// WT1 (SCOPES): Validation channel
	@Column(name = "validation_channel", length = 50)
	private String validationChannel = ValidationChannel.PORTAL.name();

	/** STORY-1909 — CDC: Type de consentement */
	@Column(name = "consent_type", length = 30)
	private String consentType = ConsentType.ETABLISSEMENT.name();

	/** STORY-1909 — CDC: Identifiant de l'utilisateur demandeur (praticien, agent) */
	@Column(name = "requester_user_id")
	private UUID requesterUserId;

	/** STORY-1909 — CDC: Identifiant de l'établissement demandeur */
	@Column(name = "requester_organization_id")
	private UUID requesterOrganizationId;

	/** STORY-1909 — CDC: Motif du consentement */
	@Column(name = "reason", columnDefinition = "TEXT")
	private String reason;

	/** STORY-1909 — CDC: Date de demande */
	@Column(name = "requested_at")
	private Instant requestedAt;

	/** STORY-1909 — CDC: Date d'approbation */
	@Column(name = "approved_at")
	private Instant approvedAt;

	/** STORY-1909 — CDC FR-CONSENT-003: Date d'expiration */
	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PatientConsentEntity() {
	}

	/** Constructeur hérité (rétrocompatibilité avec l'existant) */
	public PatientConsentEntity(UUID patientId, UUID organizationId, String status) {
		this.id = UUID.randomUUID();
		this.patientId = patientId;
		this.organizationId = organizationId;
		this.status = status;
		this.consentType = ConsentType.ETABLISSEMENT.name();
	}

	/** Constructeur CDC complet (STORY-1909) */
	public PatientConsentEntity(UUID patientId, UUID organizationId, String status,
								String consentType, UUID requesterUserId,
								UUID requesterOrganizationId, String reason, Instant expiresAt) {
		this.id = UUID.randomUUID();
		this.patientId = patientId;
		this.organizationId = organizationId;
		this.status = status;
		this.consentType = consentType;
		this.requesterUserId = requesterUserId;
		this.requesterOrganizationId = requesterOrganizationId;
		this.reason = reason;
		this.expiresAt = expiresAt;
		this.requestedAt = Instant.now();
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
		if (requestedAt == null) {
			requestedAt = now;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	// ─── Getters ────────────────────────────────────────────────────────────────

	public UUID getId() { return id; }
	public UUID getPatientId() { return patientId; }
	public UUID getOrganizationId() { return organizationId; }
	public String getStatus() { return status; }
	public String getScopes() { return scopes; }
	public String getValidationChannel() { return validationChannel; }
	public String getConsentType() { return consentType; }
	public UUID getRequesterUserId() { return requesterUserId; }
	public UUID getRequesterOrganizationId() { return requesterOrganizationId; }
	public String getReason() { return reason; }
	public Instant getRequestedAt() { return requestedAt; }
	public Instant getApprovedAt() { return approvedAt; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	// ─── Setters ────────────────────────────────────────────────────────────────

	public void setStatus(String status) { this.status = status; }
	public void setScopes(String scopes) { this.scopes = scopes; }
	public void setValidationChannel(String validationChannel) { this.validationChannel = validationChannel; }
	public void setConsentType(String consentType) { this.consentType = consentType; }
	public void setRequesterUserId(UUID requesterUserId) { this.requesterUserId = requesterUserId; }
	public void setRequesterOrganizationId(UUID requesterOrganizationId) { this.requesterOrganizationId = requesterOrganizationId; }
	public void setReason(String reason) { this.reason = reason; }
	public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
	public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
	public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

	// WT3 (DUPLICATES): Setter for patient reassignment during merge
	public void setPatientId(UUID patientId) { this.patientId = patientId; }

	/**
	 * FR-CONSENT-003 : vérifie si le consentement est encore valide (non expiré).
	 */
	public boolean isEffectivelyValid() {
		if (!ConsentStatus.APPROVED.name().equals(status)) return false;
		if (expiresAt == null) return true; // Consentement permanent
		return expiresAt.isAfter(Instant.now());
	}
}
