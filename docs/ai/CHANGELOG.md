# Changelog

Tous les changements notables du projet doivent être documentés ici.

Le format suit l'esprit de Keep a Changelog et le versioning suit Semantic Versioning.

> Le détail historique complet antérieur à cette consolidation est conservé **sans modification** dans `docs/ai/CHANGELOG-HISTORY-THROUGH-20260723.md`. Le présent fichier reste le registre actif à maintenir à partir du 23 juillet 2026.

## [Unreleased]

- **BUG-20260724-ADMISSION-FREE-TEXT-SERVICE-ORIENTATION-FIX — Sélecteurs contrôlés pour l'admission et la visite** : remplacement des champs à saisie libre `<input>` d'Orientation et Service dans le formulaire d'admission patient (`app-unified-admission`) par des éléments `<select>` contrôlés. L'orientation propose les orientations cliniques normées (Consultation, Spécialisée, Urgences, Hospitalisation, Ambulatoire, De Jour, Bilan, Autre) et le service s'alimente dynamiquement via `HospitalOrganizationApiService.listServiceCatalog()`.
- **Persistance et restauration anti-perte de brouillon d'admission (`joprelys_admission_draft`)** : sauvegarde automatique en temps réel de tous les champs saisis de l'admission dans `localStorage`. En cas d'interruption, de rafraîchissement ou de reconnexion suite à une fin de session, le brouillon est restauré à 100% à l'ouverture du formulaire avec une bannière d'information et option d'effacement. Exemption explicite de purge pour les clés de brouillon dans `AuthTokenStorageService`.
- **Maintien de session actif en arrière-plan (`AuthSessionKeepAliveService`)** : rafraîchissement proactif du jeton JWT à l'approche de l'expiration (sous 15 minutes) pendant que l'utilisateur travaille sur un formulaire, éliminant tout risque de déconnexion inopinée pendant la saisie.

