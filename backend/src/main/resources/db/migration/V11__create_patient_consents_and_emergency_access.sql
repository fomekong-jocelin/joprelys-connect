CREATE TABLE patient_consents (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_consent_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_consent_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT uq_patient_org UNIQUE (patient_id, organization_id)
);

CREATE TABLE emergency_access_authorizations (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    doctor_email VARCHAR(320) NOT NULL,
    reason TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_emergency_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_emergency_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);
