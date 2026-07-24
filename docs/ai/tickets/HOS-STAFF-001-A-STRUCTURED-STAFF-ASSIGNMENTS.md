# HOS-STAFF-001-A — Spécialités structurées et affectations datées du personnel

## Métadonnées

- Issue GitHub : #132
- Epic : EPIC-0027 / HOS-STAFF-001
- PR : #140
- Dépendances : HOS-ORG-001-A / #130 **DONE**, HOS-LOC-001-A / #131 **DONE**
- Baseline de départ : `main@e495477ea02beb05596b20b656bc092bd8fbbd83`
- Branche : `feat/132-hos-staff-001-a`
- Statut : READY TECHNIQUE — gate #1262 vert ; consolidation documentaire réalisée ; gate final post-doc restant
- Priorité : P0 avant répétition finale #127
- Estimation : 9 SP
- Profil : senior full-stack sécurité / données RH clinique
- Reviewers : Tech Lead + RH + cadre hospitalier + RSSI/DPO + Product

## Avancement au 24/07/2026

### État Git / PR

- #140 ouverte sur `main` ;
- branche synchronisée avec `main` au dernier contrôle (`behind_by = 0`) ;
- aucun thread de review ouvert au dernier contrôle ;
- PR temporairement remise en Draft pendant la consolidation documentaire afin d'éviter les runs CI annulés par des commits successifs ;
- `PROJECT-TRACKING.md`, `CHANGELOG.md` et backlog EPIC-0027 sont alignés sur l'état HOS-STAFF avant le gate final.

### CI et stabilisation

- CI #1247 sur `20984329c79be59e957fa94b179bdcfee27fae06` : **backend rouge** ; Maven a exécuté **564 tests**, avec **0 failure et 8 errors** ;
- cause #1247 : `StaffAssignmentControllerTest.setUp()` supprimait globalement les organisations alors que des `visits` d'autres classes les referenciaient encore ;
- `74a267e8ac5c922bd62dca749ef7f482c0d88afa` : suppression des purges globales et création de fixtures uniques par scénario ;
- CI #1251 sur `936d15e90cbb75e52349078ca57560b3f88b09f5` : **frontend vert complet** (tests Angular + build production), backend rouge ;
- cause backend #1251 : création des `OrganizationalUnitEntity` du fixture hors `TenantContext` ;
- `72a550410f78362359cbe9ab6be4ad6ecbaf5ba3` : création des unités A/B sous leur tenant explicite ;
- CI #1252 sur `72a550410f78362359cbe9ab6be4ad6ecbaf5ba3` : Maven exécute **564 tests**, puis 95 erreurs en cascade apparaissent après HOS-STAFF ;
- cause #1252 : les fixtures HOS-STAFF uniques n'étaient pas détruites en fin de test ; leurs lignes `staff_organizational_unit_assignments` empêchaient ensuite les nettoyages de `users` / `organizational_units` des tests FHIR, HOS-ORG et Spatial ;
- `72279a427f2c1bd7598eda003fe45de19505a7f3` : `@AfterEach` ciblé supprimant uniquement les données des deux tenants HOS-STAFF, dans l'ordre FK : affectations → unités → utilisateurs → organisations ;
- **CI #1262 sur `6c0b0123926aca04cdbe9f98b2206c70f9a15506` : Maven strict SUCCESS, tests Angular SUCCESS, build Angular production SUCCESS** ;
- gate final post-doc à exécuter sur le head contenant cette fiche + tracking + changelog + backlog avant squash merge.

## Objectif

Remplacer les champs libres `department` et `specialty` du personnel par des références structurées et historisées, afin que les droits, l'organisation clinique, les rendez-vous, les admissions et les futurs plannings puissent raisonner sur des identités stables plutôt que sur des chaînes de caractères.

## Principes non négociables

