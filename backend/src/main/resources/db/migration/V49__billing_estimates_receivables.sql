ALTER TABLE invoices ADD COLUMN validated_at TIMESTAMP;
ALTER TABLE invoices ADD COLUMN validated_by_user_id UUID;
ALTER TABLE invoices ADD COLUMN discount_amount DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE invoices ADD COLUMN discount_reason VARCHAR(255);
ALTER TABLE invoices ADD COLUMN version BIGINT DEFAULT 0;

CREATE TABLE estimates (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    visit_id UUID REFERENCES visits(id) ON DELETE CASCADE,
    estimate_number VARCHAR(50) NOT NULL UNIQUE,
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    patient_share DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    insurance_share DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE estimate_items (
    id UUID PRIMARY KEY,
    estimate_id UUID NOT NULL REFERENCES estimates(id) ON DELETE CASCADE,
    label VARCHAR(250) NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    organization_id UUID NOT NULL
);

CREATE TABLE credit_notes (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    credit_note_number VARCHAR(50) NOT NULL UNIQUE,
    amount DOUBLE PRECISION NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE receivables (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    debtor_type VARCHAR(50) NOT NULL,
    debtor_id UUID NOT NULL,
    total_amount DOUBLE PRECISION NOT NULL,
    paid_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    due_date TIMESTAMP,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_estimates_patient ON estimates(patient_id);
CREATE INDEX idx_estimates_visit ON estimates(visit_id);
CREATE INDEX idx_credit_notes_invoice ON credit_notes(invoice_id);
CREATE INDEX idx_receivables_invoice ON receivables(invoice_id);
CREATE INDEX idx_receivables_debtor ON receivables(debtor_id);
