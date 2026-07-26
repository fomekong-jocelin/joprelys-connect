CREATE TABLE ai_ambient_transcript_items (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    visit_id UUID NOT NULL REFERENCES visits(id),
    sequence_no BIGINT NOT NULL,
    source_event_id VARCHAR(200) NOT NULL,
    source VARCHAR(32) NOT NULL,
    speaker_type VARCHAR(16) NOT NULL,
    speaker_label VARCHAR(64),
    transcript_text TEXT NOT NULL,
    locale VARCHAR(16) NOT NULL,
    start_offset_ms BIGINT NOT NULL,
    end_offset_ms BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    supersedes_item_id UUID REFERENCES ai_ambient_transcript_items(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_ambient_transcript_offsets
        CHECK (start_offset_ms >= 0 AND end_offset_ms >= start_offset_ms),
    CONSTRAINT ck_ai_ambient_transcript_source
        CHECK (source IN ('AMBIENT_DIARIZED', 'MANUAL_CORRECTION')),
    CONSTRAINT ck_ai_ambient_transcript_speaker
        CHECK (speaker_type IN ('DOCTOR', 'PATIENT', 'UNSPECIFIED')),
    CONSTRAINT ck_ai_ambient_transcript_status
        CHECK (status IN ('FINAL', 'REJECTED')),
    CONSTRAINT uq_ai_ambient_transcript_event
        UNIQUE (organization_id, visit_id, source_event_id),
    CONSTRAINT uq_ai_ambient_transcript_sequence
        UNIQUE (organization_id, visit_id, sequence_no)
);

CREATE INDEX idx_ai_ambient_transcript_visit_time
    ON ai_ambient_transcript_items (organization_id, visit_id, start_offset_ms, sequence_no);

CREATE INDEX idx_ai_ambient_transcript_visit_status
    ON ai_ambient_transcript_items (organization_id, visit_id, status);

CREATE INDEX idx_ai_ambient_transcript_supersedes
    ON ai_ambient_transcript_items (supersedes_item_id);
