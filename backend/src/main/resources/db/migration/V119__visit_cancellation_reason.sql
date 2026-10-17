-- ADR-20261017 D-08 : annulation ou abandon de visite motivé et attribué.
ALTER TABLE visits ADD COLUMN cancellation_reason VARCHAR(40);
ALTER TABLE visits ADD COLUMN cancellation_note VARCHAR(500);
ALTER TABLE visits ADD COLUMN cancelled_by UUID;
