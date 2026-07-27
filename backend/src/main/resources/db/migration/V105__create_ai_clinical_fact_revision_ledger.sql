ALTER TABLE ai_clinical_facts
    ADD CONSTRAINT uq_ai_clinical_fact_scope UNIQUE (id, organization_id, visit_id);

CREATE TABLE ai_clinical_fact_revision_batches (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    visit_id UUID NOT NULL REFERENCES visits(id),
    revision_request_id UUID NOT NULL,
    base_projection_version VARCHAR(128) NOT NULL,
    result_projection_version VARCHAR(128) NOT NULL,
    request_sha256 VARCHAR(64) NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_clinical_fact_revision_hash CHECK (request_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_ai_clinical_fact_revision_base_version CHECK (length(trim(base_projection_version)) > 0),
    CONSTRAINT ck_ai_clinical_fact_revision_result_version CHECK (length(trim(result_projection_version)) > 0),
    CONSTRAINT uq_ai_clinical_fact_revision_request UNIQUE (
        organization_id, visit_id, revision_request_id
    ),
    CONSTRAINT uq_ai_clinical_fact_revision_batch_scope UNIQUE (
        id, organization_id, visit_id
    )
);

CREATE TABLE ai_clinical_fact_revision_operations (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    visit_id UUID NOT NULL REFERENCES visits(id),
    operation_request_id UUID NOT NULL,
    position_no INTEGER NOT NULL,
    operation_type VARCHAR(16) NOT NULL,
    target_fact_id UUID,
    result_fact_id UUID,
    retraction_reason VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ai_clinical_fact_revision_batch_scope FOREIGN KEY (
        batch_id, organization_id, visit_id
    ) REFERENCES ai_clinical_fact_revision_batches (
        id, organization_id, visit_id
    ) ON DELETE CASCADE,
    CONSTRAINT fk_ai_clinical_fact_revision_target_scope FOREIGN KEY (
        target_fact_id, organization_id, visit_id
    ) REFERENCES ai_clinical_facts (
        id, organization_id, visit_id
    ),
    CONSTRAINT fk_ai_clinical_fact_revision_result_scope FOREIGN KEY (
        result_fact_id, organization_id, visit_id
    ) REFERENCES ai_clinical_facts (
        id, organization_id, visit_id
    ),
    CONSTRAINT ck_ai_clinical_fact_revision_position CHECK (position_no >= 0),
    CONSTRAINT ck_ai_clinical_fact_revision_type CHECK (
        operation_type IN ('KEEP', 'ADD', 'REPLACE', 'RETRACT')
    ),
    CONSTRAINT ck_ai_clinical_fact_retraction_reason CHECK (
        retraction_reason IS NULL OR retraction_reason IN (
            'EXPLICIT_CORRECTION', 'EXPLICIT_NEGATION', 'CLINICIAN_CANCELLATION'
        )
    ),
    CONSTRAINT ck_ai_clinical_fact_revision_shape CHECK (
        (operation_type = 'KEEP'
            AND target_fact_id IS NOT NULL
            AND result_fact_id IS NULL
            AND retraction_reason IS NULL)
        OR
        (operation_type = 'ADD'
            AND target_fact_id IS NULL
            AND result_fact_id IS NOT NULL
            AND retraction_reason IS NULL)
        OR
        (operation_type = 'REPLACE'
            AND target_fact_id IS NOT NULL
            AND result_fact_id IS NOT NULL
            AND retraction_reason IS NULL)
        OR
        (operation_type = 'RETRACT'
            AND target_fact_id IS NOT NULL
            AND result_fact_id IS NULL
            AND retraction_reason IS NOT NULL)
    ),
    CONSTRAINT uq_ai_clinical_fact_revision_position UNIQUE (batch_id, position_no),
    CONSTRAINT uq_ai_clinical_fact_revision_operation UNIQUE (
        organization_id, visit_id, operation_request_id
    ),
    CONSTRAINT uq_ai_clinical_fact_revision_operation_scope UNIQUE (
        id, organization_id, visit_id
    )
);

CREATE TABLE ai_clinical_fact_revision_evidence (
    id UUID PRIMARY KEY,
    operation_id UUID NOT NULL,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    visit_id UUID NOT NULL REFERENCES visits(id),
    transcript_item_id UUID NOT NULL REFERENCES ai_ambient_transcript_items(id),
    quote_start_char INTEGER NOT NULL,
    quote_end_char INTEGER NOT NULL,
    quote_text TEXT NOT NULL,
    primary_support BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ai_clinical_fact_revision_evidence_scope FOREIGN KEY (
        operation_id, organization_id, visit_id
    ) REFERENCES ai_clinical_fact_revision_operations (
        id, organization_id, visit_id
    ) ON DELETE CASCADE,
    CONSTRAINT ck_ai_clinical_fact_revision_evidence_range CHECK (
        quote_start_char >= 0 AND quote_end_char > quote_start_char
    ),
    CONSTRAINT uq_ai_clinical_fact_revision_evidence UNIQUE (
        operation_id, transcript_item_id, quote_start_char, quote_end_char
    )
);

CREATE INDEX idx_ai_clinical_fact_revision_visit
    ON ai_clinical_fact_revision_batches (organization_id, visit_id, created_at DESC);

CREATE INDEX idx_ai_clinical_fact_revision_retracted_target
    ON ai_clinical_fact_revision_operations (
        organization_id, visit_id, operation_type, target_fact_id
    );

CREATE INDEX idx_ai_clinical_fact_revision_evidence_transcript
    ON ai_clinical_fact_revision_evidence (organization_id, visit_id, transcript_item_id);