- **HOS-ORG-001-A / #130 / PR #133 — organisation hospitalière structurée** : ajout d'un bounded context `hospitalorganization` séparé de `spatial`, avec hiérarchie facultative `POLE → DEPARTMENT → SERVICE → CARE_UNIT`. Une petite clinique peut créer directement un service sous l'établissement sans niveau factice.
- **Flyway V87 — référentiels organisationnels** : ajout de `hospital_service_catalog`, `medical_specialty_catalog`, `organizational_unit_type_catalog` et `organizational_units`. Les codes sont stables ; le code d'unité est unique par tenant ; le parent est protégé par FK composite tenant.
- **Catalogue services FR/EN** : 14 types initiaux contrôlés couvrant notamment médecine générale, médecine interne, maternité/gynécologie-obstétrique, pédiatrie, urgences, chirurgie générale, cardiologie, réanimation, anesthésie, bloc, laboratoire, imagerie, pharmacie et hospitalisation polyvalente.
- **Catalogue spécialités FR/EN** : 10 spécialités initiales contrôlées pour préparer HOS-STAFF-001-A sans nouvelle saisie libre.
- **Permission `ORGANIZATION_STRUCTURE_MANAGE`** : capacité dédiée à la gestion des pôles, départements, services et unités ; attribuée aux profils administratifs prévus, non accordée par défaut aux métiers cliniques.
- **API `/api/hospital-organization`** : lecture des catalogues, lecture des unités, création/modification et activation/désactivation ; aucun DELETE physique dans ce lot.
- **UI Angular organisation hospitalière** : route `/clinic/hospital-organization`, navigation permission-first, configuration mobile-first, Tailwind CSS v4, FR/EN, light/dark, sans Angular Material.
- **Documentation HOS-ORG** : ticket technique, Functional Spec, Technical Design, Data Model, API Contract, Test Plan et ADR-0002 alignés sur l'implémentation fusionnée.
- **HOS-LOC-001-A / #131 / PR #137 — géographie et espaces hospitaliers structurés** : ajout de la hiérarchie géographique facultative `SITE → BUILDING → FLOOR → ZONE`, d'espaces physiques génériques `SPACE`, d'un catalogue contrôlé des types d'espace et de rattachements datés `OrganizationalUnit ↔ Space`.
- **Flyway V88 — preflight breaking cleanup** : blocage fail-fast avant toute mutation destructive si des données legacy `wards/rooms/beds/bed_assignments/hospitalizations` nécessitent encore une décision de migration explicite ; aucun mapping automatique par nom n'est autorisé.
- **Flyway V89 — nouveau modèle spatial** : création des tables de géographie, espaces, catalogues, profils d'hébergement et affectations unité-espace ; passage des lits de `room_id` à `space_id`, références structurées des hospitalisations et suppression finale de `wards/rooms` sur une base compatible.
- **Flyway V90 — non-chevauchement des affectations unité-espace** : contrainte PostgreSQL empêchant deux périodes actives/chevauchantes pour un même couple unité-espace ; no-op explicite sur H2.
- **Flyway V91 — confort d'hébergement** : ajout de `comfort_level` dans `InpatientSpaceProfile` afin de préserver la tarification STANDARD/VIP sans dépendre d'une ancienne `Room`.
- **API spatiale HOS-LOC** : configuration des localisations, espaces, profils d'hébergement, lits et rattachements datés ; lecture de capacité par espace et par unité organisationnelle.
- **UI Angular HOS-LOC** : configuration mobile-first des localisations/espaces/lits, affichage séparé des rattachements service ↔ espace, capacité par espace et parcours d'admission `unité → espace → lit`, y compris continuité Urgence → Hospitalisation.
- **HOS-STAFF-001-A / #132 / PR #140 — affectations structurées du personnel** : ajout des relations historisées `Staff ↔ OrganizationalUnit` et `Staff ↔ MedicalSpecialty`, avec périodes de validité, affectation principale/secondaire, spécialité principale et rôle d'affectation contextuel distinct du RBAC global.
- **Flyway V92–V95 — cutover staff structuré** : V92 preflight fail-fast sur les anciennes chaînes `users.department/users.specialty`, V93 tables/catalogue d'affectations, V94 contraintes PostgreSQL de non-chevauchement et d'unicité temporelle des principales, V95 suppression physique des deux colonnes legacy.
- **API staff structurée** : endpoints `/api/staff/{id}/assignments` pour lire, créer, modifier et clôturer les affectations de spécialité/unité, ainsi que `/api/staff/assignment-roles` pour le catalogue contextuel contrôlé.
- **UI Angular HOS-STAFF** : éditeur mobile-first des spécialités et unités datées, historique conservé, indicateurs principal/actif, sélecteurs alimentés par HOS-ORG et aucun Angular Material.
- **Projection structurée du personnel** : `StaffApiService` enrichit la liste avec `activeOrganizationalUnits[]` (`id`, `code`, libellés FR/EN, `primary`) pour permettre aux consommateurs de raisonner sur des UUID stables.
- **Endpoint de consultation spatiale soignants (`GET /api/spatial/inpatient-spaces`)** : exposition de la liste des espaces d'hébergement physiques actifs autorisée aux rôles soignants via la permission existante `HOSPITALIZATION_READ`, permettant aux médecins et infirmiers de consulter la capacité et l'occupation des lits (`/clinic/spatial`) sans leur donner accès aux routes d'administration d'infrastructure (`/api/spatial/configuration/*`).

### Changed

