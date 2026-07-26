ALTER TABLE ai_ambient_transcript_items
    ADD CONSTRAINT uq_ai_ambient_transcript_single_successor
    UNIQUE (supersedes_item_id);
