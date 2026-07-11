ALTER TABLE emergencies ADD COLUMN admission_request_id UUID;

CREATE UNIQUE INDEX ux_emergencies_admission_request
    ON emergencies (organization_id, admission_request_id);
