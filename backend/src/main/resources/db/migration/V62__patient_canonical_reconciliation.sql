ALTER TABLE patients
    ADD CONSTRAINT uq_patients_id_organization UNIQUE (id, organization_id);

CREATE TABLE patient_reconciliation_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    source_patient_id UUID NOT NULL,
    candidate_patient_id UUID,
    decision VARCHAR(32) NOT NULL,
    previous_identity_status VARCHAR(32) NOT NULL,
    resulting_identity_status VARCHAR(32) NOT NULL,
    similarity_score DECIMAL(5, 2),
    match_reasons TEXT,
    evidence_source_type VARCHAR(48),
    evidence_reference VARCHAR(255),
    justification VARCHAR(1500) NOT NULL,
    corrected_event_id UUID,
    idempotency_key VARCHAR(120) NOT NULL,
    created_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_patient_reconciliation_source
        FOREIGN KEY (source_patient_id, organization_id)
        REFERENCES patients(id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_reconciliation_candidate
        FOREIGN KEY (candidate_patient_id, organization_id)
        REFERENCES patients(id, organization_id),
    CONSTRAINT fk_patient_reconciliation_corrected_event
        FOREIGN KEY (corrected_event_id)
        REFERENCES patient_reconciliation_events(id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_reconciliation_actor
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_patient_reconciliation_distinct_patients
        CHECK (candidate_patient_id IS NULL OR candidate_patient_id <> source_patient_id),
    CONSTRAINT chk_patient_reconciliation_score
        CHECK (similarity_score IS NULL OR (similarity_score >= 0 AND similarity_score <= 100)),
    CONSTRAINT uq_patient_reconciliation_idempotency
        UNIQUE (organization_id, idempotency_key)
);

CREATE INDEX idx_patient_reconciliation_source
    ON patient_reconciliation_events (organization_id, source_patient_id, created_at);

CREATE INDEX idx_patient_reconciliation_candidate
    ON patient_reconciliation_events (organization_id, candidate_patient_id, created_at);

CREATE TABLE patient_canonical_links (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    source_patient_id UUID NOT NULL,
    canonical_patient_id UUID NOT NULL,
    decision_event_id UUID NOT NULL,
    source_previous_identity_status VARCHAR(32) NOT NULL,
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_patient_canonical_link_source
        FOREIGN KEY (source_patient_id, organization_id)
        REFERENCES patients(id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_canonical_link_target
        FOREIGN KEY (canonical_patient_id, organization_id)
        REFERENCES patients(id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_canonical_link_event
        FOREIGN KEY (decision_event_id)
        REFERENCES patient_reconciliation_events(id) ON DELETE CASCADE,
    CONSTRAINT chk_patient_canonical_link_distinct
        CHECK (source_patient_id <> canonical_patient_id),
    CONSTRAINT uq_patient_canonical_link_source
        UNIQUE (source_patient_id)
);

CREATE INDEX idx_patient_canonical_link_target
    ON patient_canonical_links (organization_id, canonical_patient_id, linked_at);

CREATE TABLE patient_identity_aliases (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    origin_patient_id UUID NOT NULL,
    canonical_patient_id UUID NOT NULL,
    alias_type VARCHAR(32) NOT NULL,
    alias_value VARCHAR(80) NOT NULL,
    created_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_patient_identity_alias_origin
        FOREIGN KEY (origin_patient_id, organization_id)
        REFERENCES patients(id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_identity_alias_canonical
        FOREIGN KEY (canonical_patient_id, organization_id)
        REFERENCES patients(id, organization_id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_identity_alias_actor
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_patient_identity_alias
        UNIQUE (organization_id, alias_type, alias_value)
);

CREATE INDEX idx_patient_identity_alias_canonical
    ON patient_identity_aliases (organization_id, canonical_patient_id, alias_type);

INSERT INTO patient_identity_aliases (
    id,
    organization_id,
    origin_patient_id,
    canonical_patient_id,
    alias_type,
    alias_value,
    created_at,
    updated_at,
    version
)
SELECT
    p.id,
    p.organization_id,
    p.id,
    p.id,
    'URG_TEMP',
    p.temporary_patient_number,
    p.created_at,
    p.updated_at,
    0
FROM patients p
WHERE p.temporary_patient_number IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM patient_identity_aliases a
      WHERE a.organization_id = p.organization_id
        AND a.alias_type = 'URG_TEMP'
        AND a.alias_value = p.temporary_patient_number
  );