1. `StaffMember` / utilisateur reste une identité de personne ; une unité organisationnelle n'est jamais encodée dans son libellé.
2. Un membre du personnel peut être affecté à plusieurs unités, avec dates de validité et rôle d'affectation.
3. Une spécialité médicale vient exclusivement de `medical_specialty_catalog` créé par HOS-ORG.
4. Les affectations organisationnelles ciblent `organizational_units` créé par HOS-ORG.
5. Aucun mapping automatique par ressemblance de `users.department` ou `users.specialty`.
6. Toute donnée legacy ambiguë doit provoquer un diagnostic explicite ; aucun fallback silencieux.
7. Les API/UI de création et modification du personnel ne doivent plus avoir besoin de texte libre `department/specialty`.
8. Tenant isolation obligatoire côté application et DB.
9. Historisation : pas de réécriture destructive des affectations passées.
10. Aucun déploiement PROD/RECETTE dans ce lot.

## Modèle cible

```text
User / Staff Member
├── StaffSpecialtyAssignment
│   ├── specialtyCode → medical_specialty_catalog.code
│   ├── isPrimary
│   ├── validFrom
│   └── validTo?
│
└── StaffOrganizationalUnitAssignment
    ├── organizationalUnitId → organizational_units.id
    ├── assignmentRoleCode
    ├── isPrimary
    ├── validFrom
    └── validTo?
```

### Spécialités

- plusieurs spécialités possibles si le métier l'exige ;
- au plus une spécialité principale active par membre ;
- code contrôlé, libellé FR/EN résolu depuis le catalogue ;
- aucune spécialité libre persistée dans le nouveau flux.

### Affectations organisationnelles

- plusieurs unités possibles ;
- au plus une affectation principale active par membre ;
- une unité désactivée ne peut pas recevoir une nouvelle affectation active ;
- les périodes historiques restent consultables ;
- les affectations cross-tenant sont interdites par l'application et la base ;
- le rôle d'affectation est contextuel et distinct du rôle RBAC global.

## Découpage

### Task A1 — data / migration / invariants — 3 SP

- [x] analyser les champs legacy `users.department` / `users.specialty` et leurs consommateurs ;
- [x] créer `staff_specialty_assignments` ;
- [x] créer `staff_organizational_unit_assignments` ;
- [x] ajouter contraintes tenant et FK vers les référentiels HOS-ORG ;
- [x] empêcher plusieurs affectations principales actives du même type ;
- [x] définir le preflight des données legacy ambiguës via V92 ;
- [x] aucun mapping automatique par nom ;
- [x] supprimer physiquement les colonnes legacy via V95 après preflight.

Migrations : V92 preflight fail-fast, V93 modèle structuré, V94 contraintes PostgreSQL de périodes, V95 suppression `users.department/users.specialty`.

### Task A2 — backend / contrats / sécurité — 3 SP

- [x] repositories + services métier ;
- [x] lecture des spécialités et affectations actives/historiques ;
- [x] création/modification/clôture des affectations ;
- [x] intégrer les références structurées dans le flux staff ;
- [x] supprimer le besoin des champs libres `department/specialty` dans les contrats d'écriture ;
- [x] retirer `department/specialty` de `UserAccountEntity`, `StaffResponse`, `UpdateStaffRequest` et du profil ;
- [x] protéger les mutations par `USER_MANAGE` et les lectures par `USER_READ/USER_MANAGE` ;
- [x] tenant isolation et contrôles cross-tenant ;
- [x] annuaire patient migré vers `specialtyCode` + `organizationalUnitId` ;
- [x] tests backend HOS-STAFF et PostgreSQL ajoutés ;
- [x] Maven strict global vert sur #1262 ;
- [ ] Maven strict global vert sur le head final post-doc.

### Task A3 — Angular / migration consommateurs / QA — 3 SP

