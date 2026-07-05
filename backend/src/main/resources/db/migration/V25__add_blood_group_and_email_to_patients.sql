-- V25: Add blood group and email to patients table (Module 3 / DPU compliance)

ALTER TABLE patients ADD COLUMN blood_group VARCHAR(10);
ALTER TABLE patients ADD COLUMN email VARCHAR(255);
