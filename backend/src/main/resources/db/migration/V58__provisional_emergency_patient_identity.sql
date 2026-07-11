ALTER TABLE patients ALTER COLUMN full_name DROP NOT NULL;
ALTER TABLE patients ALTER COLUMN gender DROP NOT NULL;
ALTER TABLE patients ALTER COLUMN birth_date DROP NOT NULL;
ALTER TABLE patients ALTER COLUMN city DROP NOT NULL;

ALTER TABLE patients ADD COLUMN identity_status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED';
ALTER TABLE patients ADD COLUMN temporary_patient_number VARCHAR(40);
ALTER TABLE patients ADD COLUMN identity_confidence_level VARCHAR(24) NOT NULL DEFAULT 'VERIFIED';
ALTER TABLE patients ADD COLUMN apparent_gender VARCHAR(32);
ALTER TABLE patients ADD COLUMN estimated_age_range VARCHAR(64);
ALTER TABLE patients ADD COLUMN physical_description TEXT;
ALTER TABLE patients ADD COLUMN found_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE patients ADD COLUMN found_location VARCHAR(255);

CREATE UNIQUE INDEX ux_patients_temporary_patient_number
    ON patients (temporary_patient_number);
CREATE INDEX idx_patients_identity_status
    ON patients (organization_id, identity_status);

CREATE TABLE patient_number_counters (
    counter_key VARCHAR(64) PRIMARY KEY,
    next_value BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE patient_identity_declarations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    field_name VARCHAR(80) NOT NULL,
    declared_value TEXT NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_details VARCHAR(500),
    confidence_level VARCHAR(24) NOT NULL,
    verification_status VARCHAR(24) NOT NULL,
    declared_by UUID NOT NULL,
    declared_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_identity_declaration_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_identity_declaration_actor
        FOREIGN KEY (declared_by) REFERENCES users(id)
);

CREATE INDEX idx_identity_declarations_patient
    ON patient_identity_declarations (organization_id, patient_id, declared_at);

CREATE TABLE patient_identity_status_history (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    changed_by UUID NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identity_status_history_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_identity_status_history_actor
        FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE INDEX idx_identity_status_history_patient
    ON patient_identity_status_history (organization_id, patient_id, changed_at);
