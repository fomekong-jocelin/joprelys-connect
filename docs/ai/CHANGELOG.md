# Changelog

Tous les changements notables du projet doivent être documentés ici.

Le format suit l'esprit de Keep a Changelog et le versioning suit Semantic Versioning.

> Le détail historique complet antérieur à cette consolidation est conservé **sans modification** dans `docs/ai/CHANGELOG-HISTORY-THROUGH-20260723.md`. Le présent fichier reste le registre actif à maintenir à partir du 23 juillet 2026.

## [Unreleased]

### Added

- **HOS-ORG-001-A / #130 / PR #133 — organisation hospitalière structurée** : ajout d'un bounded context `hospitalorganization` séparé de `spatial`, avec hiérarchie facultative `POLE → DEPARTMENT → SERVICE → CARE_UNIT`. Une petite clinique peut créer directement un service sous l'établissement sans niveau factice.
- **Flyway V87 — référentiels organisationnels** : ajout de `hospital_service_catalog`, `medical_specialty_catalog`, `organizational_unit_type_catalog` et `organizational_units`. Les codes sont stables ; le code d'unité est unique par tenant ; le parent est protégé par FK composite tenant.
- **Catalogue services FR/EN** : 14 types initiaux contrôlés couvrant notamment médecine générale, médecine interne, maternité/gynécologie-obstétrique, pédiatrie, urgences, chirurgie générale, cardiologie, réanimation, anesthésie, bloc, laboratoire, imagerie, pharmacie et hospitalisation polyvalente.
- **Catalogue spécialités FR/EN** : 10 spécialités initiales contrôlées pour préparer HOS-STAFF-001-A sans nouvelle saisie libre.
- **Permission `ORGANIZATION_STRUCTURE_MANAGE`** : capacité dédiée à la gestion des pôles, départements, services et unités ; attribuée aux profils administratifs prévus, non accordée par défaut aux métiers cliniques.
- **API `/api/hospital-organization`** : lecture des catalogues, lecture des unités, création/modification et activation/désactivation ; aucun DELETE physique dans ce lot.
- **UI Angular organisation hospitalière** : route `/clinic/hospital-organization`, navigation permission-first, configuration mobile-first, Tailwind CSS v4, FR/EN, light/dark, sans Angular Material.
- **Documentation HOS-ORG** : ticket technique, Functional Spec, Technical Design, Data Model, API Contract, Test Plan et ADR-0002 alignés sur l'implémentation fusionnée.

### Changed

- **Séparation organisation / géographie / capacité** : `Ward` n'est plus la cible d'extension pour représenter pôle, département ou unité. Les prochains travaux passent par HOS-LOC-001-A (#131) puis HOS-STAFF-001-A (#132).
- **Internationalisation des services** : un `SERVICE` ne persiste pas un libellé français dans `organizational_units.name`. Il conserve `serviceCatalogCode` et le client résout le libellé FR/EN depuis le catalogue actif.
- **Isolation tenant HOS-ORG** : défense en profondeur via scope authentifié, `TenantContext`/`@TenantId`, repositories explicitement filtrés par `organizationId` et FK composite parent/tenant.
- **Cycle de vie organisationnel** : les unités historiquement utilisables sont désactivées/réactivées au lieu d'être supprimées physiquement. La désactivation d'un parent possédant un enfant actif est refusée.
- **EPIC-0027** : backlog rebaseliné sur les incréments réellement engagés : HOS-ORG-001-A 9 SP livré, HOS-LOC-001-A 9 SP prochain, HOS-STAFF-001-A 9 SP dépendant.
- **QA-DEMO-20260725 / #127** : les données de démonstration doivent utiliser les nouveaux référentiels structurés dès leur disponibilité ; aucun nouveau service, département ou spécialité ne doit être créé via un champ texte libre.

### Fixed

- **Portabilité H2/PostgreSQL de `unit_type`** : remplacement d'un CHECK littéral fragile par `organizational_unit_type_catalog` + FK et converter JPA enum ↔ VARCHAR fail-closed ; la contrainte reste forte au lieu d'être supprimée pour satisfaire les tests.
- **Test historique V86** : `LegacyHospitalizationPermissionPostgresqlMigrationTest` ne suppose plus que V86 restera éternellement la dernière migration. Il vérifie désormais le contrat réel : V86 appliquée, `HOSPITALIZATION_MANAGE` supprimée, aucun remapping automatique des permissions.
- **Erreurs UI HOS-ORG** : les erreurs sont traduites via le mécanisme i18n existant au lieu d'exposer un détail backend français dans une interface anglaise.

### Validation

- **Backend** : GitHub Actions CI #1066 — Maven strict `clean verify` vert sur le dernier changement backend fonctionnel, sans `skipTests`.
- **PostgreSQL 16** : tests de migration V87 couvrant catalogues, FK tenant, contraintes d'identité SERVICE, type inconnu et isolation cross-tenant.
- **Frontend** : dernière CI frontend fonctionnelle verte — tests Angular et build production ; les commits postérieurs avant merge #133 étaient uniquement documentaires/synchronisation de `main`.
- **PR #133** : squash merge dans `main` au commit `72b5e139592b20a9ea14ae366d3fcbab99c46cf1`.

### Security

- aucun mapping automatique depuis `Ward.name`, `users.department` ou `users.specialty` ;
- aucun fallback de rétrocompatibilité introduit pour les nouveaux flux ;
- aucun secret ajouté au repository ;
- aucune action PROD/RECETTE et aucun développement serveur pour HOS-ORG-001-A ;
- `HOSPITALIZATION_MANAGE` reste supprimée depuis HOS-RBAC-001-D / V86.

### Documentation maintenance

- Le snapshot historique du changelog au moment de cette consolidation est conservé dans `docs/ai/CHANGELOG-HISTORY-THROUGH-20260723.md` afin de ne supprimer aucun travail documentaire antérieur tout en gardant le registre actif lisible.
