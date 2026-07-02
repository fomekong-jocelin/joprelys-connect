-- STORY-0603 : Revocation et annulation de documents medicaux
-- Ajout des colonnes de tracabilite de revocation sur medical_documents
-- Note : instructions separees pour compatibilite H2 (tests) et PostgreSQL (prod)

ALTER TABLE medical_documents ADD COLUMN revoked_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE medical_documents ADD COLUMN revoked_by_user_id UUID;
ALTER TABLE medical_documents ADD COLUMN revocation_reason VARCHAR(500);
