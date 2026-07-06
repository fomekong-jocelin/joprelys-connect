-- Flyway Migration V41: Enrich users table with profile details and graphical assets paths
ALTER TABLE users ADD COLUMN photo_path VARCHAR(255);
ALTER TABLE users ADD COLUMN signature_path VARCHAR(255);
ALTER TABLE users ADD COLUMN stamp_path VARCHAR(255);
ALTER TABLE users ADD COLUMN phone VARCHAR(50);
ALTER TABLE users ADD COLUMN specialty VARCHAR(150);
ALTER TABLE users ADD COLUMN registration_number VARCHAR(100);
ALTER TABLE users ADD COLUMN department VARCHAR(150);
ALTER TABLE users ADD COLUMN bio TEXT;
