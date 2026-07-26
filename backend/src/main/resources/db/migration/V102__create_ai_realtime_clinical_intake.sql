CREATE TABLE ai_realtime_clinical_intake (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    visit_id UUID NOT NULL REFERENCES visits(id),
    sequence_no BIGINT NOT NULL,
    event_id VARCHAR(200) NOT NULL,
    item_id VARCHAR(200),
    transcript_text TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_realtime_intake_sequence CHECK (sequence_no > 0),
    CONSTRAINT ck_ai_realtime_intake_confidence CHECK (confidence >= 0.0 AND confidence <= 1.0),
    CONSTRAINT uq_ai_realtime_intake_event UNIQUE (organization_id, visit_id, event_id),
    CONSTRAINT uq_ai_realtime_intake_item UNIQUE (organization_id, visit_id, item_id)
);

CREATE INDEX idx_ai_realtime_intake_visit_sequence
    ON ai_realtime_clinical_intake (organization_id, visit_id, sequence_no);
