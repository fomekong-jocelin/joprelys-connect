-- TASK-20260717 : unicité de la structure hospitalière par clinique.
CREATE UNIQUE INDEX IF NOT EXISTS uq_wards_org_name
    ON wards (organization_id, name);

CREATE UNIQUE INDEX IF NOT EXISTS uq_rooms_org_ward_number
    ON rooms (organization_id, ward_id, room_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_beds_org_room_number
    ON beds (organization_id, room_id, bed_number);
