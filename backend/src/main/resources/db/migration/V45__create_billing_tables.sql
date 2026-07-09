CREATE SEQUENCE invoice_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE insurance_conventions (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    coverage_percentage DOUBLE PRECISION NOT NULL DEFAULT 0.8,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE tariff_grid (
    id UUID PRIMARY KEY,
    key_letter VARCHAR(100) NOT NULL,
    unit_value DOUBLE PRECISION NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_key_letter_org UNIQUE (key_letter, organization_id)
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    visit_id UUID REFERENCES visits(id),
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    insurance_convention_id UUID REFERENCES insurance_conventions(id),
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    patient_share DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    insurance_share DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    label VARCHAR(250) NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    coefficient DOUBLE PRECISION,
    total_item_amount DOUBLE PRECISION NOT NULL,
    organization_id UUID NOT NULL
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    amount DOUBLE PRECISION NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    received_by_user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_invoices_patient ON invoices(patient_id);
CREATE INDEX idx_invoices_visit ON invoices(visit_id);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);
