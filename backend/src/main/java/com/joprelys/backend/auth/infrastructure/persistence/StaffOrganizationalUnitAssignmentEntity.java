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
@Table(name = "staff_organizational_unit_assignments")
public class StaffOrganizationalUnitAssignmentEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "organizational_unit_id", nullable = false)
    private UUID organizationalUnitId;

    @Column(name = "assignment_role_code", nullable = false, length = 64)
    private String assignmentRoleCode;

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

    protected StaffOrganizationalUnitAssignmentEntity() {
    }

    public StaffOrganizationalUnitAssignmentEntity(
            UUID organizationId,
            UUID staffId,
            UUID organizationalUnitId,
            String assignmentRoleCode,
            boolean primary,
            Instant validFrom,
            Instant validTo) {
        this.id = UUID.randomUUID();
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.staffId = Objects.requireNonNull(staffId, "staffId");
        this.organizationalUnitId = Objects.requireNonNull(organizationalUnitId, "organizationalUnitId");
        this.assignmentRoleCode = requireCode(assignmentRoleCode);
        this.primary = primary;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.validTo = validTo;
        validatePeriod();
    }

    public void update(
            UUID organizationalUnitId,
            String assignmentRoleCode,
            boolean primary,
            Instant validFrom,
            Instant validTo) {
        this.organizationalUnitId = Objects.requireNonNull(organizationalUnitId, "organizationalUnitId");
        this.assignmentRoleCode = requireCode(assignmentRoleCode);
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
        boolean startsBeforeOtherEnds = to == null || validFrom.isBefore(to);
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

    private static String requireCode(String value) {
        String normalized = Objects.requireNonNull(value, "assignmentRoleCode").trim().toUpperCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("assignmentRoleCode est obligatoire.");
        }
        return normalized;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getStaffId() { return staffId; }
    public UUID getOrganizationalUnitId() { return organizationalUnitId; }
    public String getAssignmentRoleCode() { return assignmentRoleCode; }
    public boolean isPrimary() { return primary; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidTo() { return validTo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
