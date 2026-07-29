ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN original_transcript_text TEXT;

ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN correction_count INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN corrected_by_user_id UUID;

ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN corrected_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN capture_status VARCHAR(16) DEFAULT 'PENDING' NOT NULL;

ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN analyzed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN consumed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE ai_realtime_clinical_intake
    ADD CONSTRAINT fk_ai_realtime_intake_corrected_by
        FOREIGN KEY (corrected_by_user_id) REFERENCES users(id);

ALTER TABLE ai_realtime_clinical_intake
    ADD CONSTRAINT ck_ai_realtime_intake_correction_count
        CHECK (correction_count >= 0);

ALTER TABLE ai_realtime_clinical_intake
    ADD CONSTRAINT ck_ai_realtime_intake_capture_status
        CHECK (capture_status IN ('PENDING', 'ANALYZED', 'CONSUMED'));

CREATE INDEX idx_ai_realtime_intake_active_capture
    ON ai_realtime_clinical_intake (organization_id, visit_id, source, capture_status, sequence_no);
