# SEC-20261107 — P1 : CSP, QR patient signé, gitleaks en CI

## 1. Contexte

Suite de `SEC-20261107-P0-PIN-CHIFFRE-QR-SERVEUR.md`. Mode **Engineering**. Estimation : S/M.
Branche `fix/security-p1-csp-qr-gitleaks`.

| # | Faille | Preuve |
|---|---|---|
| P1-5 | Aucune Content-Security-Policy (web ni API) | `SecurityConfig`, `angular.json` |
| P1-6 | QR d'accès patient non signé : contenu falsifiable (patient, validité) | `PatientAccessQrController` |
| P1-7 | Aucune détection de secrets avant commit ni en CI | `.github/workflows/ci.yml` |

## 2. Action plan

- [x] API : CSP stricte sur `/api/**` (`default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'`) + `Referrer-Policy: no-referrer` ; Swagger non affecté
- [x] Web : `security.autoCsp` en production (CSP par hachage des scripts inline, `object-src 'none'`, `base-uri 'self'`). Build de production vérifié, application chargée sans violation CSP
- [x] QR patient signé HMAC-SHA256 (`PatientAccessQrSigner`, champ `sig`), clé dédiée `JOPRELYS_PATIENT_QR_KEY`, démarrage refusé sans elle ; `isValid` refuse QR expiré, patient ou validité modifiés, autre clé
- [x] gitleaks : `.gitleaks.toml` (faux positifs justifiés), hook `.githooks/pre-commit`, job CI `Secrets (gitleaks)`
- [x] Tests : `PatientAccessQrSignerTest` (5), `SecurityHeadersTest` (2), `PatientAccessQrControllerTest`. Backend complet : 1 050 tests, 0 échec, 9 ignorés
- [ ] **Avant déploiement** : définir `JOPRELYS_PATIENT_QR_KEY` (≥ 32 caractères, coffre)
- [ ] Serveur web (nginx) : ajouter `frame-ancestors 'none'`, `Strict-Transport-Security` et `X-Content-Type-Options` sur le site (une balise `<meta>` ne peut pas porter `frame-ancestors`)
- [ ] Endpoint de vérification du QR côté professionnel (scan) : à créer avec la fonctionnalité de lecture, en réutilisant `PatientAccessQrSigner.isValid`
- [ ] Activer le hook local : `git config core.hooksPath .githooks`
- [ ] `backend/.env.prod` (non versionné) contient des clés OpenAI, Anthropic et Google : les garder hors du poste ou les faire tourner

## 3. SemVer

Correctif de sécurité, nouvelle variable obligatoire en production : **PATCH** (signaler `JOPRELYS_PATIENT_QR_KEY`).
