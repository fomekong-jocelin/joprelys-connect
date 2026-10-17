-- ADR-20261017 D-03 : idempotence et forçage motivé des administrations.
ALTER TABLE medication_administrations ADD COLUMN client_request_id UUID;
ALTER TABLE medication_administrations ADD COLUMN override_reason VARCHAR(500);
ALTER TABLE medication_administrations ADD CONSTRAINT uq_medication_administration_client_request
    UNIQUE (organization_id, client_request_id);
