-- V23: Granular scopes and validation channel columns (TICKET-1306/1307)

ALTER TABLE patient_consents ADD COLUMN IF NOT EXISTS scopes VARCHAR(500) DEFAULT 'medical_records,prescriptions,lab_results,allergies_history';
ALTER TABLE patient_consents ADD COLUMN IF NOT EXISTS validation_channel VARCHAR(50) DEFAULT 'PORTAL';

ALTER TABLE external_access_requests ADD COLUMN IF NOT EXISTS scopes VARCHAR(500) DEFAULT 'medical_records,prescriptions,lab_results,allergies_history';

ALTER TABLE hospitalizations ALTER COLUMN pdf_file_path TYPE VARCHAR(500);