- [x] modèles et services Angular structurés ;
- [x] écran staff : sélecteurs de spécialités et unités depuis référentiels actifs ;
- [x] affichage des affectations datées et de l'affectation principale ;
- [x] supprimer les champs texte libres des formulaires staff actifs ;
- [x] profil personnel : suppression des saisies libres department/specialty ;
- [x] portail rendez-vous : annuaire et filtres migrés vers codes/UUID structurés ;
- [x] `StaffApiService` expose `activeOrganizationalUnits[]` avec UUID, code, libellés FR/EN et indicateur principal ;
- [x] la projection `StaffMember.department` résiduelle est explicitement un **snapshot de présentation dérivé** de l'unité principale active : elle n'est ni persistée dans `users`, ni acceptée dans un payload d'écriture ;
- [x] le module Visite historique conserve `visits.service_name` comme snapshot texte ; son cutover complet vers `organizationalUnitId` est distinct de HOS-STAFF et ne doit pas réintroduire de colonne libre dans Staff ;
- [x] documentation historique `clinic-staff-department-filter/TECHNICAL-DESIGN.md` marquée **SUPERSEDED** ;
- [ ] revue humaine finale FR/EN, light/dark, responsive dans #127 ;
- [x] tests Angular + build production verts sur #1262 ;
- [ ] tests Angular + build production verts sur le head final post-doc.

## Critères d'acceptation

- [x] aucun nouveau staff ne peut enregistrer un `department` libre ;
- [x] aucune spécialité libre n'est enregistrée dans le nouveau flux ;
- [x] seules les spécialités actives du catalogue sont sélectionnables ;
- [x] seules les unités organisationnelles actives du tenant sont assignables ;
- [x] un staff peut avoir plusieurs affectations datées ;
- [x] une affectation principale active est identifiable sans ambiguïté ;
- [x] une spécialité principale active est identifiable sans ambiguïté ;
- [x] les historiques ne sont pas détruits lors d'un changement d'unité/spécialité ;
- [x] les affectations cross-tenant sont refusées ;
- [x] aucun mapping automatique des anciennes chaînes par similarité ;
- [x] aucun consommateur staff/rendez-vous ne dépend d'une colonne `users.department` / `users.specialty` ;
- [x] gate #1262 : Maven strict + PostgreSQL/Testcontainers + tests Angular + build production verts sur le même head fonctionnel ;
- [x] documentation centrale, tracking et changelog alignés avant gate final ;
- [x] aucune action PROD/RECETTE.

## Décisions prises pendant l'implémentation

- le rôle d'affectation organisationnelle possède un catalogue clinique distinct (`staff_assignment_role_catalog`) et n'est pas déduit automatiquement du rôle RBAC global ;
- les spécialités sont modélisées de façon générique pour permettre plusieurs catégories de professionnels sans changer le schéma ;
- l'annuaire patient ne filtre plus les médecins par chaînes `department/specialty`, mais par affectations actives structurées ;
- le preflight V92 bloque toute donnée legacy libre non vide : une reprise de données éventuelle devra faire l'objet d'une décision/script explicitement revu, jamais d'un fuzzy mapping ;
- le module Visite n'est pas refondu dans #132 : `visits.service_name` reste un snapshot métier existant ; `StaffApiService` fournit néanmoins les UUID d'unités actives pour son futur cutover ;
- la projection `StaffMember.department` ne constitue pas une compatibilité base/API d'écriture : elle est calculée en mémoire depuis l'affectation structurée principale et peut être supprimée sans migration de données lors du cutover Visite.

## Definition of Done

- [x] modèle + migrations cohérents ;
- [x] contrats backend structurés ;
- [x] Angular sans saisie libre department/specialty ;
- [ ] scan final zéro dépendance active aux **colonnes** legacy `users.department/users.specialty` ;
- [x] tenant isolation application + DB ;
- [x] gate technique #1262 vert ;
- [ ] gate technique final post-doc vert sur le head exact à merger ;
- [x] branche synchronisée avec le `main` courant au dernier contrôle ;
- [x] documentation centrale/tracking/changelog/backlog alignés ;
- [ ] PR squash-mergée ;
- [ ] recette humaine #127 rejouée.
