ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN discarded_by_user_id UUID;

ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN discarded_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE ai_realtime_clinical_intake
    ADD CONSTRAINT fk_ai_realtime_intake_discarded_by
        FOREIGN KEY (discarded_by_user_id) REFERENCES users(id);

ALTER TABLE ai_realtime_clinical_intake
    DROP CONSTRAINT ck_ai_realtime_intake_capture_status;

ALTER TABLE ai_realtime_clinical_intake
    ADD CONSTRAINT ck_ai_realtime_intake_capture_status
        CHECK (capture_status IN ('PENDING', 'ANALYZED', 'CONSUMED', 'DISCARDED'));
