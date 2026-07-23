ALTER TABLE users
    ADD CONSTRAINT uq_users_id_organization_staff UNIQUE (id, organization_id);

CREATE TABLE staff_assignment_role_catalog (
    code VARCHAR(64) PRIMARY KEY,
    name_fr VARCHAR(120) NOT NULL,
    name_en VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO staff_assignment_role_catalog (code, name_fr, name_en, active) VALUES
    ('PRACTITIONER', 'Praticien', 'Practitioner', TRUE),
    ('NURSE', 'Infirmier / infirmière', 'Nurse', TRUE),
    ('MIDWIFE', 'Sage-femme', 'Midwife', TRUE),
    ('CARE_ASSISTANT', 'Aide-soignant', 'Care assistant', TRUE),
    ('PHARMACIST', 'Pharmacien', 'Pharmacist', TRUE),
    ('LAB_TECHNICIAN', 'Technicien de laboratoire', 'Laboratory technician', TRUE),
    ('IMAGING_TECHNICIAN', 'Technicien d''imagerie', 'Imaging technician', TRUE),
    ('UNIT_MANAGER', 'Responsable d''unité', 'Unit manager', TRUE),
    ('ADMINISTRATIVE_SUPPORT', 'Support administratif', 'Administrative support', TRUE),
    ('OTHER_CONTROLLED', 'Autre rôle contrôlé', 'Other controlled role', TRUE);

CREATE TABLE staff_specialty_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    specialty_code VARCHAR(64) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_staff_specialty_assignment_period
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT fk_staff_specialty_assignment_staff_tenant
        FOREIGN KEY (staff_id, organization_id)
        REFERENCES users (id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_staff_specialty_assignment_specialty
        FOREIGN KEY (specialty_code)
        REFERENCES medical_specialty_catalog (code)
        ON DELETE RESTRICT,
    CONSTRAINT uq_staff_specialty_assignment_identity
        UNIQUE (organization_id, staff_id, specialty_code, valid_from)
);

CREATE INDEX idx_staff_specialty_assignment_staff
    ON staff_specialty_assignments (organization_id, staff_id, valid_from);
CREATE INDEX idx_staff_specialty_assignment_specialty
    ON staff_specialty_assignments (specialty_code, valid_from);

CREATE TABLE staff_organizational_unit_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    organizational_unit_id UUID NOT NULL,
    assignment_role_code VARCHAR(64) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_staff_unit_assignment_period
        CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT fk_staff_unit_assignment_staff_tenant
        FOREIGN KEY (staff_id, organization_id)
        REFERENCES users (id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_staff_unit_assignment_unit_tenant
        FOREIGN KEY (organizational_unit_id, organization_id)
        REFERENCES organizational_units (id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_staff_unit_assignment_role
        FOREIGN KEY (assignment_role_code)
        REFERENCES staff_assignment_role_catalog (code)
        ON DELETE RESTRICT,
    CONSTRAINT uq_staff_unit_assignment_identity
        UNIQUE (organization_id, staff_id, organizational_unit_id, valid_from)
);

CREATE INDEX idx_staff_unit_assignment_staff
    ON staff_organizational_unit_assignments (organization_id, staff_id, valid_from);
CREATE INDEX idx_staff_unit_assignment_unit
    ON staff_organizational_unit_assignments (organization_id, organizational_unit_id, valid_from);
