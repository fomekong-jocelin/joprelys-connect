# HOS-LOC-001-A — Géographie, espaces génériques et rattachement des lits

## Métadonnées

- Issue GitHub : #131
- Epic : EPIC-0027 / HOS-LOC-001
- Priorité : P0 avant répétition finale de la démonstration #127
- Baseline de départ : `main@0a3516956a20b72608695d58cad9a76b04b23d9c`
- Baseline resynchronisée avant fusion : `main@44b599a77c92f3ed812e62961e8ed7e1c9f3397b`
- Commit de synchronisation : `243671d69bf1e44992626bb4a5ad9a19f3232fc7`
- Branche : `feat/131-hos-loc-001-a`
- PR : #137
- Statut : DONE TECHNIQUE — candidate au squash merge
- Estimation réévaluée : 13 SP
- Profil : senior full-stack + DBA
- Reviewers : Tech Lead, DBA, cadre/logistique, responsable hospitalisation, Product

## Objectif livré

Le modèle ambigu `Ward → Room → Bed` est remplacé par trois dimensions distinctes :

1. une géographie facultative `SITE → BUILDING → FLOOR → ZONE` ;
2. des `SPACE` génériques typés, pouvant exister directement sous l'établissement ;
3. des rattachements datés `OrganizationalUnit ↔ Space` ;
4. des lits rattachés uniquement à des espaces d'hébergement compatibles ;
5. des admissions et transferts basés sur `serviceUnitId`, `spaceId` et `bedId`.

`SERVICE` / `CARE_UNIT` reste une identité organisationnelle. `SPACE` reste une identité physique. Aucune salle n'est déduite d'un nom de service et aucun service n'est déduit d'un nom de salle.

## Décisions finales

- `WardEntity`, `RoomEntity`, leurs repositories, DTO et anciennes routes de configuration sont retirés du code de production.
- `HospitalServiceType` n'est plus utilisé comme pseudo-référentiel spatial.
- un `FacilitySpace` possède un type contrôlé et peut être rattaché à zéro ou une localisation géographique ;
- un `FacilitySpace` peut être utilisé par plusieurs unités organisationnelles ;
- un `OrganizationalUnit` peut utiliser plusieurs espaces ;
- les rattachements unité-espace sont datés et tenant-safe ;
- un lit référence `space_id`, jamais `room_id` ;
- un séjour conserve les UUID structurés courants et des snapshots lisibles pour les documents ;
- le niveau de confort STANDARD/VIP est porté par `InpatientSpaceProfile`, afin de conserver la logique de facturation sans dépendre d'une ancienne `Room` ;
- aucune migration par ressemblance de noms n'est admise ;
- une base contenant encore des données legacy spatiales ou d'hospitalisation bloque avant toute mutation destructive.

## Découpage livré

### Task A1 — data / migration / invariants — 5 SP

- [x] préflight Flyway fail-fast legacy ;
- [x] géographie et espaces ;
- [x] catalogue contrôlé des types d'espace ;
- [x] profil d'hébergement ;
- [x] rattachements unité-espace datés ;
- [x] migration `beds.room_id → beds.space_id` ;
- [x] hospitalisations avec UUID structurés + snapshots ;
- [x] retrait DB de `wards/rooms` sur base compatible.

### Task A2 — backend / contrats / sécurité — 5 SP

- [x] API de configuration géographie/espaces ;
- [x] CRUD des lits par `spaceId` ;
- [x] profil d'hébergement et confort ;
- [x] lecture de capacité par espace et unité ;
- [x] admission UUID ;
- [x] transfert UUID ;
- [x] claim atomique du lit préservé ;
- [x] isolation tenant applicative et contraintes DB ;
- [x] suppression des consommateurs backend `Ward/Room` ;
- [x] tests de concurrence, intégrité, capacité et transfert.

### Task A3 — Angular / UX / tests — 3 SP

