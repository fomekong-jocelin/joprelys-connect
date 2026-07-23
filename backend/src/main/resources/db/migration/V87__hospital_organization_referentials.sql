-- HOS-ORG-001-A / #130
-- Sépare l'organisation hospitalière de la géographie et introduit des catalogues contrôlés.

CREATE TABLE hospital_service_catalog (
    code VARCHAR(64) PRIMARY KEY,
    name_fr VARCHAR(120) NOT NULL,
    name_en VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_hospital_service_catalog_code_not_blank CHECK (TRIM(code) <> ''),
    CONSTRAINT chk_hospital_service_catalog_names_not_blank CHECK (TRIM(name_fr) <> '' AND TRIM(name_en) <> '')
);

CREATE TABLE medical_specialty_catalog (
    code VARCHAR(64) PRIMARY KEY,
    name_fr VARCHAR(120) NOT NULL,
    name_en VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_medical_specialty_catalog_code_not_blank CHECK (TRIM(code) <> ''),
    CONSTRAINT chk_medical_specialty_catalog_names_not_blank CHECK (TRIM(name_fr) <> '' AND TRIM(name_en) <> '')
);

CREATE TABLE organizational_unit_type_catalog (
    code VARCHAR(32) PRIMARY KEY
);

INSERT INTO organizational_unit_type_catalog (code) VALUES
    ('POLE'),
    ('DEPARTMENT'),
    ('SERVICE'),
    ('CARE_UNIT');

CREATE TABLE organizational_units (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    parent_id UUID,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120),
    unit_type VARCHAR(32) NOT NULL,
    service_catalog_code VARCHAR(64),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_organizational_units_id_org UNIQUE (id, organization_id),
    CONSTRAINT uq_organizational_units_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_organizational_units_parent_tenant
        FOREIGN KEY (parent_id, organization_id)
        REFERENCES organizational_units(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_organizational_units_type
        FOREIGN KEY (unit_type)
        REFERENCES organizational_unit_type_catalog(code)
        ON DELETE RESTRICT,
    CONSTRAINT fk_organizational_units_service_catalog
        FOREIGN KEY (service_catalog_code)
        REFERENCES hospital_service_catalog(code)
        ON DELETE RESTRICT,
    CONSTRAINT chk_organizational_units_code_not_blank CHECK (TRIM(code) <> ''),
    CONSTRAINT chk_organizational_units_identity CHECK (
        (unit_type = 'SERVICE' AND service_catalog_code IS NOT NULL AND name IS NULL)
        OR
        (unit_type <> 'SERVICE' AND service_catalog_code IS NULL AND name IS NOT NULL AND TRIM(name) <> '')
    )
);

CREATE INDEX idx_organizational_units_org_parent
    ON organizational_units (organization_id, parent_id);
CREATE INDEX idx_organizational_units_org_type_active
    ON organizational_units (organization_id, unit_type, active);
CREATE INDEX idx_organizational_units_service_catalog
    ON organizational_units (service_catalog_code);

INSERT INTO hospital_service_catalog (code, name_fr, name_en, active) VALUES
    ('GENERAL_MEDICINE', 'Médecine générale', 'General medicine', TRUE),
    ('INTERNAL_MEDICINE', 'Médecine interne', 'Internal medicine', TRUE),
    ('MATERNITY_OBGYN', 'Maternité / gynécologie-obstétrique', 'Maternity / obstetrics and gynecology', TRUE),
    ('PEDIATRICS', 'Pédiatrie', 'Pediatrics', TRUE),
    ('EMERGENCY', 'Urgences', 'Emergency department', TRUE),
    ('GENERAL_SURGERY', 'Chirurgie générale', 'General surgery', TRUE),
    ('CARDIOLOGY', 'Cardiologie', 'Cardiology', TRUE),
    ('INTENSIVE_CARE', 'Réanimation / soins intensifs', 'Intensive care', TRUE),
    ('ANESTHESIA', 'Anesthésie', 'Anesthesia', TRUE),
    ('OPERATING_THEATRE', 'Bloc opératoire', 'Operating theatre', TRUE),
    ('LABORATORY', 'Laboratoire', 'Laboratory', TRUE),
    ('IMAGING', 'Imagerie', 'Imaging', TRUE),
    ('PHARMACY', 'Pharmacie', 'Pharmacy', TRUE),
    ('INPATIENT_GENERAL', 'Hospitalisation polyvalente', 'General inpatient care', TRUE);

INSERT INTO medical_specialty_catalog (code, name_fr, name_en, active) VALUES
    ('GENERAL_MEDICINE', 'Médecine générale', 'General medicine', TRUE),
    ('INTERNAL_MEDICINE', 'Médecine interne', 'Internal medicine', TRUE),
    ('PEDIATRICS', 'Pédiatrie', 'Pediatrics', TRUE),
    ('OBSTETRICS_GYNECOLOGY', 'Gynécologie-obstétrique', 'Obstetrics and gynecology', TRUE),
    ('CARDIOLOGY', 'Cardiologie', 'Cardiology', TRUE),
    ('GENERAL_SURGERY', 'Chirurgie générale', 'General surgery', TRUE),
    ('ANESTHESIA_CRITICAL_CARE', 'Anesthésie-réanimation', 'Anesthesia and critical care', TRUE),
    ('RADIOLOGY', 'Radiologie / imagerie', 'Radiology / imaging', TRUE),
    ('MEDICAL_BIOLOGY', 'Biologie médicale', 'Medical biology', TRUE),
    ('HOSPITAL_PHARMACY', 'Pharmacie hospitalière', 'Hospital pharmacy', TRUE);
