-- Migration V37 : Alignement des documents médicaux avec le CDC

CREATE SEQUENCE IF NOT EXISTS medical_document_number_seq START WITH 1 INCREMENT BY 1;

-- Ajout des nouveaux champs sur la table medical_documents
ALTER TABLE medical_documents ADD COLUMN IF NOT EXISTS qr_code_url VARCHAR(500);
ALTER TABLE medical_documents ADD COLUMN IF NOT EXISTS verification_url VARCHAR(500);
ALTER TABLE medical_documents ADD COLUMN IF NOT EXISTS author_user_id UUID;
ALTER TABLE medical_documents ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 1;
ALTER TABLE medical_documents ADD COLUMN IF NOT EXISTS previous_document_id UUID;

-- Ajout des contraintes de clé étrangère
ALTER TABLE medical_documents ADD CONSTRAINT fk_medical_docs_author FOREIGN KEY (author_user_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE medical_documents ADD CONSTRAINT fk_medical_docs_previous FOREIGN KEY (previous_document_id) REFERENCES medical_documents(id) ON DELETE SET NULL;

-- Mettre à jour la valeur legacy 'SYNTHESE' vers 'COMPTE_RENDU_CONSULTATION'
UPDATE medical_documents SET document_type = 'COMPTE_RENDU_CONSULTATION' WHERE document_type = 'SYNTHESE';

-- Mettre à jour les documents existants avec des valeurs par défaut pour les nouveaux champs
UPDATE medical_documents SET qr_code_url = '/api/public/documents/' || CAST(id AS VARCHAR) || '/qr' WHERE qr_code_url IS NULL;
UPDATE medical_documents SET verification_url = 'http://localhost:4200/verify/' || CAST(id AS VARCHAR) WHERE verification_url IS NULL;

-- Contrainte check pour le statut du document (VALID, REVOQUE, ANNULE, REMPLACE)
ALTER TABLE medical_documents DROP CONSTRAINT IF EXISTS chk_medical_document_status;
-- ALTER TABLE medical_documents ADD CONSTRAINT chk_medical_document_status CHECK (status IN ('VALID', 'REVOQUE', 'ANNULE', 'REMPLACE'));
