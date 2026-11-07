package com.joprelys.backend.eco.infrastructure.persistence;

import com.joprelys.backend.eco.domain.ConsentScope;
import com.joprelys.backend.eco.domain.EcoStatus;
import com.joprelys.backend.eco.domain.GranteeType;
import com.joprelys.backend.eco.domain.RevokedBy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Consentement attaché à une liaison : base de toute lecture croisée. */
@Entity
@Table(name = "eco_consents")
public class EcoConsentEntity {

    @Id
    private UUID id;

    @Column(name = "patient_link_id", nullable = false)
    private UUID patientLinkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EcoStatus status;

    /** Périmètres séparés par des virgules, dans l'ordre de l'énumération. */
    @Column(name = "scopes", nullable = false)
    private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(name = "grantee_type", nullable = false, length = 20)
    private GranteeType granteeType;

    @Column(name = "grantee_reference", nullable = false, length = 100)
    private String granteeReference;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_by", length = 20)
    private RevokedBy revokedBy;

    @Column(name = "revocation_reason", length = 200)
    private String revocationReason;

    @Column(name = "version_id", nullable = false)
    private int versionId;

    protected EcoConsentEntity() {
    }

    public EcoConsentEntity(UUID patientLinkId, Collection<ConsentScope> scopes, GranteeType granteeType,
                            String granteeReference, Instant periodStart, Instant periodEnd) {
        this.id = UUID.randomUUID();
        this.patientLinkId = patientLinkId;
        this.status = EcoStatus.ACTIVE;
        this.scopes = EnumSet.copyOf(scopes).stream().map(Enum::name).collect(Collectors.joining(","));
        this.granteeType = granteeType;
        this.granteeReference = granteeReference;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.grantedAt = periodStart;
        this.versionId = 1;
    }

    /** Révocation idempotente : renvoie {@code false} si le consentement était déjà révoqué. */
    public boolean revoke(Instant when, RevokedBy by, String reason) {
        if (status == EcoStatus.REVOKED) {
            return false;
        }
        status = EcoStatus.REVOKED;
        revokedAt = when;
        revokedBy = by;
        revocationReason = reason;
        versionId++;
        return true;
    }

    public Set<ConsentScope> scopeSet() {
        return Arrays.stream(scopes.split(",")).map(ConsentScope::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ConsentScope.class)));
    }

    public UUID getId() { return id; }
    public UUID getPatientLinkId() { return patientLinkId; }
    public EcoStatus getStatus() { return status; }
    public GranteeType getGranteeType() { return granteeType; }
    public String getGranteeReference() { return granteeReference; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public RevokedBy getRevokedBy() { return revokedBy; }
    public String getRevocationReason() { return revocationReason; }
    public int getVersionId() { return versionId; }
}
