CREATE TABLE ai_ambient_audio_chunks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    visit_id UUID NOT NULL REFERENCES visits(id),
    chunk_id VARCHAR(160) NOT NULL,
    audio_sha256 VARCHAR(64) NOT NULL,
    start_offset_ms BIGINT NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    claimed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(128),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_ambient_audio_chunk_offset CHECK (start_offset_ms >= 0),
    CONSTRAINT ck_ai_ambient_audio_chunk_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT uq_ai_ambient_audio_chunk UNIQUE (organization_id, visit_id, chunk_id)
);

CREATE INDEX idx_ai_ambient_audio_chunk_visit_status
    ON ai_ambient_audio_chunks (organization_id, visit_id, status, start_offset_ms);

CREATE INDEX idx_ai_ambient_audio_chunk_claimed
    ON ai_ambient_audio_chunks (status, claimed_at);
