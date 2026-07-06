-- V38: STORY-1909 — Alignement Module 12 : Consentements patient et accès externe conformes CDC
-- Nouveaux champs CDC sur patient_consents :
--   consent_type   : PONCTUEL, TEMPORAIRE, ETABLISSEMENT, PROFESSIONNEL, LIMITE, URGENCE
--   requester_user_id / requester_organization_id : qui a déclenché le consentement
--   reason         : motif du consentement
--   requested_at   : date de demande
--   approved_at    : date d'approbation
--   expires_at     : durée d'expiration (FR-CONSENT-003)
-- Mise à jour de la contrainte d'unicité : autorise plusieurs consentements par (patient, org) si type différent

-- Suppression de la contrainte UNIQUE existante (patient_id, organization_id) car on peut avoir
-- plusieurs types de consentement pour la même paire.
ALTER TABLE patient_consents DROP CONSTRAINT IF EXISTS uq_patient_org;

-- Ajout des nouvelles colonnes CDC
ALTER TABLE patient_consents ADD COLUMN IF NOT EXISTS consent_type VARCHAR(30) DEFAULT 'ETABLISSEMENT';
ALTER TABLE patient_consents ADD COLUMN IF NOT EXISTS requester_user_id UUID;
ALTER TABLE patient_consents ADD COLUMN IF NOT EXISTS requester_organization_id UUID;
ALTER TABLE patient_consents ADD COLUMN IF NOT EXISTS reason TEXT;
ALTER TABLE patient_consents ADD COLUMN IF NOT EXISTS requested_at TIMESTAMP;
ALTER TABLE patient_consents ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE patient_consents ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

-- Mise à jour du statut : ACTIVE → APPROVED (migration des données existantes)
UPDATE patient_consents SET status = 'APPROVED' WHERE status = 'ACTIVE';
UPDATE patient_consents SET status = 'REVOKED'  WHERE status = 'REVOKED';

-- Nouvelle contrainte unique sur (patient_id, organization_id, consent_type)
CREATE UNIQUE INDEX IF NOT EXISTS uq_patient_org_consent_type
    ON patient_consents (patient_id, organization_id, consent_type);

-- Journal d'audit des consentements déjà géré par audit_logs (FR-CONSENT-005)
