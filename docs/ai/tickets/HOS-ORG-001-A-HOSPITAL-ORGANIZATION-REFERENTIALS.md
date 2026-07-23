# HOS-ORG-001-A — Référentiels et unités organisationnelles hospitalières

## Métadonnées

- Issue GitHub : #130
- Epic : EPIC-0027 / HOS-ORG-001
- Priorité : P0 avant démonstration client du 25 juillet 2026
- Baseline de démarrage : `main@81b7d20c4cf44800e436c86b964a38bc62929305`
- Baseline réalignée avant merge : `main@acb60bfa61e8773064a66cc6cfc871d01c701d6a`
- Branche : `feat/130-hos-org-001-a`
- PR : #133
- Commit squash `main` : `72b5e139592b20a9ea14ae366d3fcbab99c46cf1`
- Flyway : V87
- Statut : **DONE — code fusionné ; consolidation documentaire finale en cours sur PR docs-only**
- Profil : senior full-stack / architecte
- Reviewers : Tech Lead, DBA, Product/Direction médicale

## Objectif

Remplacer la notion ambiguë de « service » portée par `Ward` et les nouveaux usages de champs texte libres par un référentiel organisationnel explicite, hiérarchique et tenant-scoped permettant de représenter :

`Pôle → Département → Service → Unité de soins`

Tous les niveaux sont facultatifs. Une petite clinique peut créer directement des services sous l'établissement ; un CHU peut activer toute la hiérarchie.

## Décisions livrées

- Organisation et géographie sont deux axes distincts.
- `Ward` n'est pas étendu pour représenter pôle/département/unité.
- Un `SERVICE` utilise un catalogue codifié ; son nom métier n'est pas saisi librement.
- Le libellé d'un `SERVICE` n'est pas figé en français dans `organizational_units` : `serviceCatalogCode` est la source et l'UI résout FR/EN.
- Les spécialités médicales disposent d'un catalogue codifié.
- Une unité organisationnelle est désactivée/réactivée plutôt que supprimée physiquement.
- L'autorisation de gestion est dédiée : `ORGANIZATION_STRUCTURE_MANAGE`.
- Aucune nouvelle UI ne crée de fallback vers `department`, `specialty` ou un nom de service libre.
- Aucun mapping automatique depuis `Ward.name`, `users.department` ou `users.specialty` n'a été réalisé.

## Action plan réalisé

- [x] Relire audit, ADR-0002, backlog et règles UI/architecture.
- [x] Créer issue #130 et découpage 3 × 3 SP.
- [x] Documenter fonctionnel, technique, data, API et tests avant code.
- [x] Ajouter migration V87 et modèle backend.
- [x] Ajouter permission `ORGANIZATION_STRUCTURE_MANAGE` et matrice RBAC.
- [x] Ajouter API catalogues + unités.
- [x] Ajouter tests backend/migration/sécurité.
- [x] Ajouter UI Angular mobile-first FR/EN light/dark.
- [x] Ajouter tests Angular et build production.
- [x] Réaligner la branche sur le `main` courant avant merge sans écraser #129.
- [x] Mettre à jour ADR et `PROJECT-TRACKING.md` dans #133.
- [x] Exécuter la CI réelle et corriger sans affaiblir les contraintes/tests.
- [x] Squash merge PR #133 dans `main`.
- [x] Consolider le backlog EPIC-0027 sur une branche docs-only post-merge.
- [x] Consolider le changelog actif en conservant le snapshot historique intégral.

## Critères d'acceptation

- [x] Petite clinique : création directe de services sans pôle/département obligatoire.
- [x] Hôpital complexe : hiérarchie Pôle → Département → Service → Unité.
- [x] Les services utilisent un `serviceCatalogCode` contrôlé.
- [x] Les spécialités utilisent un catalogue codifié.
- [x] Un service ne peut pas être placé sous un parent invalide.
- [x] Un enfant ne peut pas référencer un parent d'un autre tenant.
- [x] Les codes d'unités sont uniques par tenant et peuvent être réutilisés dans un autre tenant.
- [x] Une unité avec enfant actif ne peut pas être désactivée.
- [x] Une unité désactivée conserve son identité/historique et peut être réactivée selon les règles.
- [x] API couverte sur les comportements 200/201/400/403/404/409 pertinents.
- [x] UI mobile-first, FR/EN, light/dark, route et navigation permission-first.
- [x] Aucun nom métier de SERVICE saisi librement dans la nouvelle UI.
- [x] Aucun Angular Material, Tailwind CSS v4 et tokens existants.
- [x] Maven strict vert.
- [x] PostgreSQL 16 V87 vert.
- [x] Tests Angular et build production verts sur le dernier changement frontend fonctionnel.

## Preuves techniques

### Backend

- GitHub Actions CI #1066 : `Backend — Maven Build & Tests` **SUCCESS** ;
- aucun `-DskipTests` ni `-Dmaven.test.skip` ;
- `HospitalOrganizationControllerTest` ;
- `HospitalOrganizationRbacCatalogTest` ;
- `OrganizationalUnitTypeConverterTest` ;
- `HospitalOrganizationPostgresqlMigrationTest`.

### PostgreSQL / Flyway

V87 crée :

- `hospital_service_catalog` ;
- `medical_specialty_catalog` ;
- `organizational_unit_type_catalog` ;
- `organizational_units`.

Les tests couvrent notamment :

- FK parent/tenant composite ;
- même code autorisé dans deux tenants distincts ;
- type d'unité inconnu refusé ;
- SERVICE sans catalogue refusé ;
- SERVICE avec nom libre refusé.

### Frontend

La dernière CI ayant modifié le frontend fonctionnel est verte pour :

- tests Angular ;
- build production ;
- traduction FR/EN ;
- absence de champ nom libre pour SERVICE ;
- résolution du libellé catalogue selon la locale ;
- fallback d'erreur localisé.

Les commits postérieurs avant le squash #133 étaient documentaires/synchronisation `main` uniquement.

## Estimation et capacité

9 SP total, découpés en tâches de 3 SP :

1. data/migration/catalogues — 3 SP ;
2. services/API/RBAC — 3 SP ;
3. UI mobile-first/i18n/QA — 3 SP.

Estimation initiale senior : 3 à 4 jours. Le jalon de démonstration n'a entraîné aucune suppression de test ni contournement de sécurité.

## Risques résiduels / dépendances

- `Ward/Room/Bed` reste temporairement le modèle historique de géographie/hospitalisation jusqu'à HOS-LOC-001-A / #131 ;
- `users.department` et `users.specialty` ne sont pas encore retirés tant que HOS-STAFF-001-A / #132 n'a pas livré leurs remplacements structurés ;
- les catalogues initiaux sont un référentiel technique extensible, pas une nomenclature médicale universelle exhaustive ;
- la recette métier authentifiée reste à intégrer au jalon QA #127.

Ces éléments ne doivent pas être traités par fallback : #131 puis #132 portent explicitement leur suppression/remplacement.

## SemVer

HOS-ORG-001-A est un ajout parallèle **MINOR**.

Le retrait futur des contrats/champs legacy (`Ward/Room` comme modèle cible, `department`, `specialty`, etc.) sera un changement **MAJOR** lorsque les remplacements structurés seront fusionnés et validés.
