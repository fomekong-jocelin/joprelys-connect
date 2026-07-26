ALTER TABLE ai_ambient_audio_chunks
    ADD COLUMN claim_generation BIGINT NOT NULL DEFAULT 1;

ALTER TABLE ai_ambient_audio_chunks
    ADD COLUMN claim_token UUID;

-- Existing rows do not have a live worker that owns a lease. Their immutable
-- primary key is therefore a safe non-null bootstrap token. Any real reclaim
-- rotates this value to a fresh random UUID in application code.
UPDATE ai_ambient_audio_chunks
SET claim_token = id
WHERE claim_token IS NULL;

ALTER TABLE ai_ambient_audio_chunks
    ALTER COLUMN claim_token SET NOT NULL;

ALTER TABLE ai_ambient_audio_chunks
    ADD CONSTRAINT ck_ai_ambient_audio_chunk_claim_generation
        CHECK (claim_generation >= 1);
