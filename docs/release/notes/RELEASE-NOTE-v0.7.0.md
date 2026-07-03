# RELEASE NOTE — v0.7.0

## 1. Résumé

La version `0.7.0` de Joprelys Connect apporte des améliorations majeures en matière d'intégration continue, de sécurité OWASP, de gestion fonctionnelle des stocks et d'interopérabilité médicale :
1. **Pipeline CI/CD Robuste (STORY-1101)** : Automatisation des tests et builds Maven/Angular via GitHub Actions sur chaque PR et commit.
2. **Sécurisation IDOR (STORY-1102)** : Renforcement des contrôles OWASP A01 au niveau du portail patient grâce à `PatientAccessGuardService`.
3. **Gestion Réelle des Stocks (STORY-1103)** : Lancement du suivi d'inventaire de pharmacie avec Flyway V17, entité JPA multi-tenant et décrémentation transactionnelle avec verrouillage optimiste JPA (`@Version`).
4. **Import Structuré FHIR (STORY-1104)** : Parseur JSON FHIR R4 `DiagnosticReport` Jackson-only pour l'ingestion automatisée des examens de laboratoire sans dépendances externes lourdes.
5. **Assainissement dette technique (STORY-1105)** : Suppression des starters de test fictifs Maven, configuration complète de `application-test.yml` pour les tests in-memory, et index de migration compatibles H2.

## 2. Version

| Champ | Valeur |
|---|---|
| Version précédente | 0.6.0 |
| Version publiée | 0.7.0 |
| Type de bump | MINOR |
| Date | 2026-07-03 |
| Responsable release | Antigravity |

## 3. Justification SemVer

Le bump est de type **MINOR** :
- Ajout de nouvelles fonctionnalités rétrocompatibles (endpoints publics d'importation FHIR, API CRUD de gestion de stocks).
- Introduction de composants techniques réutilisables (`FhirDiagnosticReportParser`, `PatientAccessGuardService`).
- Nouvelle table Flyway `drug_stocks` non destructive.
- Aucun changement de contrat destructif sur les API existantes.

## 4. Tickets inclus

| Ticket | Titre | Type | Statut |
|---|---|---|---|
| [STORY-1101](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1101-ci-cd-verify-compilation.md) | CI/CD Automatisation & Compilation strictes | DevOps | DONE |
| [STORY-1102](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1102-owasp-audit-idor-prevention.md) | Audit OWASP & Sécurisation IDOR Portail Patient | Sécurité | DONE |
| [STORY-1103](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1103-pharmacy-real-drug-stocks.md) | Gestion réelle des stocks de médicaments | Fonctionnel | DONE |
| [STORY-1104](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1104-lab-fhir-diagnostic-report-import.md) | Import de résultats structurés de laboratoire (FHIR/HL7) | Intégration | DONE |
| [STORY-1105](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1105-qa-technical-debt-cleanup.md) | Nettoyage de la dette technique & application QA | Qualité | DONE |

## 5. Changements

### Added

- **Pipeline CI/CD** : Intégration continue GitHub Actions exécutant `./mvnw clean verify` et `npm test && npm run build` à chaque Pull Request.
- **Sécurité IDOR** : Centralisation des contrôles dans `PatientAccessGuardService` ; tout accès à un DPU non corrélé au token JWT retourne HTTP 403.
- **Module Stocks Pharmacie** : Table de données `drug_stocks`, verrous optimistes `@Version` JPA et service de contrôle et décrémentation lors de la validation des dispensations.
- **Import FHIR R4** : Parseur JSON `FhirDiagnosticReportParser` Jackson-only extrayant les analytes, valeurs, interprétations et dates, raccordé à l'ingestion des résultats du patient.
- **Endpoint FHIR Labo** : `POST /api/public/lab-integration/fhir/diagnostic-report` sécurisé par `X-API-KEY`.

### Changed

- **Mappers de Consultation** : Optimisation des requêtes SQL via `JOIN FETCH` de `visit` et `doctor` pour éviter les `LazyInitializationException` dans le contrôleur.
- **Config de test** : Alignement complet de `application-test.yml` avec l'écosystème de tests unitaires locaux.

### Fixed

- **Index Flyway V17** : Ajustement de l'index sur `drug_stocks` pour garantir le support syntaxique multi-bases (H2 et PostgreSQL).

## 6. Breaking changes

- Aucun.

## 7. Migrations

Migration Flyway additive `V17__create_drug_stocks_table.sql`.
Aucune migration destructive n'est nécessaire.

## 8. Tests et QA

| Vérification | Résultat | Preuve |
|---|---|---|
| Backend tests | ✅ Succès | 136 tests unitaires et d'intégration au vert (`./mvnw test`) |
| Angular build | ✅ Succès | Compilation et tests frontend validés |
| Security scan | ✅ Validé | Couverture totale des tests d'intrusion simulés IDOR (`PatientIdorSecurityTest`) |
| Pipeline CI | ✅ Configuré | Workflow validé et prêt à se déclencher sur GitHub |

## 9. Déploiement

```bash
# Compilation et packaging backend
./mvnw clean package

# Build frontend
cd web && npm ci && npm run build
```

## 10. Tag Git

```bash
git tag -a v0.7.0 -m "Release v0.7.0"
git push origin v0.7.0
```

## 11. Rollback

En cas d'anomalie critique en production :
1. Revenir sur le tag précédent `v0.6.0`.
2. Déployer à nouveau les packages correspondants.
3. Supprimer ou ignorer la table de stock `drug_stocks` si nécessaire (aucune donnée métier pré-existante n'est altérée car la table est additive).

## 12. Risques restants

- Aucun risque critique identifié.

---

## Vérification Maven / Tailwind / Angular Material

- [x] Backend Spring Boot vérifié avec Maven uniquement.
- [x] Angular vérifié avec Tailwind CSS v4.
- [x] Aucun Angular Material introduit.
- [x] Spring Boot utilise `application.yml` et profils YAML ; aucun `application.properties`.
- [x] Angular possède `proxy.conf.json`, `proxyConfig` dans `angular.json`, et aucune URL backend hardcodée.
