-- HOS-BED-002-D : historiser les changements de capacité et de préparation des lits.
-- Le journal est append-only au niveau applicatif et relié au lit avec intégrité tenant.
-- La cascade reste temporairement compatible avec la suppression physique legacy des lits ;
-- GAP-012 remplacera ce comportement par un archivage métier.

CREATE TABLE bed_state_changes (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    bed_id UUID NOT NULL,
    state_axis VARCHAR(20) NOT NULL,
    previous_value VARCHAR(50) NOT NULL,
    new_value VARCHAR(50) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    reason_note VARCHAR(500),
    actor_id UUID,
    actor_display_name VARCHAR(160) NOT NULL,
    source VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_bed_state_changes_axis
        CHECK (state_axis IN ('CAPACITY', 'READINESS')),
    CONSTRAINT ck_bed_state_changes_source
        CHECK (source IN ('MANUAL', 'SYSTEM_TRANSFER', 'SYSTEM_PHYSICAL_DEPARTURE', 'LEGACY_SUPERVISION')),
    CONSTRAINT ck_bed_state_changes_values
        CHECK (previous_value <> new_value),
    CONSTRAINT ck_bed_state_changes_actor_name
        CHECK (TRIM(actor_display_name) <> ''),
    CONSTRAINT fk_bed_state_changes_bed_organization
        FOREIGN KEY (bed_id, organization_id)
        REFERENCES beds(id, organization_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_bed_state_changes_bed_time
    ON bed_state_changes(organization_id, bed_id, occurred_at DESC);

CREATE INDEX idx_bed_state_changes_reason_time
    ON bed_state_changes(organization_id, reason_code, occurred_at DESC);
