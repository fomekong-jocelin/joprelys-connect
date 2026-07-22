package com.joprelys.backend.spatial.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "bed_state_changes")
public class BedStateChangeEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "bed_id", nullable = false)
    private UUID bedId;

    @Column(name = "state_axis", nullable = false, length = 20)
    private String axis;

    @Column(name = "previous_value", nullable = false, length = 50)
    private String previousValue;

    @Column(name = "new_value", nullable = false, length = 50)
    private String newValue;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(name = "reason_note", length = 500)
    private String reasonNote;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_display_name", nullable = false, length = 160)
    private String actorDisplayName;

    @Column(name = "source", nullable = false, length = 40)
    private String source;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected BedStateChangeEntity() {
    }

    public BedStateChangeEntity(
            BedEntity bed,
            BedStateAxis axis,
            String previousValue,
            String newValue,
            BedStateReasonCode reasonCode,
            String reasonNote,
            UUID actorId,
            String actorDisplayName,
            BedStateChangeSource source,
            Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.organizationId = Objects.requireNonNull(bed.getOrganizationId(), "L'établissement du lit est obligatoire.");
        this.bedId = Objects.requireNonNull(bed.getId(), "L'identifiant du lit est obligatoire.");
        this.axis = Objects.requireNonNull(axis, "L'axe du changement est obligatoire.").name();
        this.previousValue = requiredValue(previousValue, "L'ancienne valeur est obligatoire.");
        this.newValue = requiredValue(newValue, "La nouvelle valeur est obligatoire.");
        if (this.previousValue.equals(this.newValue)) {
            throw new IllegalArgumentException("Un changement de lit doit modifier la valeur de l'axe concerné.");
        }
        this.reasonCode = Objects.requireNonNull(reasonCode, "Le motif du changement est obligatoire.").name();
        this.reasonNote = normalize(reasonNote);
        this.actorId = actorId;
        this.actorDisplayName = requiredValue(actorDisplayName, "Le nom de l'acteur est obligatoire.");
        this.source = Objects.requireNonNull(source, "La source du changement est obligatoire.").name();
        this.occurredAt = Objects.requireNonNull(occurredAt, "La date du changement est obligatoire.");
    }

    private String requiredValue(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getBedId() {
        return bedId;
    }

    public BedStateAxis getAxis() {
        return BedStateAxis.valueOf(axis);
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public BedStateReasonCode getReasonCode() {
        return BedStateReasonCode.valueOf(reasonCode);
    }

    public String getReasonNote() {
        return reasonNote;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public BedStateChangeSource getSource() {
        return BedStateChangeSource.valueOf(source);
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
