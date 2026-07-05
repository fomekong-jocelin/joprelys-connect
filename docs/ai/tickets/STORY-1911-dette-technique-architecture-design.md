# STORY-1911 — Dette technique : Architecture SOLID, sécurité et design system

| Champ | Valeur |
|---|---|
| **ID** | STORY-1911 |
| **Epic** | EPIC-0014 |
| **Type** | User Story / Refactoring |
| **Titre** | Dette technique — Architecture SOLID, sécurité et design system |
| **Statut** | READY |
| **Priorité** | P1 |
| **Stack** | Full-stack |
| **Profil recommandé** | Senior Tech Lead |
| **Estimation Senior** | 2.0j |
| **Estimation Intermédiaire** | 3.0j |
| **Estimation Junior** | 5.0j |
| **Sprint cible** | SPRINT-0011 (buffer) |
| **Assigné** | À assigner |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte

L’audit a révélé plusieurs violations des standards projet qui doivent être corrigées pour garantir la maintenabilité, la sécurité et la cohérence du design. Ce ticket traite les problèmes transverses.

---

## 2. Critères d’acceptation

### Architecture backend

- [ ] Aucun controller n’appelle directement un repository (sauf délégué via service/facade).
- [ ] `PatientService` est découpé en services spécialisés (`PatientCreationService`, `PatientMergeService`, `PatientConsentService`, `PatientSummaryService`, etc.).
- [ ] Aucun service/controller n’expose d’entité JPA en réponse API.
- [ ] Les statuts métier sont des enums avec contrainte CHECK en DB.

### Sécurité / 12-Factor

- [ ] Secrets JWT et clés API externalisés (`${JWT_SECRET}`, `${LAB_API_KEY}`) ; plus de valeurs par défaut dans `application.yml`.
- [ ] Suppression des résidus Gradle (`backend/gradle/`).
- [ ] `backend/.gitignore` mis à jour selon `docs/standards/GITIGNORE-STANDARDS.md`.
- [ ] `flyway.validate-on-migrate: true` et `flyway.repair-on-migrate: false` en production (conserver repair en test/dev).

### Design system frontend

- [ ] Dictionnaire i18n externalisé dans `assets/i18n/fr.json` et `en.json`.
- [ ] `<title>` dynamique.
- [ ] Arrondis uniformisés ≤ 8px.
- [ ] Couleurs hardcodées remplacées par les tokens CSS.
- [ ] Logo centralisé via `<app-logo>`.

---

## 3. Tâches techniques

1. Refactorer les controllers pour supprimer les injections de repositories directs.
2. Découper `PatientService`.
3. Remplacer les statuts String par des enums + migrations CHECK.
4. Externaliser les secrets.
5. Supprimer les résidus Gradle.
6. Externaliser les traductions Angular.
7. Uniformiser le design system.
8. Tests.

---

## 4. Fichiers impactés

- `backend/src/main/java/com/joprelys/backend/patient/api/PatientController.java`
- `backend/src/main/java/com/joprelys/backend/patient/api/PatientMedicalInfoController.java`
- `backend/src/main/java/com/joprelys/backend/patient/api/PatientPortalController.java`
- `backend/src/main/java/com/joprelys/backend/consultation/api/ConsultationController.java`
- `backend/src/main/java/com/joprelys/backend/prescription/api/PrescriptionController.java`
- `backend/src/main/java/com/joprelys/backend/lab/api/LabOrderController.java`
- `backend/src/main/java/com/joprelys/backend/patient/application/PatientService.java`
- `backend/src/main/resources/application.yml`
- `backend/pom.xml`
- `backend/.gitignore`
- `web/src/app/core/i18n/i18n.service.ts`
- `web/src/app/index.html`
- `web/src/app/styles.css`
- `web/src/app/shared/layout/app-shell.component.ts`
- `web/src/assets/i18n/fr.json` (nouveau)
- `web/src/assets/i18n/en.json` (nouveau)

---

## 5. Tests attendus

- [ ] Backend : `./mvnw test` et `./mvnw clean verify` passants.
- [ ] Frontend : `npm run lint`, `npm run test`, `npm run build` passants.
- [ ] Vérification qu’aucun controller n’injecte de repository directement.

---

## 6. Dépendances

- Aucune (peut être traitée en parallèle, mais ne doit pas bloquer les US fonctionnelles).

---

## 7. Risques

- Risque de régression lors du refactor de `PatientService`.
- Changement de configuration Flyway à tester sur environnement de staging.

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0) pour le refactor / amélioration.
