-- Script de migration V54 : Ajout des colonnes de résolution d'écart par le DAF sur les sessions de caisse
ALTER TABLE cash_register_sessions ADD COLUMN discrepancy_resolved BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE cash_register_sessions ADD COLUMN resolution_notes VARCHAR(1000);
ALTER TABLE cash_register_sessions ADD COLUMN resolved_by_user_id VARCHAR(36);
ALTER TABLE cash_register_sessions ADD COLUMN resolved_at TIMESTAMP WITH TIME ZONE;
