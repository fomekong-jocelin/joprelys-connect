# TICKET-0701 — Enregistrement et API REST des logs d'audit (backend)

> Fichier obligatoire pour chaque ticket ou intervention IA.

## 1. Objectif

Implémenter la persistance des logs d'audit en base de données et l'API REST de consultation associée pour le backend Spring Boot. Cela permet de répondre aux exigences réglementaires de traçabilité des dossiers médicaux.

## 2. Critères d'acceptation

- [x] Création de la table `audit_logs` en base de données (H2 / PostgreSQL) via migration Flyway.
- [x] Création de l'entité `AuditLogEntity`, du repository et du service d'audit.
- [x] Journalisation automatique des événements clés suivants :
  - Connexion réussie et échecs de connexion (via Spring Security Event Listeners).
  - Création de patient et modification d'identité de patient (via interceptor/aspect ou service).
  - Téléchargement et révocation/annulation d'ordonnance ou de document médical (dans `DocumentService`).
- [x] Exposition de l'API REST de consultation :
  - `GET /api/audit/patients/{patientId}` (logs par patient, triés du plus récent au plus ancien).
  - `GET /api/audit/organizations/{organizationId}` (logs par organisation, triés du plus récent au plus ancien).
- [x] Contrôle d'autorisation d'accès par rôle (`AUDITEUR` a accès à tout, `ADMIN_CLINIQUE` à sa clinique, `MEDECIN` à sa clinique/ses patients).
- [x] Isolation multi-tenant stricte : Un admin de l'organisation A ne peut pas lire les logs de l'organisation B.
- [x] Immuabilité : Aucune route HTTP `DELETE`, `PUT` ou `PATCH` sur l'audit.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | EPIC-0007 — Traçabilité & Audit Logs |
| User story parent | STORY-0701 |
| Sprint cible | SPRINT-0003 |
| Priorité business | P1 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | Intermédiaire |
| Effort estimé senior | 0.7j |
| Effort estimé intermédiaire | 1.0j |
| Effort estimé junior | 1.8j |
| Responsable | Antigravity |
| Reviewer obligatoire | Lead Developer |
| Risque fonctionnel | Moyen |
| Risque technique | Moyen |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé

- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `PROJECT-MANAGER-SKILL.md` lu si nécessaire
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Tests existants analysés
- [x] Contrats API analysés
- [x] Impacts sécurité analysés
- [x] Impacts données analysés
- [x] Impacts backend analysés
- [x] Backend Maven uniquement vérifié
- [x] Backend `application.yml` / profils YAML vérifiés ; aucun nouveau `application.properties`

## 5. Hypothèses

- Pour le MVP, l'écriture des logs d'audit s'effectue de manière synchrone dans la même transaction que l'action.
- L'adresse IP de l'acteur et le User-Agent sont résolus depuis le contexte HTTP (`HttpServletRequest`) s'il est disponible.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Performance d'écriture en base | Moyen | Indexation fine sur `patient_id` et `actor_organization_id`. |
| Cross-tenant leaks | Fort | Sécurité stricte basée sur l'organisation de l'utilisateur connecté dans `AuditController`. |

## 7. Action plan

- [x] Écrire la migration de base de données Flyway `V10__create_audit_logs_table.sql`.
- [x] Créer `AuditLogEntity.java`, `AuditLogRepository.java`.
- [x] Créer `AuditService.java` et `AuditServiceImpl.java`.
- [x] Créer `AuditLogResponse.java` et `AuditController.java`.
- [x] Créer `AuditSecurityEventListener.java` pour journaliser les logins (Success / Failure).
- [x] Intégrer l'appel à `AuditService` dans `PatientService` (création/modification).
- [x] Intégrer l'appel à `AuditService` dans `DocumentService` (téléchargement/révocation/génération).
- [x] Ajouter des tests d'intégration dans `AuditControllerTest.java` validant la sécurité et l'isolation.
- [x] Exécuter `mvn clean verify` pour s'assurer que le build passe au vert.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée

- Migration Flyway V10 ajoutant la table `audit_logs` avec index de performance.
- Modèle de données JPA `AuditLogEntity` et repository d'audit.
- Service `AuditService` et son implémentation avec extraction automatique de l'IP et du User-Agent depuis le contexte HTTP.
- Endpoints REST `/api/audit/patients/{patientId}` et `/api/audit/organizations/{organizationId}` sécurisés avec isolation multi-tenant.
- Écouteurs d'événements Spring Security pour la traçabilité des authentifications.
- Intégration de la journalisation dans la création/consultation de patients et la gestion de documents (création, téléchargement, révocation, annulation).

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-02 | Antigravity | 0.05j | 10% | Spécifications | Aucun | Cadrage et specs |
| 2026-07-02 | Antigravity | 0.8j | 100% | Aucun | Aucun | Développement backend, intégrations et tests |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Backend
mvn clean verify
```

### Résultats

- [x] Tests unitaires OK
- [x] Tests intégration OK (dont `AuditControllerTest` au vert avec 7 tests d'intégration d'audit)
- [x] Build OK

## 11. Documentation

- [x] Documentation fonctionnelle initiale créée : `docs/features/audit/FUNCTIONAL-SPEC.md`
- [x] Documentation technique initiale créée : `docs/features/audit/TECHNICAL-DESIGN.md`
- [x] Changelog mis à jour : `docs/ai/CHANGELOG.md`
- [x] Suivi projet mis à jour : `docs/ai/PROJECT-TRACKING.md`

## 12. Reste à faire

- Aucun (la story backend est entièrement terminée).

## 13. Statut final

Statut : DONE

## 14. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | MINOR |
| Justification | Ajout d'un nouveau module d'audit avec API REST, rétrocompatible. |
| Breaking change | Non |
| Migration DB | Oui |
| Changement API | Oui (nouveaux endpoints d'audit) |
| Impact Angular | Non |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 15. Impact thème / i18n / branding

- [ ] Impact Angular UI analysé
- [ ] Impact Flutter UI analysé

## Documentation First

- [x] Documentation fonctionnelle initiale créée / mise à jour : `docs/features/audit/FUNCTIONAL-SPEC.md`
- [x] Documentation technique initiale créée / mise à jour : `docs/features/audit/TECHNICAL-DESIGN.md`
- [ ] `API-CONTRACT.md` créé / mis à jour si API impactée
- [ ] `DATA-MODEL.md` créé / mis à jour si base de données impactée
- [ ] `TEST-PLAN.md` créé / mis à jour selon les tests attendus
- [ ] `USER-GUIDE.md` créé / mis à jour si impact utilisateur final

## Design System / UI

- *Non applicable.*

## Vérification `.gitignore`

- [x] `.gitignore` présent à la racine
- [x] `.gitignore` adapté à la stack réelle du projet
- [x] `docs/standards/GITIGNORE-STANDARDS.md` respecté
- [x] Aucun secret, cache ou artefact de build versionné
