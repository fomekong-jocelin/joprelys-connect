-- STORY-2305 / #46
-- Continuité explicite urgence -> hospitalisation et état financier différé.

ALTER TABLE hospitalizations
    ADD COLUMN emergency_id UUID;

ALTER TABLE hospitalizations
    ADD CONSTRAINT fk_hospitalizations_emergency
        FOREIGN KEY (emergency_id) REFERENCES emergencies(id);

CREATE INDEX idx_hospitalizations_emergency_id
    ON hospitalizations(emergency_id);

ALTER TABLE invoices
    ADD COLUMN regularization_status VARCHAR(40) NOT NULL DEFAULT 'RESOLVED';

ALTER TABLE invoices
    ALTER COLUMN regularization_status DROP DEFAULT;

CREATE INDEX idx_invoices_patient_regularization
    ON invoices(patient_id, regularization_status);
