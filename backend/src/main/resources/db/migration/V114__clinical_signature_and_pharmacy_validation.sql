-- Scellement serveur : aucune revendication d'horodatage qualifié externe.
ALTER TABLE consultations ADD COLUMN signed_by UUID REFERENCES users(id);
ALTER TABLE consultations ADD COLUMN signed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE consultations ADD COLUMN signed_content_hash VARCHAR(64);
ALTER TABLE prescriptions ADD COLUMN signed_by UUID REFERENCES users(id);
ALTER TABLE prescriptions ADD COLUMN signed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE prescriptions ADD COLUMN signed_content_hash VARCHAR(64);
ALTER TABLE prescriptions ADD COLUMN pharmacy_validated_by UUID REFERENCES users(id);
ALTER TABLE prescriptions ADD COLUMN pharmacy_validated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE prescriptions ADD COLUMN pharmacy_validation_notes VARCHAR(2000);
