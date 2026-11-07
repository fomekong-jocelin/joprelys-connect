# SEC-20261107 — P0 : PIN d'ordonnance chiffré au repos, QR patient généré par le serveur

## 1. Contexte

Audit de l'écosystème Joprelys · Dictalys · AllôPharma (05-07/11/2026) : failles critiques à traiter avant tout pont
réseau. Mode **Engineering**. Estimation : S/M. Branche `fix/security-p0-pin-qr`.

| # | Faille | Preuve |
|---|---|---|
| P0-3 | PIN d'ordonnance stocké en clair (`prescriptions.pin_code`) | `PrescriptionEntity` |
| P0-4 | QR d'accès patient généré par `api.qrserver.com` : l'identifiant patient part chez un tiers ; validité fixée par le navigateur | `patient-qr-code-page.component.ts` |

## 2. Action plan

- [x] `PrescriptionPinCipher` (AES-256-GCM, clé `JOPRELYS_PRESCRIPTION_PIN_KEY`, refus de démarrer sans clé)
- [x] `EncryptedPinConverter` sur `PrescriptionEntity.pinCode` → colonne `pin_ciphertext` (migration V122)
- [x] `PrescriptionPinEncryptionBackfill` : chiffre puis efface les PIN en clair existants au démarrage (idempotent)
- [x] `GET /api/patient/access-qr` (PNG, `no-store`, validité serveur 15 min, ZXing déjà présent)
- [x] Page QR : image du serveur, squelette de chargement, erreur avec « Réessayer », i18n fr/en
- [x] Tests : `PrescriptionPinCipherTest`, `PatientAccessQrControllerTest`, cas ajoutés à `PharmacyControllerTest`,
  specs web du portail. Backend complet : 1 043 tests, 0 échec ; `tsc` sans erreur
- [ ] Définir `JOPRELYS_PRESCRIPTION_PIN_KEY` en production avant déploiement (la conserver dans un coffre)
- [ ] Migration ultérieure : `DROP COLUMN pin_code` après vérification en production
- [ ] Signer le QR patient ; ajouter une CSP ; gitleaks dans la CI

## 3. SemVer

Correctif de sécurité, nouvelle variable d'environnement obligatoire en production : **PATCH** (signaler la variable dans les notes de version).
