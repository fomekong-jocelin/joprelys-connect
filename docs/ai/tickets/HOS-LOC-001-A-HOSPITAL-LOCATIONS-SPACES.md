# HOS-LOC-001-A — Sites, bâtiments, étages, zones et espaces hospitaliers

## Métadonnées

- Issue GitHub : #131
- Epic : EPIC-0027 / HOS-LOC-001
- Dépendance terminée : HOS-ORG-001-A / #130 / PR #133
- Baseline : `main@0a3516956a20b72608695d58cad9a76b04b23d9c`
- Branche : `feat/131-hos-loc-001-a`
- Priorité : P0 avant finalisation des données de démonstration #127
- Estimation : 9 SP, à revalider après audit exhaustif des consommateurs `Ward/Room/Bed`
- Statut : IN_PROGRESS — documentation avant code
- Reviewers : Tech Lead, DBA, Product, cadre hospitalier/logistique

## Objectif

Introduire une géographie hospitalière explicite et indépendante de l'organisation médicale :

`SITE → BUILDING → FLOOR → ZONE → SPACE`

Tous les niveaux intermédiaires sont facultatifs. Un petit cabinet peut créer directement ses espaces sous l'établissement ou sous un site unique ; un hôpital complexe peut utiliser toute la hiérarchie.

## Problème actuel

Le modèle historique `Ward → Room → Bed` confond :

- service médical ;
- localisation physique ;
- chambre d'hospitalisation ;
- capacité en lits.

Il ne représente pas proprement une salle de consultation, un box d'urgence, une salle d'attente, un laboratoire, une pharmacie, un bloc, une morgue ou un espace partagé.

## Décisions obligatoires

- organisation et géographie restent séparées ;
- aucune entité `OrganizationalUnit` ne devient enfant d'un bâtiment ;
- un espace peut être lié à plusieurs unités organisationnelles par des rattachements datés ;
- une unité peut utiliser plusieurs espaces, sites ou bâtiments ;
- le type d'espace vient d'un catalogue codifié ;
- aucun type d'espace libre n'est ajouté ;
- aucun mapping automatique depuis `wards.name` ou `rooms.name` n'est autorisé sans règle explicite et preuve ;
- aucun nouveau parcours métier ne doit dépendre d'un nom de salle ou de service libre ;
- les lits ne sont autorisés que dans des espaces dont le type accepte l'hébergement ;
- aucun fallback final vers `Ward/Room` n'est accepté.

## Périmètre

### A1 — Géographie et espaces — 3 SP

- hiérarchie tenant-scoped `SITE/BUILDING/FLOOR/ZONE/SPACE` ;
- catalogue des types d'espace ;
- règles parent/enfant ;
- activation/désactivation ;
- contraintes tenant, unicité et cycles.

### A2 — Rattachements organisation ↔ espace et capacité — 3 SP

- relation N–N datée entre `organizational_units` et les espaces ;
- rattachement principal/secondaire ;
- périodes `validFrom/validTo` ;
- validation tenant et période ;
- déclaration explicite des types d'espace compatibles avec des lits ;
- stratégie de migration/cutover des chambres et lits actuels.

### A3 — API, UI mobile-first et tests — 3 SP

- API de configuration ;
- écran mobile-first FR/EN light/dark ;
- navigation hiérarchique ;
- création depuis catalogues contrôlés ;
- tests backend, PostgreSQL et Angular ;
- documentation et runbook de migration.

## Catalogue initial des types d'espace

Au minimum :

- `CONSULTATION_ROOM` ;
- `TREATMENT_ROOM` ;
- `EMERGENCY_BOX` ;
- `WAITING_ROOM` ;
- `HOSPITAL_ROOM` ;
- `OPERATING_ROOM` ;
- `RECOVERY_ROOM` ;
- `INTENSIVE_CARE_ROOM` ;
- `LABORATORY_ROOM` ;
- `IMAGING_ROOM` ;
- `PHARMACY_SPACE` ;
- `STORAGE_SPACE` ;
- `OFFICE` ;
- `MORGUE_SPACE` ;
- `SANITARY_SPACE` ;
- `OTHER_CONTROLLED`.

Chaque type précise notamment s'il peut contenir des lits.

## Critères d'acceptation

- [ ] Un établissement simple peut créer directement un espace sans bâtiment artificiel.
- [ ] Un hôpital peut configurer plusieurs sites, bâtiments, étages et zones.
- [ ] Les niveaux intermédiaires sont facultatifs sans casser les validations.
- [ ] Un espace possède un code stable, un type contrôlé et un nom local propre.
- [ ] Un box d'urgence, une consultation, une salle d'attente, un laboratoire et une pharmacie sont représentables.
- [ ] Une chambre d'hospitalisation est un espace de type compatible avec les lits.
- [ ] Un espace peut être partagé entre plusieurs unités via des liens datés.
- [ ] Une unité peut utiliser des espaces répartis sur plusieurs bâtiments/sites.
- [ ] Aucun parent ou lien cross-tenant n'est possible.
- [ ] Les cycles géographiques sont refusés.
- [ ] Une désactivation destructive est refusée si des descendants ou rattachements actifs existent.
- [ ] Aucune nouvelle saisie libre de type de salle/service n'est introduite.
- [ ] UI 320/375/768/1366 px, clavier/focus, FR/EN et light/dark.
- [ ] Aucun Angular Material, aucune URL backend ou couleur métier hardcodée.
- [ ] Maven strict, PostgreSQL 16, tests Angular et build production verts.

## Definition of Ready

- [x] HOS-ORG-001-A fusionné et documenté.
- [x] Issue #131 créée.
- [x] Branche créée depuis le `main` contenant #133/#135.
- [ ] Audit exhaustif des consommateurs `Ward/Room/Bed` terminé.
- [ ] Design fonctionnel, technique, data, API et tests validé avant migration.
- [ ] Décision de cutover legacy documentée.

## Definition of Done

- modèle, migration, API, UI, tests et documentation alignés ;
- aucune dépendance nouvelle au modèle ambigu `Ward/Room` ;
- stratégie de sortie du legacy exécutable et testée ;
- CI verte ;
- tracking/changelog/backlog alignés ;
- aucune action serveur directe.

## Risques

- nombre réel de consommateurs `Ward/Room/Bed` supérieur à l'estimation initiale ;
- confusion entre chambre et espace générique ;
- migration de données impossible automatiquement en présence de libellés ambigus ;
- impact admission/transfert/disponibilité des lits ;
- risque de PR trop large si le cutover n'est pas séquencé avec des critères de sortie stricts.

## Plan immédiat

1. inventorier entités, migrations, repositories, DTO, services, contrôleurs, écrans et tests utilisant `Ward/Room/Bed` ;
2. finaliser le modèle géographique et les invariants ;
3. décider le découpage de migration sans dette permanente ;
4. écrire les contrats avant code ;
5. seulement ensuite commencer Flyway et backend.
