CREATE TABLE patients (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    global_patient_number VARCHAR(50) NOT NULL UNIQUE,
    local_patient_number VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    birth_date DATE NOT NULL,
    phone VARCHAR(50) NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    address VARCHAR(255),
    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(50),
    allergies TEXT,
    medical_history TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_patients_organization_id ON patients (organization_id);
CREATE INDEX idx_patients_global_patient_number ON patients (global_patient_number);
CREATE INDEX idx_patients_local_patient_number ON patients (local_patient_number);
CREATE INDEX idx_patients_full_name ON patients (full_name);
