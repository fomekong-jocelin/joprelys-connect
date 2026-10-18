-- ADR-20261017 D-11 à D-17 : facturation par sources, tarifs obligatoires, acomptes, avoirs approuvés.

-- D-11 / D-15 : chaque ligne connaît sa source et son tarif.
ALTER TABLE invoice_items ADD COLUMN source_type VARCHAR(40);
ALTER TABLE invoice_items ADD COLUMN source_id UUID;
ALTER TABLE invoice_items ADD COLUMN tariff_key VARCHAR(100);
ALTER TABLE invoice_items ADD COLUMN tariff_missing BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_invoice_items_source ON invoice_items (source_type, source_id);

-- D-12 : la pharmacie qui délivre est connue ; seule une délivrance interne est facturée.
ALTER TABLE prescription_dispensations ADD COLUMN dispensing_organization_id UUID;

-- D-13 / D-17 : paramètres financiers de l'établissement.
ALTER TABLE organizations ADD COLUMN lab_payment_before_sampling BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE organizations ADD COLUMN credit_note_approval_threshold NUMERIC(19, 4) NOT NULL DEFAULT 50000;

-- D-17 : avoir émis, approuvé, remboursé.
ALTER TABLE credit_notes ADD COLUMN issued_by UUID;
ALTER TABLE credit_notes ADD COLUMN approved_by UUID;
ALTER TABLE credit_notes ADD COLUMN approved_at TIMESTAMP;
ALTER TABLE credit_notes ADD COLUMN refunded_amount DOUBLE PRECISION NOT NULL DEFAULT 0;

-- D-16 : acomptes patient encaissés avant facturation.
CREATE TABLE patient_deposits (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    deposit_number VARCHAR(50) NOT NULL UNIQUE,
    patient_id UUID NOT NULL REFERENCES patients(id),
    visit_id UUID,
    amount NUMERIC(19, 4) NOT NULL,
    remaining_amount NUMERIC(19, 4) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    note VARCHAR(500),
    cash_session_id UUID NOT NULL,
    received_by_user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_patient_deposits_amounts CHECK (amount > 0 AND remaining_amount >= 0 AND remaining_amount <= amount)
);
CREATE INDEX idx_patient_deposits_patient ON patient_deposits (organization_id, patient_id);

ALTER TABLE payments ADD COLUMN deposit_id UUID REFERENCES patient_deposits(id);

CREATE SEQUENCE IF NOT EXISTS patient_deposit_number_seq START WITH 1 INCREMENT BY 1;
