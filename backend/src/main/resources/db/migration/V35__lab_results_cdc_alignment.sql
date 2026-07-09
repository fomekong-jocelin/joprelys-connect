-- Migration V35 : Alignement des résultats d'examens biologiques avec le CDC

CREATE SEQUENCE IF NOT EXISTS lab_result_number_seq START WITH 1 INCREMENT BY 1;

-- Ajout des nouveaux champs sur la table lab_results
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'VALIDATED';
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS validator_user_id UUID;
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS conclusion TEXT;
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS document_id UUID;
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 1;
ALTER TABLE lab_results ADD COLUMN IF NOT EXISTS parent_result_id UUID;

-- Rendre validator_name optionnel
ALTER TABLE lab_results ALTER COLUMN validator_name DROP NOT NULL;

-- Ajout des contraintes de clé étrangère
ALTER TABLE lab_results ADD CONSTRAINT fk_lab_results_validator_user FOREIGN KEY (validator_user_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE lab_results ADD CONSTRAINT fk_lab_results_document FOREIGN KEY (document_id) REFERENCES medical_documents(id) ON DELETE SET NULL;
ALTER TABLE lab_results ADD CONSTRAINT fk_lab_results_parent FOREIGN KEY (parent_result_id) REFERENCES lab_results(id) ON DELETE SET NULL;

-- Contrainte check pour le statut
-- ALTER TABLE lab_results ADD CONSTRAINT chk_lab_result_status CHECK (status IN ('DRAFT', 'VALIDATED', 'CANCELLED'));

-- Mettre à jour les anciennes lignes
UPDATE lab_results SET status = 'VALIDATED' WHERE status IS NULL;
