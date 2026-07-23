-- HOS-LOC-001-A / #131
-- Le niveau de confort appartenait historiquement à Room et reste requis par la pré-facturation.

ALTER TABLE inpatient_space_profiles
    ADD COLUMN comfort_level VARCHAR(50) NOT NULL DEFAULT 'STANDARD';

ALTER TABLE inpatient_space_profiles
    ADD CONSTRAINT chk_inpatient_space_profiles_comfort_not_blank
        CHECK (TRIM(comfort_level) <> '');
