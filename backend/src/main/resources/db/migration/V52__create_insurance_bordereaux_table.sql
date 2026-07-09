CREATE SEQUENCE insurance_bordereau_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE insurance_bordereaux (
    id UUID PRIMARY KEY,
    bordereau_number VARCHAR(50) NOT NULL UNIQUE,
    insurance_convention_id UUID NOT NULL REFERENCES insurance_conventions(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE invoices ADD COLUMN insurance_bordereau_id UUID REFERENCES insurance_bordereaux(id) ON DELETE SET NULL;

CREATE INDEX idx_invoices_insurance_bordereau ON invoices(insurance_bordereau_id);
CREATE INDEX idx_insurance_bordereaux_org ON insurance_bordereaux(organization_id);
