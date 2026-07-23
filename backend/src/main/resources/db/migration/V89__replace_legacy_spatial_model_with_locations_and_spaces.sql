-- HOS-LOC-001-A / #131
-- Précondition forte : V88 a vérifié l'absence de données legacy incompatibles.
-- Cette migration remplace définitivement Ward/Room comme modèle cible.

CREATE TABLE facility_location_node_type_catalog (
    code VARCHAR(32) PRIMARY KEY,
    rank_value INT NOT NULL UNIQUE,
    CONSTRAINT chk_facility_location_type_code_not_blank CHECK (TRIM(code) <> '')
);

INSERT INTO facility_location_node_type_catalog (code, rank_value) VALUES
    ('SITE', 10),
    ('BUILDING', 20),
    ('FLOOR', 30),
    ('ZONE', 40);

CREATE TABLE facility_location_nodes (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    parent_id UUID,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    node_type VARCHAR(32) NOT NULL REFERENCES facility_location_node_type_catalog(code) ON DELETE RESTRICT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_facility_location_nodes_id_org UNIQUE (id, organization_id),
    CONSTRAINT uq_facility_location_nodes_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_facility_location_parent_tenant
        FOREIGN KEY (parent_id, organization_id)
        REFERENCES facility_location_nodes(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_facility_location_nodes_code_not_blank CHECK (TRIM(code) <> ''),
    CONSTRAINT chk_facility_location_nodes_name_not_blank CHECK (TRIM(name) <> '')
);

CREATE INDEX idx_facility_location_nodes_org_parent
    ON facility_location_nodes (organization_id, parent_id);
CREATE INDEX idx_facility_location_nodes_org_type_active
    ON facility_location_nodes (organization_id, node_type, active);

CREATE TABLE space_type_catalog (
    code VARCHAR(64) PRIMARY KEY,
    name_fr VARCHAR(120) NOT NULL,
    name_en VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_space_type_catalog_code_not_blank CHECK (TRIM(code) <> ''),
    CONSTRAINT chk_space_type_catalog_names_not_blank CHECK (TRIM(name_fr) <> '' AND TRIM(name_en) <> '')
);

INSERT INTO space_type_catalog (code, name_fr, name_en, active) VALUES
    ('CONSULTATION_ROOM', 'Salle de consultation', 'Consultation room', TRUE),
    ('TREATMENT_ROOM', 'Salle de soins / examen', 'Treatment / examination room', TRUE),
    ('EMERGENCY_BOX', 'Box d''urgence', 'Emergency bay', TRUE),
    ('WAITING_ROOM', 'Salle d''attente', 'Waiting room', TRUE),
    ('HOSPITAL_ROOM', 'Chambre d''hospitalisation', 'Hospital room', TRUE),
    ('OPERATING_ROOM', 'Salle opératoire', 'Operating room', TRUE),
    ('RECOVERY_ROOM', 'Salle de réveil / SSPI', 'Recovery room', TRUE),
    ('ICU_ROOM', 'Réanimation / soins intensifs', 'Intensive care room', TRUE),
    ('DELIVERY_ROOM', 'Salle d''accouchement', 'Delivery room', TRUE),
    ('NEONATAL_ROOM', 'Espace néonatal', 'Neonatal room', TRUE),
    ('LABORATORY_ROOM', 'Laboratoire', 'Laboratory room', TRUE),
    ('IMAGING_ROOM', 'Salle d''imagerie', 'Imaging room', TRUE),
    ('PHARMACY', 'Pharmacie', 'Pharmacy', TRUE),
    ('STORAGE', 'Stockage', 'Storage', TRUE),
    ('OFFICE', 'Bureau', 'Office', TRUE),
    ('MORGUE', 'Morgue', 'Morgue', TRUE),
    ('OTHER_CONTROLLED', 'Autre espace contrôlé', 'Other controlled space', TRUE);

CREATE TABLE inpatient_space_type_catalog (
    space_type_code VARCHAR(64) PRIMARY KEY
        REFERENCES space_type_catalog(code) ON DELETE RESTRICT
);

INSERT INTO inpatient_space_type_catalog (space_type_code) VALUES
    ('HOSPITAL_ROOM'),
    ('ICU_ROOM');

CREATE TABLE facility_spaces (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    location_node_id UUID,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    space_type_code VARCHAR(64) NOT NULL REFERENCES space_type_catalog(code) ON DELETE RESTRICT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_facility_spaces_id_org UNIQUE (id, organization_id),
    CONSTRAINT uq_facility_spaces_id_org_type UNIQUE (id, organization_id, space_type_code),
    CONSTRAINT uq_facility_spaces_org_code UNIQUE (organization_id, code),
    CONSTRAINT fk_facility_spaces_location_tenant
        FOREIGN KEY (location_node_id, organization_id)
        REFERENCES facility_location_nodes(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_facility_spaces_code_not_blank CHECK (TRIM(code) <> ''),
    CONSTRAINT chk_facility_spaces_name_not_blank CHECK (TRIM(name) <> '')
);

CREATE INDEX idx_facility_spaces_org_location
    ON facility_spaces (organization_id, location_node_id);
CREATE INDEX idx_facility_spaces_org_type_active
    ON facility_spaces (organization_id, space_type_code, active);

CREATE TABLE inpatient_space_profiles (
    space_id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    space_type_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_inpatient_space_profiles_space_org UNIQUE (space_id, organization_id),
    CONSTRAINT fk_inpatient_profile_space_identity
        FOREIGN KEY (space_id, organization_id, space_type_code)
        REFERENCES facility_spaces(id, organization_id, space_type_code)
        ON DELETE RESTRICT,
    CONSTRAINT fk_inpatient_profile_allowed_type
        FOREIGN KEY (space_type_code)
        REFERENCES inpatient_space_type_catalog(space_type_code)
        ON DELETE RESTRICT
);

CREATE TABLE organizational_unit_space_assignments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    organizational_unit_id UUID NOT NULL,
    space_id UUID NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_organizational_unit_space_assignments_id_org UNIQUE (id, organization_id),
    CONSTRAINT fk_unit_space_assignment_unit_tenant
        FOREIGN KEY (organizational_unit_id, organization_id)
        REFERENCES organizational_units(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_unit_space_assignment_space_tenant
        FOREIGN KEY (space_id, organization_id)
        REFERENCES facility_spaces(id, organization_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_unit_space_assignment_period
        CHECK (valid_to IS NULL OR valid_to > valid_from)
);

CREATE INDEX idx_unit_space_assignments_org_unit_period
    ON organizational_unit_space_assignments (organization_id, organizational_unit_id, valid_from, valid_to);
CREATE INDEX idx_unit_space_assignments_org_space_period
    ON organizational_unit_space_assignments (organization_id, space_id, valid_from, valid_to);

-- Beds: conserver toute la logique d'état/capacité existante, remplacer uniquement l'identité Room par Space.
DROP INDEX IF EXISTS uq_beds_org_room_number;

ALTER TABLE beds
    ADD COLUMN space_id UUID;

ALTER TABLE beds
    DROP COLUMN room_id;

ALTER TABLE beds
    ALTER COLUMN space_id SET NOT NULL;

ALTER TABLE beds
    ADD CONSTRAINT fk_beds_inpatient_space_tenant
        FOREIGN KEY (space_id, organization_id)
        REFERENCES inpatient_space_profiles(space_id, organization_id)
        ON DELETE RESTRICT;

ALTER TABLE beds
    ADD CONSTRAINT uq_beds_id_space_organization UNIQUE (id, space_id, organization_id);

CREATE UNIQUE INDEX uq_beds_org_space_number
    ON beds (organization_id, space_id, bed_number);

CREATE INDEX idx_beds_space_id
    ON beds (space_id);

-- Hospitalizations: UUID structurés = sources de vérité ; chaînes historiques = snapshots explicites.
ALTER TABLE hospitalizations
    RENAME COLUMN service_name TO service_name_snapshot;
ALTER TABLE hospitalizations
    RENAME COLUMN room_number TO space_name_snapshot;
ALTER TABLE hospitalizations
    RENAME COLUMN bed_number TO bed_number_snapshot;

ALTER TABLE hospitalizations
    ADD COLUMN current_service_unit_id UUID;
ALTER TABLE hospitalizations
    ADD COLUMN current_space_id UUID;
ALTER TABLE hospitalizations
    ADD COLUMN current_bed_id UUID;

ALTER TABLE hospitalizations
    ALTER COLUMN current_service_unit_id SET NOT NULL;
ALTER TABLE hospitalizations
    ALTER COLUMN current_space_id SET NOT NULL;
ALTER TABLE hospitalizations
    ALTER COLUMN current_bed_id SET NOT NULL;

ALTER TABLE hospitalizations
    ADD CONSTRAINT fk_hospitalizations_current_service_unit_tenant
        FOREIGN KEY (current_service_unit_id, organization_id)
        REFERENCES organizational_units(id, organization_id)
        ON DELETE RESTRICT;

ALTER TABLE hospitalizations
    ADD CONSTRAINT fk_hospitalizations_current_space_tenant
        FOREIGN KEY (current_space_id, organization_id)
        REFERENCES facility_spaces(id, organization_id)
        ON DELETE RESTRICT;

ALTER TABLE hospitalizations
    ADD CONSTRAINT fk_hospitalizations_current_bed_space_tenant
        FOREIGN KEY (current_bed_id, current_space_id, organization_id)
        REFERENCES beds(id, space_id, organization_id)
        ON DELETE RESTRICT;

CREATE INDEX idx_hospitalizations_current_service_unit
    ON hospitalizations (organization_id, current_service_unit_id);
CREATE INDEX idx_hospitalizations_current_space
    ON hospitalizations (organization_id, current_space_id);
CREATE INDEX idx_hospitalizations_current_bed
    ON hospitalizations (organization_id, current_bed_id);

-- Le modèle legacy n'est plus conservé sous forme d'alias.
DROP TABLE rooms;
DROP TABLE wards;
