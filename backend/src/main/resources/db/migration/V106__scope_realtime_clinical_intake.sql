ALTER TABLE ai_realtime_clinical_intake
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'CONSULTATION';

ALTER TABLE ai_realtime_clinical_intake
    ADD CONSTRAINT ck_ai_realtime_intake_source
    CHECK (source IN ('CONSULTATION', 'VITALS'));

ALTER TABLE ai_realtime_clinical_intake
    DROP CONSTRAINT uq_ai_realtime_intake_event;
ALTER TABLE ai_realtime_clinical_intake
    DROP CONSTRAINT uq_ai_realtime_intake_item;

ALTER TABLE ai_realtime_clinical_intake
    ADD CONSTRAINT uq_ai_realtime_intake_event
    UNIQUE (organization_id, visit_id, source, event_id);
ALTER TABLE ai_realtime_clinical_intake
    ADD CONSTRAINT uq_ai_realtime_intake_item
    UNIQUE (organization_id, visit_id, source, item_id);

CREATE INDEX idx_ai_realtime_intake_visit_source_sequence
    ON ai_realtime_clinical_intake (organization_id, visit_id, source, sequence_no);
