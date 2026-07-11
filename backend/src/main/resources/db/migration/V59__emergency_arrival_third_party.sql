ALTER TABLE emergencies ADD COLUMN third_party_name VARCHAR(160);
ALTER TABLE emergencies ADD COLUMN third_party_phone VARCHAR(40);
ALTER TABLE emergencies ADD COLUMN third_party_relationship VARCHAR(80);
ALTER TABLE emergencies ADD COLUMN third_party_id_document VARCHAR(120);
ALTER TABLE emergencies ADD COLUMN third_party_circumstances VARCHAR(1000);
ALTER TABLE emergencies ADD COLUMN third_party_consent_to_contact BOOLEAN NOT NULL DEFAULT FALSE;
