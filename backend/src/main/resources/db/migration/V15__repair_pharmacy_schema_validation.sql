ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS prescription_number VARCHAR(50);
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS pin_code VARCHAR(4);
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_prescriptions_prescription_number
    ON prescriptions (prescription_number);

CREATE TABLE IF NOT EXISTS prescription_dispensations (
    id UUID PRIMARY KEY,
    prescription_id UUID NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    dispensed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    pharmacy_name VARCHAR(200) NOT NULL,
    pharmacist_license VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dispensation_items (
    id UUID PRIMARY KEY,
    dispensation_id UUID NOT NULL REFERENCES prescription_dispensations(id) ON DELETE CASCADE,
    prescription_item_id UUID NOT NULL REFERENCES prescription_items(id),
    quantity_dispensed INT NOT NULL,
    substituted_with VARCHAR(200)
);

CREATE INDEX IF NOT EXISTS idx_prescription_dispensations_prescription_id
    ON prescription_dispensations (prescription_id);
CREATE INDEX IF NOT EXISTS idx_dispensation_items_dispensation_id
    ON dispensation_items (dispensation_id);
CREATE INDEX IF NOT EXISTS idx_dispensation_items_prescription_item_id
    ON dispensation_items (prescription_item_id);
