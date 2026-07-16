-- Migration V68 : Suppression des contraintes CHECK sur les statuts
-- Suite au choix Option B (revenir aux migrations V35/V37 d'origine,
-- puis supprimer proprement les contraintes via une migration dédiée).
-- Compatible H2 et PostgreSQL 16.

ALTER TABLE lab_results DROP CONSTRAINT IF EXISTS chk_lab_result_status;
ALTER TABLE medical_documents DROP CONSTRAINT IF EXISTS chk_medical_document_status;
