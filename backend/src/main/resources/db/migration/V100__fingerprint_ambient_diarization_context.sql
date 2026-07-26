ALTER TABLE ai_ambient_audio_chunks
    ADD COLUMN diarization_context_sha256 VARCHAR(64) NOT NULL
    DEFAULT 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
