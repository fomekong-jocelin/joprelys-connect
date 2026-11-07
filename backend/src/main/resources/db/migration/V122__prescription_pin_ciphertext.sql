-- SEC P0-3 : le PIN d'ordonnance n'est plus stocké en clair.
-- Étape 1 : nouvelle colonne chiffrée (AES-256-GCM, chiffrement applicatif).
-- Les PIN existants de pin_code sont chiffrés puis effacés au démarrage par PrescriptionPinEncryptionBackfill
-- (le chiffrement exige la clé applicative, il ne peut pas se faire en SQL).
-- Étape 2 (migration ultérieure, après vérification en production) : DROP COLUMN pin_code.
ALTER TABLE prescriptions ADD COLUMN pin_ciphertext VARCHAR(128);