- [x] configuration géographique mobile-first ;
- [x] configuration espaces / profils / lits ;
- [x] affichage des rattachements unité ↔ espace séparément ;
- [x] admission structurée unité → espace → lit ;
- [x] continuité urgence → hospitalisation structurée ;
- [x] tableau de capacité par espace ;
- [x] anciennes routes Angular Ward/Room retirées ;
- [x] tests Angular ;
- [x] build Angular production.

## Migrations

- **V88** — preflight Java : fail-fast si données legacy incompatibles ; aucune mutation destructive avant validation ;
- **V89** — création géographie/espaces/catalogues/profils/rattachements, passage des lits à `space_id`, références structurées des séjours, suppression `wards/rooms` ;
- **V90** — PostgreSQL : interdiction des périodes unité-espace chevauchantes ;
- **V91** — ajout du `comfort_level` au profil d'hébergement pour préserver la tarification STANDARD/VIP.

## QA automatisée

### Gate pré-synchronisation

- CI #1193 : backend Maven strict + tests Angular + build production verts sur `ae99538a01d6105e4efbfb246dd6031c994305ad`.

### Synchronisation main

- `main@44b599a77c92f3ed812e62961e8ed7e1c9f3397b` fusionné non destructivement dans la branche via `243671d69bf1e44992626bb4a5ad9a19f3232fc7` ;
- comparaison Git : `behind_by = 0` après synchronisation.

### Gate post-synchronisation

- CI #1196 :
  - [x] `./mvnw clean verify` SUCCESS ;
  - [x] PostgreSQL/Testcontainers SUCCESS dans la suite Maven ;
  - [x] tests Angular SUCCESS ;
  - [x] build Angular production SUCCESS.

## Critères d'acceptation

- [x] les niveaux SITE/BUILDING/FLOOR/ZONE sont facultatifs et hiérarchiques ;
- [x] un SPACE peut exister directement sous l'établissement ;
- [x] les types d'espace sont contrôlés et localisés FR/EN ;
- [x] seul un espace avec profil d'hébergement peut recevoir des lits ;
- [x] `beds.room_id` est supprimé et remplacé par `space_id` ;
- [x] les liens unité-espace sont datés et tenant-safe ;
- [x] les périodes chevauchantes pour le même couple unité-espace sont refusées sur PostgreSQL ;
- [x] admission et transfert n'utilisent plus de noms comme clés ;
- [x] l'hospitalisation stocke des UUID structurés + snapshots dérivés ;
- [x] aucune API de configuration active Ward/Room ne subsiste ;
- [x] `HospitalServiceType` n'est plus nécessaire au modèle spatial ;
- [x] migration legacy incompatible fail-fast avant suppression/altération destructive ;
- [x] greenfield V1 → tête Flyway validé par la suite automatisée ;
- [x] tenant isolation backend + DB ;
- [x] Maven strict + PostgreSQL + tests Angular + build production verts ;
- [ ] recette visuelle humaine 320/375/768/1366, FR/EN et light/dark à rejouer dans le jalon #127.

## Definition of Done technique

- [x] modèle, migrations, API, UI et tests alignés ;
- [x] aucun consommateur backend actif ne dépend de `Ward/Room` comme identité métier ;
- [x] admission/transfert basés sur UUID ;
- [x] facturation de l'hébergement portée par le profil d'espace ;
- [x] CI réelle verte après synchronisation avec `main` ;
- [x] aucun thread de review ouvert au moment de la clôture documentaire ;
- [x] aucune action PROD/RECETTE réalisée ;
- [ ] squash merge PR #137 ;
- [ ] UAT / répétition humaine #127.

## SemVer

Ce lot remplace des contrats actifs (`Ward/Room`, payload d'admission, transfert et structure des hospitalisations). Il est donc **MAJOR / breaking** même si le projet est encore en phase de développement.

Un alias de présentation Angular temporaire peut exposer `roomNumber = spaceName` uniquement à un ancien fragment d'affichage historique. Il n'est jamais persistant, n'est jamais envoyé dans un payload d'écriture et ne constitue pas une identité métier. Sa suppression visuelle complète peut être faite sans changement de domaine ni de base.
