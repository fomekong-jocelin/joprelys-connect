ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN original_transcript_text TEXT,
    ADD COLUMN correction_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN corrected_by_user_id UUID REFERENCES users(id),
    ADD COLUMN corrected_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN capture_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN analyzed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN consumed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE ai_realtime_clinical_intake
    ADD CONSTRAINT ck_ai_realtime_intake_correction_count CHECK (correction_count >= 0),
    ADD CONSTRAINT ck_ai_realtime_intake_capture_status
        CHECK (capture_status IN ('PENDING', 'ANALYZED', 'CONSUMED'));

CREATE INDEX idx_ai_realtime_intake_active_capture
    ON ai_realtime_clinical_intake (organization_id, visit_id, source, capture_status, sequence_no);
