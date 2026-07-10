ALTER TABLE insurance_bordereaux
    ALTER COLUMN total_amount SET DATA TYPE NUMERIC(19,4);

ALTER TABLE insurance_bordereaux ADD COLUMN IF NOT EXISTS accepted_amount NUMERIC(19,4);
ALTER TABLE insurance_bordereaux ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE insurance_bordereaux ADD COLUMN IF NOT EXISTS insurer_reference VARCHAR(120);
ALTER TABLE insurance_bordereaux ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(120);
ALTER TABLE insurance_bordereaux ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);
ALTER TABLE insurance_bordereaux ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE insurance_bordereaux ADD COLUMN IF NOT EXISTS received_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE insurance_bordereaux ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE insurance_bordereaux ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE insurance_bordereaux ADD COLUMN IF NOT EXISTS settled_at TIMESTAMP WITH TIME ZONE;

UPDATE insurance_bordereaux
SET accepted_amount = total_amount,
    paid_amount = total_amount,
    settled_at = COALESCE(settled_at, updated_at)
WHERE status = 'PAID';
