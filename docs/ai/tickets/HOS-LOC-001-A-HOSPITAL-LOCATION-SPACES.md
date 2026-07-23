# HOS-LOC-001-A — Géographie, espaces génériques et rattachement des lits

## Métadonnées

- Issue GitHub : #131
- Epic : EPIC-0027 / HOS-LOC-001
- Priorité : P0 avant répétition finale de la démonstration #127
- Baseline : `main@0a3516956a20b72608695d58cad9a76b04b23d9c`
- Branche : `feat/131-hos-loc-001-a`
- Statut : READY — audit et documentation avant code
- Profil : senior full-stack + DBA
- Reviewers : Tech Lead, DBA, cadre/logistique, responsable hospitalisation, Product

## Objectif

Remplacer proprement la structure spatiale ambiguë `Ward → Room → Bed` par :

1. une géographie facultative `SITE → BUILDING → FLOOR → ZONE` ;
2. des `SPACE` génériques typés ;
3. des rattachements datés `OrganizationalUnit ↔ Space` ;
4. des lits rattachés uniquement à des espaces d'hébergement compatibles ;
5. des contrats d'admission/transfert basés sur UUID, jamais sur les noms de service/chambre/lit.

## Diagnostic vérifié sur le main

- `WardEntity` mélange nom de service, capacité spatiale et tenant ;
- `RoomEntity` exige un `WardEntity` et ne peut représenter qu'une chambre sous service ;
- `BedEntity` exige un `RoomEntity` ;
- `BedRepository.findConfiguredBed(...)` retrouve un lit par `wardName + roomNumber + bedNumber` ;
- `HospitalizationAdmissionService` utilise ces trois chaînes comme clés de configuration ;
- `HospitalizationEntity` conserve `service_name`, `room_number`, `bed_number` sans UUID structurés ;
- `SpatialService.transferPatient(...)` déduit encore service et chambre depuis `bed.room.ward` ;
- `SpatialConfigurationController` expose encore CRUD physique `wards/rooms/beds` ;
- V44/V69/V74 portent le schéma legacy ;
- HOS-ORG-001-A / V87 fournit désormais la vraie structure organisationnelle.

## Décisions

- `Ward` et `Room` ne sont pas conservés comme aliases du nouveau modèle.
- `HospitalServiceType` n'est pas repris dans le nouveau référentiel géographique ; capacité fonctionnelle et identité organisationnelle restent séparées.
- Une `SPACE` est un objet physique/localisable ; un `SERVICE` est un objet organisationnel.
- Un espace peut être partagé par plusieurs unités et une unité peut utiliser plusieurs espaces.
- Un lit est rattaché à un espace d'hébergement via `space_id`.
- Les admissions utilisent `serviceUnitId`, `spaceId`, `bedId`.
- Les transferts utilisent `targetServiceUnitId`, `targetSpaceId`, `targetBedId` car un lit/space partagé ne permet pas de déduire l'unité métier.
- Les libellés lisibles dans l'hospitalisation deviennent des snapshots documentaires, pas des clés métier.
- Aucune migration par ressemblance de noms n'est admise.
- Une base contenant encore des données legacy spatiales/hospitalisations bloque la migration avant mutation destructive.

## Découpage réévalué

### Task A1 — data / migration / invariants — 5 SP
- préflight Flyway fail-fast legacy ;
- géographie, types d'espace, espaces, profil d'hébergement ;
- rattachements unité-espace datés ;
- beds `space_id` ;
- hospitalizations UUID structurés + snapshots ;
- retrait DB de `wards/rooms` sur base compatible.

### Task A2 — backend / contrats / sécurité — 5 SP
- API configuration géographie/espaces ;
- lecture capacité par espace/unité ;
- admission UUID ;
- transfert UUID ;
- suppression des repositories/services/DTO Ward/Room après migration de tous les consommateurs ;
- tests tenant, règles et concurrence.

### Task A3 — Angular / UX / i18n — 3 SP
- configuration géographique mobile-first ;
- configuration espaces/lits ;
- admission structurée service → espace → lit ;
- suppression de l'UI Ward/Room legacy ;
- FR/EN, light/dark, responsive, accessibilité ;
- tests + build.

**Total réévalué : 13 SP.**

Estimation : senior 5–7 j ; intermédiaire 7–10 j ; junior non recommandé seul.

## Action plan

- [x] auditer `Ward/Room/Bed`, migrations V44/V69/V74 et consommateurs ;
- [x] auditer admission/transfert et dépendances par noms ;
- [x] mettre à jour issue #131 avec le caractère breaking assumé ;
- [x] documenter fonctionnel, technique, data, API, tests et migration ;
- [ ] implémenter le préflight Flyway versionné ;
- [ ] implémenter le nouveau modèle géographique ;
- [ ] migrer beds/hospitalizations ;
- [ ] migrer admission/transfert ;
- [ ] retirer Ward/Room et `HospitalServiceType` si plus aucun consommateur ;
- [ ] implémenter UI mobile-first ;
- [ ] exécuter Maven strict, PostgreSQL 16, Angular tests/build ;
- [ ] aligner tracking/changelog/backlog ;
- [ ] PR Ready + CI réelle + squash merge.

## Critères d'acceptation

- [ ] les niveaux SITE/BUILDING/FLOOR/ZONE sont facultatifs et hiérarchiques ;
- [ ] un SPACE peut exister directement sous l'établissement ;
- [ ] les types d'espace sont contrôlés et localisés FR/EN ;
- [ ] seul un espace avec profil d'hébergement peut recevoir des lits ;
- [ ] `beds.room_id` est supprimé et remplacé par `space_id` ;
- [ ] les liens unité-espace sont datés et tenant-safe ;
- [ ] aucune période dupliquée/chevauchante pour le même couple unité-espace ;
- [ ] admission et transfert n'utilisent plus de noms comme clés ;
- [ ] hospitalisation stocke des UUID structurés + snapshots dérivés ;
- [ ] aucune API/UI active Ward/Room ne subsiste ;
- [ ] aucun `HospitalServiceType` ne subsiste si son dernier usage était Ward ;
- [ ] migration legacy incompatible fail-fast avant suppression/altération destructive ;
- [ ] greenfield V1 → nouvelle tête Flyway verte ;
- [ ] tenant isolation backend + DB ;
- [ ] UI 320/375/768/1366, FR/EN, light/dark, clavier/focus ;
- [ ] Maven strict + PostgreSQL + Angular + build verts.

## SemVer

Ce lot remplace des contrats actifs (`Ward/Room`, payload admission et structure des hospitalisations). Il est donc **MAJOR / breaking** même si le projet est encore en phase de développement.

Aucun alias ou constructeur backward-compatible n'est ajouté pour masquer ce changement.
