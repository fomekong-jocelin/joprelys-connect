CREATE TABLE ai_ambient_note_revisions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    visit_id UUID NOT NULL REFERENCES visits(id),
    revision_no BIGINT NOT NULL,
    template_code VARCHAR(32) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    transcript_max_sequence BIGINT NOT NULL,
    model_name VARCHAR(128),
    tokens_used INTEGER,
    generated_by_user_id UUID NOT NULL REFERENCES users(id),
    supersedes_note_id UUID REFERENCES ai_ambient_note_revisions(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE,
    decided_by_user_id UUID REFERENCES users(id),
    CONSTRAINT ck_ai_ambient_note_template CHECK (template_code IN ('SOAP', 'APSO', 'MULTI_SECTION')),
    CONSTRAINT ck_ai_ambient_note_status CHECK (status IN ('GENERATED', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT ck_ai_ambient_note_sequence CHECK (revision_no > 0 AND transcript_max_sequence >= 0),
    CONSTRAINT uq_ai_ambient_note_revision UNIQUE (organization_id, visit_id, revision_no)
);

CREATE TABLE ai_ambient_note_statements (
    id UUID PRIMARY KEY,
    note_revision_id UUID NOT NULL REFERENCES ai_ambient_note_revisions(id) ON DELETE CASCADE,
    section_code VARCHAR(32) NOT NULL,
    statement_order INTEGER NOT NULL,
    statement_text TEXT NOT NULL,
    critical BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_ambient_note_statement_order CHECK (statement_order > 0),
    CONSTRAINT uq_ai_ambient_note_statement_order UNIQUE (note_revision_id, section_code, statement_order)
);

CREATE TABLE ai_ambient_note_evidence (
    note_statement_id UUID NOT NULL REFERENCES ai_ambient_note_statements(id) ON DELETE CASCADE,
    transcript_item_id UUID NOT NULL REFERENCES ai_ambient_transcript_items(id),
    PRIMARY KEY (note_statement_id, transcript_item_id)
);

CREATE INDEX idx_ai_ambient_note_visit
    ON ai_ambient_note_revisions (organization_id, visit_id, revision_no DESC);

CREATE INDEX idx_ai_ambient_note_status
    ON ai_ambient_note_revisions (organization_id, visit_id, status);

CREATE INDEX idx_ai_ambient_note_statement_revision
    ON ai_ambient_note_statements (note_revision_id, section_code, statement_order);

CREATE INDEX idx_ai_ambient_note_evidence_transcript
    ON ai_ambient_note_evidence (transcript_item_id);
