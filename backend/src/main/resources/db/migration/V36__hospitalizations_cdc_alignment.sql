-- Migration V36 : Alignement des hospitalisations avec le CDC

CREATE SEQUENCE IF NOT EXISTS hospitalization_number_seq START WITH 1 INCREMENT BY 1;

-- Ajout des colonnes sur la table hospitalizations
ALTER TABLE hospitalizations ADD COLUMN IF NOT EXISTS hospitalization_number VARCHAR(50);
ALTER TABLE hospitalizations ADD COLUMN IF NOT EXISTS visit_id UUID;
ALTER TABLE hospitalizations ADD COLUMN IF NOT EXISTS responsible_practitioner_id UUID;
ALTER TABLE hospitalizations ADD COLUMN IF NOT EXISTS document_id UUID;

-- Mettre à jour les anciennes lignes avec un numéro temporaire unique
UPDATE hospitalizations SET hospitalization_number = 'HOSP-OLD-' || SUBSTRING(CAST(id AS VARCHAR), 1, 8) WHERE hospitalization_number IS NULL;

-- Rendre hospitalization_number NOT NULL
ALTER TABLE hospitalizations ALTER COLUMN hospitalization_number SET NOT NULL;

-- Ajout des contraintes de clé étrangère
ALTER TABLE hospitalizations ADD CONSTRAINT fk_hosp_visit FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE SET NULL;
ALTER TABLE hospitalizations ADD CONSTRAINT fk_hosp_practitioner FOREIGN KEY (responsible_practitioner_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE hospitalizations ADD CONSTRAINT fk_hosp_document FOREIGN KEY (document_id) REFERENCES medical_documents(id) ON DELETE SET NULL;

-- Ajout de la colonne hash sur la table medical_documents pour conformité documents vérifiables
ALTER TABLE medical_documents ADD COLUMN IF NOT EXISTS hash VARCHAR(64);
