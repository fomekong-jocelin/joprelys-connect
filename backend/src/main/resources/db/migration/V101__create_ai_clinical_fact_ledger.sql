CREATE TABLE ai_clinical_facts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    visit_id UUID NOT NULL REFERENCES visits(id),
    sequence_no BIGINT NOT NULL,
    source_event_id VARCHAR(200) NOT NULL,
    fact_type VARCHAR(32) NOT NULL,
    authority VARCHAR(32) NOT NULL,
    concept_code VARCHAR(64) NOT NULL,
    concept_text VARCHAR(256) NOT NULL,
    polarity VARCHAR(16) NOT NULL,
    value_primary VARCHAR(128),
    value_secondary VARCHAR(128),
    unit_code VARCHAR(32),
    temporality_text VARCHAR(256),
    laterality VARCHAR(16) NOT NULL,
    frequency_text VARCHAR(128),
    route_text VARCHAR(64),
    fact_status VARCHAR(16) NOT NULL,
    supersedes_fact_id UUID REFERENCES ai_clinical_facts(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_clinical_fact_sequence CHECK (sequence_no > 0),
    CONSTRAINT ck_ai_clinical_fact_type CHECK (fact_type IN (
        'SYMPTOM', 'VITAL', 'MEDICATION', 'ALLERGY', 'HISTORY', 'ASSESSMENT', 'PLAN', 'ORDER'
    )),
    CONSTRAINT ck_ai_clinical_fact_authority CHECK (authority IN (
        'PATIENT_REPORTED', 'CLINICIAN_OBSERVED', 'CLINICIAN_DECISION'
    )),
    CONSTRAINT ck_ai_clinical_fact_polarity CHECK (polarity IN ('POSITIVE', 'NEGATIVE', 'UNCERTAIN')),
    CONSTRAINT ck_ai_clinical_fact_laterality CHECK (laterality IN ('LEFT', 'RIGHT', 'BILATERAL', 'UNSPECIFIED')),
    CONSTRAINT ck_ai_clinical_fact_status CHECK (fact_status IN ('ASSERTED', 'RETRACTED')),
    CONSTRAINT uq_ai_clinical_fact_sequence UNIQUE (organization_id, visit_id, sequence_no),
    CONSTRAINT uq_ai_clinical_fact_event UNIQUE (organization_id, visit_id, source_event_id),
    CONSTRAINT uq_ai_clinical_fact_successor UNIQUE (supersedes_fact_id)
);

CREATE TABLE ai_clinical_fact_evidence (
    id UUID PRIMARY KEY,
    fact_id UUID NOT NULL REFERENCES ai_clinical_facts(id) ON DELETE CASCADE,
    transcript_item_id UUID NOT NULL REFERENCES ai_ambient_transcript_items(id),
    quote_start_char INTEGER NOT NULL,
    quote_end_char INTEGER NOT NULL,
    quote_text TEXT NOT NULL,
    primary_support BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_clinical_fact_evidence_range CHECK (
        quote_start_char >= 0 AND quote_end_char > quote_start_char
    ),
    CONSTRAINT uq_ai_clinical_fact_evidence UNIQUE (
        fact_id, transcript_item_id, quote_start_char, quote_end_char
    )
);

CREATE INDEX idx_ai_clinical_fact_visit
    ON ai_clinical_facts (organization_id, visit_id, sequence_no);

CREATE INDEX idx_ai_clinical_fact_type
    ON ai_clinical_facts (organization_id, visit_id, fact_type, fact_status);

CREATE INDEX idx_ai_clinical_fact_evidence_transcript
    ON ai_clinical_fact_evidence (transcript_item_id);
