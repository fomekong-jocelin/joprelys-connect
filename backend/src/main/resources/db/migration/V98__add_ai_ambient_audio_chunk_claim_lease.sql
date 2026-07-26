ALTER TABLE ai_ambient_audio_chunks
    ADD COLUMN claim_generation BIGINT NOT NULL DEFAULT 1;

ALTER TABLE ai_ambient_audio_chunks
    ADD COLUMN claim_token UUID;

UPDATE ai_ambient_audio_chunks
SET claim_token = (
    substr(md5(id::text), 1, 8) || '-' ||
    substr(md5(id::text), 9, 4) || '-' ||
    substr(md5(id::text), 13, 4) || '-' ||
    substr(md5(id::text), 17, 4) || '-' ||
    substr(md5(id::text), 21, 12)
)::uuid
WHERE claim_token IS NULL;

ALTER TABLE ai_ambient_audio_chunks
    ALTER COLUMN claim_token SET NOT NULL;

ALTER TABLE ai_ambient_audio_chunks
    ADD CONSTRAINT ck_ai_ambient_audio_chunk_claim_generation
        CHECK (claim_generation >= 1);
