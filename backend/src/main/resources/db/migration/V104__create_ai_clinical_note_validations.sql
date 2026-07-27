CREATE TABLE ai_clinical_note_validations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    visit_id UUID NOT NULL REFERENCES visits(id),
    validation_request_id UUID NOT NULL,
    projection_version VARCHAR(128) NOT NULL,
    projection_schema_version VARCHAR(64) NOT NULL,
    max_fact_sequence BIGINT NOT NULL,
    validated_by_user_id UUID NOT NULL REFERENCES users(id),
    validated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_note_validation_fact_sequence CHECK (max_fact_sequence >= 0),
    CONSTRAINT uq_ai_note_validation_request UNIQUE (
        organization_id, visit_id, validation_request_id
    ),
    CONSTRAINT uq_ai_note_validation_projection_user UNIQUE (
        organization_id, visit_id, projection_version, validated_by_user_id
    )
);

CREATE TABLE ai_clinical_note_validation_facts (
    id UUID PRIMARY KEY,
    validation_id UUID NOT NULL REFERENCES ai_clinical_note_validations(id) ON DELETE CASCADE,
    fact_id UUID NOT NULL REFERENCES ai_clinical_facts(id),
    fact_sequence BIGINT NOT NULL,
    section_code VARCHAR(48) NOT NULL,
    position_no INTEGER NOT NULL,
    CONSTRAINT ck_ai_note_validation_fact_ref_sequence CHECK (fact_sequence > 0),
    CONSTRAINT ck_ai_note_validation_position CHECK (position_no >= 0),
    CONSTRAINT ck_ai_note_validation_section CHECK (section_code IN (
        'HISTORY_OF_PRESENT_ILLNESS',
        'MEDICAL_HISTORY',
        'ALLERGIES',
        'VITALS',
        'ASSESSMENT',
        'MEDICATIONS',
        'ORDERS',
        'PLAN'
    )),
    CONSTRAINT uq_ai_note_validation_fact UNIQUE (validation_id, fact_id),
    CONSTRAINT uq_ai_note_validation_position UNIQUE (
        validation_id, section_code, position_no
    )
);

CREATE INDEX idx_ai_note_validation_visit
    ON ai_clinical_note_validations (organization_id, visit_id, validated_at);

CREATE INDEX idx_ai_note_validation_fact
    ON ai_clinical_note_validation_facts (fact_id);
