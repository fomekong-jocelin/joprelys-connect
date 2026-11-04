-- SEC-01 / FA-39 : protection du PIN public d'ordonnance contre la force brute.
-- PIN porté à 6 chiffres pour les nouvelles ordonnances (les PIN existants à 4 chiffres restent valides),
-- compteur de tentatives et verrouillage persistés en base (atomiques, conservés au redémarrage).
ALTER TABLE prescriptions ALTER COLUMN pin_code SET DATA TYPE VARCHAR(8);
ALTER TABLE prescriptions ADD COLUMN pin_failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE prescriptions ADD COLUMN pin_locked_until TIMESTAMP;