- **Séparation organisation / géographie / capacité** : `SERVICE` / `CARE_UNIT` représente l'organisation médicale ; `SPACE` représente le lieu physique. Un service peut utiliser plusieurs espaces et un espace peut être partagé par plusieurs unités via des affectations datées.
- **Lits** : `BedEntity` référence désormais `FacilitySpace` via `spaceId`; l'ancien `roomId` n'est plus une identité métier active.
- **Hospitalisation** : le séjour porte `currentServiceUnitId`, `currentSpaceId` et `currentBedId`; les noms du service, de l'espace et du lit sont conservés comme snapshots lisibles pour l'historique et les documents.
- **Admission et transfert** : les écritures utilisent exclusivement les UUID structurés `serviceUnitId / spaceId / bedId`; aucune résolution par libellé n'est utilisée.
- **Facturation d'hébergement** : le niveau de confort est lu depuis `InpatientSpaceProfile` ; la logique STANDARD/VIP est conservée sans lookup d'une `Room` legacy.
- **Internationalisation des services** : un `SERVICE` ne persiste pas un libellé français dans `organizational_units.name`. Il conserve `serviceCatalogCode` et le client résout le libellé FR/EN depuis le catalogue actif.
- **Isolation tenant HOS-ORG/HOS-LOC** : défense en profondeur via scope authentifié, `TenantContext`/`@TenantId`, repositories filtrés par `organizationId` et contraintes/FK composites tenant.
- **Cycle de vie organisationnel et spatial** : les unités/localisations/espaces historiquement utilisés sont activés/désactivés selon leur contrat plutôt que détournés en suppression métier ambiguë.
- **Gestion du personnel** : `UserAccount` reste une identité de personne ; l'unité, la spécialité et le rôle contextuel sont désormais des relations structurées et datées, jamais des attributs texte de l'utilisateur.
- **Contrats staff** : `UpdateStaffRequest`, `StaffResponse`, le profil et les formulaires staff ne lisent/écrivent plus `department` ou `specialty` libres.
- **Annuaire et prise de rendez-vous patient** : les médecins sont exposés et filtrés par spécialités contrôlées et unités organisationnelles UUID, avec prise en compte des affectations actives.
- **Module Visite historique** : `visits.service_name` reste un snapshot texte propre au module Visite. La projection Angular `StaffMember.department` éventuellement consommée par ce formulaire est calculée en mémoire depuis l'unité principale active, n'est pas persistée dans `users` et n'est jamais acceptée en écriture staff.
- **EPIC-0027** : HOS-ORG-001-A 9 SP fusionné ; HOS-LOC-001-A 13 SP fusionné ; HOS-STAFF-001-A 9 SP implémenté et validé par gate combiné #1262 avant consolidation documentaire.
- **QA-DEMO-20260725 / #127** : la répétition doit désormais utiliser unités, espaces, lits, spécialités et affectations staff structurées ; la recette humaine responsive/FR-EN/light-dark reste à rejouer.
- **CI backend** : le job Maven conserve `backend-verify.log` avec les rapports Surefire en artefact lors d'un échec, ce qui rend les diagnostics de `clean verify` directement exploitables.

### Removed

- **Modèle spatial applicatif Ward/Room** : suppression de `WardEntity`, `RoomEntity`, `WardRepository`, `RoomRepository`, `HospitalServiceType`, des DTO/services/use cases associés et des anciennes routes de configuration Ward/Room.
- **Contrats d'écriture texte pour l'hospitalisation** : suppression du besoin de `serviceName`, `roomNumber` et `bedNumber` comme clés d'admission/transfert.
- **Fiche HOS-LOC dupliquée** : suppression de l'ancienne documentation pré-audit 9 SP afin de conserver une seule source de vérité canonique à 13 SP.
- **Colonnes staff libres** : suppression physique de `users.department` et `users.specialty` via V95 après preflight V92 ; aucune compatibilité persistante ou fallback par similarité de nom n'est conservé.
- **Saisie libre staff** : retrait des champs département/spécialité des écrans d'administration du personnel et du profil.

### Fixed

- **Éditeur d'affectations collaborateur (`StaffAssignmentEditorComponent`)** : refonte ergonomique (affichage prioritaire des cartes d'affectations existantes, ouverture à la demande du formulaire via bouton d'action), correction des espacements (suppression du bouton directement collé aux cartes) et complétude i18n FR/EN pour l'ensemble des clés `staff.assignments.*`.
- **Portabilité H2/PostgreSQL de `unit_type`** : remplacement d'un CHECK littéral fragile par `organizational_unit_type_catalog` + FK et converter JPA enum ↔ VARCHAR fail-closed ; la contrainte reste forte au lieu d'être supprimée pour satisfaire les tests.
- **Test historique V86** : `LegacyHospitalizationPermissionPostgresqlMigrationTest` ne suppose plus que V86 restera éternellement la dernière migration. Il vérifie désormais le contrat réel : V86 appliquée, `HOSPITALIZATION_MANAGE` supprimée, aucun remapping automatique des permissions.
- **Erreurs UI HOS-ORG** : les erreurs sont traduites via le mécanisme i18n existant au lieu d'exposer un détail backend français dans une interface anglaise.
- **Tests PostgreSQL historiques** : les tests de capacité, historique d'état et intégrité des affectations utilisent désormais `FacilitySpace`; le scénario historique de chevauchement V80 reste volontairement borné à V80 au lieu de franchir V88 avec des données Ward/Room incompatibles.
- **Isolation des tests H2** : le test de facturation nettoie aussi unités, espaces, profils et lits afin de ne plus polluer les classes rendez-vous/pharmacie/FHIR suivantes.
- **Feedback Angular de configuration spatiale** : le message « Configuration enregistrée » n'est plus effacé immédiatement par le rechargement post-mutation.
- **Tests Angular legacy** : migration des specs configuration spatiale, capacité, urgence→hospitalisation, permissions et `SpatialApiService` vers `Location / Unit / Space / Bed`.
- **Isolation des tests HOS-STAFF** : `StaffAssignmentControllerTest` crée des tenants uniques, positionne explicitement `TenantContext` pour les entités multi-tenant et nettoie seulement ses propres affectations/unités/utilisateurs/organisations ; aucune purge globale inter-tests n'est utilisée.

