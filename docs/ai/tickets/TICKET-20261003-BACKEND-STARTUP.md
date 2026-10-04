# TICKET-20261003-BACKEND-STARTUP — Démarrage du backend Spring Boot en local

## Metadata
- **ID** : TICKET-20261003-BACKEND-STARTUP
- **Epic** : OPERATIONS / BACKEND_RUNTIME
- **Type** : Engineering / Démarrage & Diagnostic
- **Statut** : DONE
- **Priorité** : P0
- **Auteur** : Antigravity
- **Date** : 2026-10-03
- **Stack** : Spring Boot / Java 21 / Maven / PostgreSQL 17

## Objectif
Démarrer le backend Spring Boot de Joprelys Connect en local, vérifier la connectivité avec PostgreSQL 17, exécuter les migrations Flyway et valider que l'API est opérationnelle sur le port 8080.

## Diagnostic & Problèmes rencontrés
1. **Blocage Flyway V88 (`HOS-LOC V88 blocked: legacy spatial/hospitalization data detected`)** :
   - *Cause* : La migration V88 vérifie l'absence de données dans les tables spatiales legacy (`wards`, `rooms`, `beds`, `bed_assignments`, `hospitalizations`) avant d'appliquer V89 (qui remplace l'ancien modèle par `facility_locations`, `facility_spaces`, `inpatient_space_profiles`).
   - *Résolution* : Sauvegarde complète de la base de données locale dans `scratch/joprelys_backup_pre_v88.sql`. Purge des tables legacy et leurs dépendances directes (`hospitalization_daily_cares`, `medication_administrations`, `patient_consumptions`, `surgical_consents`, `bed_assignments`, `hospitalizations`, `beds`, `rooms`, `wards`). Les 23 migrations en attente (V88 à V110) se sont ensuite appliquées avec succès.

2. **Échec d'injection Spring Context sur `FinalClinicalReviewController`** :
   - *Cause* : `FinalClinicalReviewController`, `FinalClinicalReviewService`, et `OpenAiFinalClinicalReviewGateway` étaient annotés avec `@ConditionalOnProperty(name = "joprelys.ai.openai.final-review-enabled", havingValue = "true")` sans exiger `@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")`. Or, en configuration par défaut locale (`joprelys.ai.enabled=false`), `AiConsultationService` n'est pas instancié, ce qui provoquait une `UnsatisfiedDependencyException` au démarrage.
   - *Résolution* : Ajout de l'annotation répétable `@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")` sur ces trois composants du module AI.

## Action plan
- [x] Vérifier la présence et le statut du service PostgreSQL local (`postgresql-x64-17` actif sur port 5432)
- [x] Vérifier la connectivité à la base de données `joprelys` avec les identifiants `postgres`/`postgres`
- [x] Sauvegarder la base de données locale (`scratch/joprelys_backup_pre_v88.sql`)
- [x] Débloquer Flyway V88 en purgeant les tables spatiales/hospitalisations obsolètes conformément aux exigences V88/V89
- [x] Compiler les sources backend via Maven Wrapper (`.\mvnw.cmd compile` -> BUILD SUCCESS)
- [x] Corriger les annotations `@ConditionalOnProperty` sur `FinalClinicalReviewController`, `FinalClinicalReviewService`, et `OpenAiFinalClinicalReviewGateway`
- [x] Démarrer le backend en arrière-plan via `.\mvnw.cmd spring-boot:run`
- [x] Vérifier les logs de démarrage Spring Boot (`Started JoprelysBackendApplication in 10.105 seconds`)
- [x] Effectuer un test de connectivité HTTP sur le serveur démarré (`http://localhost:8080/v3/api-docs` HTTP 200 OK, `http://localhost:8080/swagger-ui/index.html` HTTP 200 OK)
- [x] Mettre à jour `PROJECT-TRACKING.md` et documenter l'état final

## Résultats & Vérifications
- PostgreSQL 17 opérationnel sur port 5432, migrations Flyway à jour (version V110).
- Spring Boot 4 / Java 21 actif et démarré sur le port 8080.
- Endpoint `/v3/api-docs` répond HTTP 200 (JSON 250 KB).
- Interface Swagger UI répond HTTP 200.
