package com.joprelys.backend.eco.infrastructure.persistence;

import com.joprelys.backend.eco.domain.EcoStatus;
import com.joprelys.backend.eco.domain.LinkRelationship;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Liaison d'un profil d'une application de l'écosystème à un DPU Joprelys, sans copie d'identité. */
@Entity
@Table(name = "eco_patient_links")
public class EcoPatientLinkEntity {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "dpu", nullable = false, length = 64)
    private String dpu;

    @Column(name = "issuer", nullable = false, length = 80)
    private String issuer;

    @Column(name = "application", nullable = false, length = 50)
    private String application;

    @Column(name = "external_profile_id", nullable = false)
    private UUID externalProfileId;

    /** Marqueur d'unicité (V125) : égal à {@code externalProfileId} tant que la liaison est active, nul ensuite. */
    @Column(name = "active_external_profile_id")
    private UUID activeExternalProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false, length = 20)
    private LinkRelationship relationship;

    @Column(name = "identity_subject", nullable = false)
    private String identitySubject;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EcoStatus status;

    @Column(name = "version_id", nullable = false)
    private int versionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected EcoPatientLinkEntity() {
    }

    public EcoPatientLinkEntity(UUID patientId, String dpu, String issuer, String application, UUID externalProfileId,
                                LinkRelationship relationship, String identitySubject, String clientId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.dpu = dpu;
        this.issuer = issuer;
        this.application = application;
        this.externalProfileId = externalProfileId;
        this.activeExternalProfileId = externalProfileId;
        this.relationship = relationship;
        this.identitySubject = identitySubject;
        this.clientId = clientId;
        this.status = EcoStatus.ACTIVE;
        this.versionId = 1;
        this.createdAt = createdAt;
    }

    /** Révocation idempotente : renvoie {@code false} si la liaison était déjà révoquée. */
    public boolean revoke(Instant when) {
        if (status == EcoStatus.REVOKED) {
            return false;
        }
        status = EcoStatus.REVOKED;
        activeExternalProfileId = null;
        revokedAt = when;
        versionId++;
        return true;
    }

    public UUID getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public String getDpu() { return dpu; }
    public String getIssuer() { return issuer; }
    public String getApplication() { return application; }
    public UUID getExternalProfileId() { return externalProfileId; }
    public LinkRelationship getRelationship() { return relationship; }
    public String getIdentitySubject() { return identitySubject; }
    public String getClientId() { return clientId; }
    public EcoStatus getStatus() { return status; }
    public int getVersionId() { return versionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