### Validation

- **HOS-ORG backend** : GitHub Actions CI #1066 — Maven strict `clean verify` vert sur le dernier changement backend fonctionnel, sans `skipTests`.
- **HOS-ORG PostgreSQL 16** : tests de migration V87 couvrant catalogues, FK tenant, contraintes d'identité SERVICE, type inconnu et isolation cross-tenant.
- **HOS-ORG PR #133** : squash merge dans `main` au commit `72b5e139592b20a9ea14ae366d3fcbab99c46cf1`.
- **HOS-LOC gate pré-synchronisation #1193** : Maven strict, tests Angular et build production verts sur `ae99538a01d6105e4efbfb246dd6031c994305ad`.
- **Synchronisation `main`** : `main@44b599a77c92f3ed812e62961e8ed7e1c9f3397b` intégré non destructivement dans #137 via le merge commit `243671d69bf1e44992626bb4a5ad9a19f3232fc7`; comparaison Git post-sync `behind_by = 0`.
- **HOS-LOC gate post-synchronisation #1196** : sur le même head synchronisé, `./mvnw clean verify` SUCCESS, tests PostgreSQL/Testcontainers SUCCESS, tests Angular SUCCESS et build Angular production SUCCESS.
- **HOS-LOC PR #137** : squash merge dans `main` au commit `e495477ea02beb05596b20b656bc092bd8fbbd83`; issue #131 fermée.
- **HOS-STAFF PostgreSQL** : `StructuredStaffAssignmentsPostgresqlMigrationTest` couvre preflight legacy, création du modèle structuré, contraintes et suppression des colonnes libres.
- **HOS-STAFF gate #1262** : sur `6c0b0123926aca04cdbe9f98b2206c70f9a15506`, Maven strict `clean verify` SUCCESS, tests Angular SUCCESS et build Angular production SUCCESS. Un gate final est relancé après consolidation documentaire avant merge.
- **Revue #140** : aucun thread de review ouvert lors du contrôle pré-consolidation ; branche `behind_by = 0` par rapport à `main`.

### Security

- aucun mapping automatique depuis `Ward.name`, `Room.roomNumber`, `users.department` ou `users.specialty` ;
- aucun fallback métier ou persistant n'est introduit pour identifier service, espace, lit, unité ou spécialité staff ;
- les affectations staff sont contrôlées par tenant côté application et base, avec FK composites et refus cross-tenant ;
- le rôle clinique d'affectation est distinct du rôle RBAC global et n'est jamais déduit automatiquement ;
- V92 bloque toute donnée legacy libre non vide avant la suppression V95 ;
- la projection Angular `StaffMember.department` résiduelle est un snapshot de présentation dérivé de l'unité principale active pour le module Visite historique, jamais persistant et jamais utilisé dans un payload d'écriture staff ;
- les affectations unité-espace, lits et séjours restent contrôlés par tenant côté application et base ;
- aucun secret ajouté au repository ;
- aucune action PROD/RECETTE pour HOS-ORG/HOS-LOC/HOS-STAFF ;
- `HOSPITALIZATION_MANAGE` reste supprimée depuis HOS-RBAC-001-D / V86.

### Documentation maintenance

- Le snapshot historique du changelog au moment de cette consolidation est conservé dans `docs/ai/CHANGELOG-HISTORY-THROUGH-20260723.md` afin de ne supprimer aucun travail documentaire antérieur tout en gardant le registre actif lisible.
- La documentation HOS-LOC canonique est `docs/ai/tickets/HOS-LOC-001-A-HOSPITAL-LOCATION-SPACES.md`; l'ancienne variante dupliquée a été supprimée.
- La documentation HOS-STAFF canonique est `docs/ai/tickets/HOS-STAFF-001-A-STRUCTURED-STAFF-ASSIGNMENTS.md`.
- `docs/features/clinic-staff-department-filter/TECHNICAL-DESIGN.md` est marqué **SUPERSEDED** : il décrit l'ancien filtrage basé sur `users.department` et ne doit plus guider de nouveau développement.
