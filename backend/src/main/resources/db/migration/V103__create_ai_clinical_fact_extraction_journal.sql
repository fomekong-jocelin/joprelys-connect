CREATE TABLE ai_clinical_fact_extractions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    visit_id UUID NOT NULL REFERENCES visits(id),
    transcript_item_id UUID NOT NULL REFERENCES ai_ambient_transcript_items(id),
    transcript_sha256 VARCHAR(64) NOT NULL,
    extractor_version VARCHAR(32) NOT NULL,
    model VARCHAR(128),
    candidate_count INTEGER NOT NULL,
    accepted_count INTEGER NOT NULL,
    rejected_count INTEGER NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_fact_extraction_counts CHECK (
        candidate_count >= 0
        AND accepted_count >= 0
        AND rejected_count >= 0
        AND accepted_count + rejected_count = candidate_count
    ),
    CONSTRAINT uq_ai_fact_extraction_item_version UNIQUE (
        organization_id, visit_id, transcript_item_id, extractor_version
    )
);

CREATE INDEX idx_ai_fact_extraction_visit
    ON ai_clinical_fact_extractions (organization_id, visit_id, completed_at);
