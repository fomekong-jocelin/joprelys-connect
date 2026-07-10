CREATE TABLE receivable_reminders (
    id UUID PRIMARY KEY,
    receivable_id UUID NOT NULL REFERENCES receivables(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL, -- PHONE_CALL, EMAIL, LETTER, VISIT
    status VARCHAR(50) NOT NULL, -- PENDING, PROMISED_PAYMENT, DISPUTE, UNREACHABLE
    notes TEXT,
    actor_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    organization_id UUID NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_receivable_reminders_receivable ON receivable_reminders(receivable_id);
