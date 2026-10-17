-- ADR-20261017 D-02 : la délivrance ne modifie plus le cycle de vie de l'ordonnance.
ALTER TABLE prescriptions ADD COLUMN dispensation_status VARCHAR(20) NOT NULL DEFAULT 'NONE';

UPDATE prescriptions SET dispensation_status = 'PARTIAL', status = 'ACTIVE' WHERE status = 'PARTIALLY_DISPENSED';
UPDATE prescriptions SET dispensation_status = 'FULL', status = 'ACTIVE' WHERE status = 'FULLY_DISPENSED';

ALTER TABLE prescriptions ADD CONSTRAINT ck_prescriptions_dispensation_status
    CHECK (dispensation_status IN ('NONE', 'PARTIAL', 'FULL'));
