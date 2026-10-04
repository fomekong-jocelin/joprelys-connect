-- Lien entre un pré-enregistrement validé et le dossier patient créé ou réconcilié.
-- Permet de retrouver la fiche d'admission et d'ouvrir la visite après rechargement.
ALTER TABLE patient_pre_registrations ADD COLUMN validated_patient_id UUID;
