CREATE TABLE emergency_admission_requests (
    request_id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    emergency_id UUID,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_emergency_admission_request_emergency
        FOREIGN KEY (emergency_id) REFERENCES emergencies(id)
);

CREATE UNIQUE INDEX ux_emergency_admission_request_emergency
    ON emergency_admission_requests (emergency_id);
CREATE INDEX idx_emergency_admission_request_tenant
    ON emergency_admission_requests (organization_id, status);
