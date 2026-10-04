-- Audit administrateur 2026-10-04 : noms locaux et référentiels cliniques.
ALTER TABLE organizational_units DROP CONSTRAINT chk_organizational_units_identity;
ALTER TABLE organizational_units ADD CONSTRAINT chk_organizational_units_identity CHECK (
    (unit_type = 'SERVICE' AND service_catalog_code IS NOT NULL
        AND (name IS NULL OR LENGTH(TRIM(name)) BETWEEN 2 AND 120))
    OR (unit_type <> 'SERVICE' AND service_catalog_code IS NULL
        AND name IS NOT NULL AND TRIM(name) <> '')
);
INSERT INTO hospital_service_catalog (code, name_fr, name_en, active) VALUES
    ('ORTHOPEDICS_TRAUMATOLOGY', 'Orthopédie / traumatologie', 'Orthopedics / traumatology', TRUE),
    ('NEONATOLOGY', 'Néonatologie', 'Neonatology', TRUE),
    ('INFECTIOUS_DISEASES', 'Infectiologie', 'Infectious diseases', TRUE),
    ('GASTROENTEROLOGY', 'Gastro-entérologie / endoscopie', 'Gastroenterology / endoscopy', TRUE),
    ('OPHTHALMOLOGY', 'Ophtalmologie', 'Ophthalmology', TRUE),
    ('ENT', 'ORL', 'Ear, nose and throat', TRUE),
    ('STOMATOLOGY', 'Stomatologie', 'Stomatology', TRUE),
    ('NEPHROLOGY', 'Néphrologie', 'Nephrology', TRUE),
    ('HEMODIALYSIS', 'Hémodialyse', 'Hemodialysis', TRUE);
INSERT INTO medical_specialty_catalog (code, name_fr, name_en, active) VALUES
    ('EMERGENCY_MEDICINE', 'Médecine d’urgence', 'Emergency medicine', TRUE),
    ('ORTHOPEDICS_TRAUMATOLOGY', 'Orthopédie / traumatologie', 'Orthopedics / traumatology', TRUE),
    ('NEONATOLOGY', 'Néonatologie', 'Neonatology', TRUE),
    ('INFECTIOUS_DISEASES', 'Infectiologie', 'Infectious diseases', TRUE),
    ('GASTROENTEROLOGY', 'Gastro-entérologie / endoscopie', 'Gastroenterology / endoscopy', TRUE),
    ('OPHTHALMOLOGY', 'Ophtalmologie', 'Ophthalmology', TRUE),
    ('ENT', 'ORL', 'Ear, nose and throat', TRUE),
    ('STOMATOLOGY', 'Stomatologie', 'Stomatology', TRUE),
    ('NEPHROLOGY', 'Néphrologie', 'Nephrology', TRUE),
    ('HEMODIALYSIS', 'Hémodialyse', 'Hemodialysis', TRUE);
INSERT INTO space_type_catalog (code, name_fr, name_en, active) VALUES
    ('DAY_HOSPITAL', 'Hôpital de jour', 'Day hospital', TRUE),
    ('DIALYSIS_STATION', 'Poste d’hémodialyse', 'Hemodialysis station', TRUE),
    ('CHEMOTHERAPY_STATION', 'Poste de chimiothérapie', 'Chemotherapy station', TRUE),
    ('AMBULATORY_SURGERY', 'Chirurgie ambulatoire', 'Ambulatory surgery', TRUE);
INSERT INTO inpatient_space_type_catalog (space_type_code) VALUES
    ('NEONATAL_ROOM'), ('EMERGENCY_BOX'), ('DAY_HOSPITAL'),
    ('DIALYSIS_STATION'), ('CHEMOTHERAPY_STATION'), ('AMBULATORY_SURGERY');
INSERT INTO staff_assignment_role_catalog (code, name_fr, name_en, active) VALUES
    ('MEDICAL_HEAD', 'Praticien responsable / chef de service', 'Medical head / head of department', TRUE),
    ('NURSE_MANAGER', 'Cadre de santé / infirmier major', 'Nurse manager / head nurse', TRUE);
