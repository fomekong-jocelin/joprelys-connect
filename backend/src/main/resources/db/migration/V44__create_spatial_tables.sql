-- STORY-1912 : Gestion Spatiale (Lits & Chambres)
-- Création des tables wards, rooms, beds, bed_assignments

CREATE TABLE IF NOT EXISTS wards (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_wards_organization_id ON wards (organization_id);

CREATE TABLE IF NOT EXISTS rooms (
    id UUID PRIMARY KEY,
    ward_id UUID NOT NULL REFERENCES wards(id) ON DELETE CASCADE,
    room_number VARCHAR(20) NOT NULL,
    capacity INT NOT NULL,
    comfort_level VARCHAR(50) NOT NULL, -- VIP, STANDARD
    organization_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rooms_ward_id ON rooms (ward_id);
CREATE INDEX IF NOT EXISTS idx_rooms_organization_id ON rooms (organization_id);

CREATE TABLE IF NOT EXISTS beds (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    bed_number VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'FREE', -- FREE, OCCUPIED, CLEANING, MAINTENANCE
    organization_id UUID NOT NULL,
    version INTEGER NOT NULL DEFAULT 0, -- Pour le verrouillage optimiste JPA
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_beds_room_id ON beds (room_id);
CREATE INDEX IF NOT EXISTS idx_beds_organization_id ON beds (organization_id);

CREATE TABLE IF NOT EXISTS bed_assignments (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL, -- Lié à l'hospitalisation active
    bed_id UUID NOT NULL REFERENCES beds(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    organization_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bed_assignments_hospitalization_id ON bed_assignments (hospitalization_id);
CREATE INDEX IF NOT EXISTS idx_bed_assignments_bed_id ON bed_assignments (bed_id);
CREATE INDEX IF NOT EXISTS idx_bed_assignments_organization_id ON bed_assignments (organization_id);
