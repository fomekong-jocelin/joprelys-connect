CREATE TABLE cash_registers (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE cash_register_sessions (
    id UUID PRIMARY KEY,
    cash_register_id UUID NOT NULL REFERENCES cash_registers(id) ON DELETE CASCADE,
    opened_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    opened_at TIMESTAMP NOT NULL,
    opening_balance DOUBLE PRECISION NOT NULL,
    closed_by_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    closed_at TIMESTAMP,
    closing_balance DOUBLE PRECISION,
    declared_balance DOUBLE PRECISION,
    discrepancy_amount DOUBLE PRECISION,
    discrepancy_reason VARCHAR(250),
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE cash_movements (
    id UUID PRIMARY KEY,
    cash_register_session_id UUID NOT NULL REFERENCES cash_register_sessions(id) ON DELETE CASCADE,
    movement_type VARCHAR(50) NOT NULL, -- IN, OUT, TRANSFER_TO_BANK
    amount DOUBLE PRECISION NOT NULL,
    description VARCHAR(250) NOT NULL,
    payment_method VARCHAR(50) NOT NULL, -- CASH, CHECK, BANK_TRANSFER
    reference_number VARCHAR(100),
    created_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE payment_receipts (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    receipt_number VARCHAR(50) NOT NULL UNIQUE,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE payments ADD COLUMN cash_session_id UUID REFERENCES cash_register_sessions(id) ON DELETE SET NULL;

CREATE INDEX idx_cash_registers_org ON cash_registers(organization_id);
CREATE INDEX idx_cash_sessions_register ON cash_register_sessions(cash_register_id);
CREATE INDEX idx_cash_sessions_user ON cash_register_sessions(opened_by_user_id);
CREATE INDEX idx_cash_movements_session ON cash_movements(cash_register_session_id);
CREATE INDEX idx_payment_receipts_payment ON payment_receipts(payment_id);
CREATE INDEX idx_payments_session ON payments(cash_session_id);

CREATE SEQUENCE receipt_number_seq START WITH 1 INCREMENT BY 1;
