package com.joprelys.backend.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "staff_specialty_assignments")
public class StaffSpecialtyAssignmentEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "specialty_code", nullable = false, length = 64)
    private String specialtyCode;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StaffSpecialtyAssignmentEntity() {
    }

    public StaffSpecialtyAssignmentEntity(
            UUID organizationId,
            UUID staffId,
            String specialtyCode,
            boolean primary,
            Instant validFrom,
            Instant validTo) {
        this.id = UUID.randomUUID();
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.staffId = Objects.requireNonNull(staffId, "staffId");
        this.specialtyCode = requireCode(specialtyCode, "specialtyCode");
        this.primary = primary;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        validatePeriod();
    }

    public void update(String specialtyCode, boolean primary, Instant validFrom, Instant validTo) {
        this.specialtyCode = requireCode(specialtyCode, "specialtyCode");
        this.primary = primary;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        validatePeriod();
    }

    public void closeAt(Instant closedAt) {
        Instant end = Objects.requireNonNull(closedAt, "closedAt");
        if (!end.isAfter(validFrom)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure au début de l'affectation.");
        }
        if (validTo != null && !end.isBefore(validTo)) {
            throw new IllegalArgumentException("La nouvelle date de fin doit raccourcir la période existante.");
        }
        this.validTo = end;
    }

    public boolean overlaps(Instant from, Instant to, UUID excludedId) {
        if (excludedId != null && excludedId.equals(id)) {
            return false;
        }
        Instant otherEnd = to;
        boolean startsBeforeOtherEnds = otherEnd == null || validFrom.isBefore(otherEnd);
        boolean otherStartsBeforeThisEnds = validTo == null || from.isBefore(validTo);
        return startsBeforeOtherEnds && otherStartsBeforeThisEnds;
    }

    public boolean activeAt(Instant at) {
        return !validFrom.isAfter(at) && (validTo == null || validTo.isAfter(at));
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        validatePeriod();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
        validatePeriod();
    }

    private void validatePeriod() {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure au début de l'affectation.");
        }
    }

    private static String requireCode(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim().toUpperCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " est obligatoire.");
        }
        return normalized;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getStaffId() { return staffId; }
    public String getSpecialtyCode() { return specialtyCode; }
    public boolean isPrimary() { return primary; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidTo() { return validTo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
