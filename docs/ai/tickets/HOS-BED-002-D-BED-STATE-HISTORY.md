# HOS-BED-002-D — Motifs, acteurs et historique des états de lit

## Métadonnées

- **Epic** : EPIC-0027 — Organisation hospitalière, capacité et parcours patient
- **Audit** : AUDIT-20260721
- **Écarts réduits** : GAP-006, GAP-010, GAP-030
- **Priorité** : phase 0
- **Statut** : IMPLEMENTED / QA EN COURS
- **PR** : #104

## Problème

Les changements de capacité, nettoyage et maintenance modifiaient uniquement l'état courant du lit. Le motif, l'acteur, la source et la chronologie n'étaient pas disponibles sous forme structurée.

## Solution

La migration V84 ajoute un journal `bed_state_changes` append-only au niveau applicatif. Chaque événement contient :

- le lit et son établissement ;
- l'axe `CAPACITY` ou `READINESS` ;
- l'ancienne et la nouvelle valeur ;
- un motif codifié et une note facultative ;
- l'identifiant et le nom historisé de l'acteur ;
- la source manuelle ou automatique ;
- l'horodatage.

## Règles métier

- une fermeture de capacité exige un motif de fermeture compatible ;
- une réouverture exige `CAPACITY_REOPENING` ;
- le nettoyage manuel et la maintenance utilisent des catalogues séparés ;
- `CAPACITY_OTHER` et `CLEANING_INCIDENT` exigent une note ;
- `CLEANING_AFTER_TRANSFER` et `CLEANING_AFTER_DEPARTURE` sont réservés aux workflows automatiques ;
- les anciennes commandes `/status` sont conservées et tracées comme `LEGACY_SUPERVISION` ;
- aucun événement n'est créé si la valeur ne change pas.

## API

```text
GET  /api/spatial/beds/{id}/state-history
POST /api/spatial/beds/{id}/capacity-status
POST /api/spatial/beds/{id}/cleaning-status
POST /api/spatial/beds/{id}/maintenance-status
```

Les commandes spécialisées acceptent :

```json
{
  "status": "CLOSED",
  "reasonCode": "CAPACITY_STAFFING_SHORTAGE",
  "note": "Équipe de nuit incomplète"
}
```

## Compatibilité

La suppression physique legacy d'un lit supprime encore son historique par cascade. Cette concession est explicitement temporaire ; GAP-012 doit introduire l'archivage métier avant de rendre l'historique indépendant de la durée de vie physique de la ligne `beds`.

## Critères d'acceptation

- [x] V84 et contraintes tenant ajoutées ;
- [x] événements manuels historisés ;
- [x] transfert historisé avec `CLEANING_AFTER_TRANSFER` ;
- [x] départ physique historisé avec `CLEANING_AFTER_DEPARTURE` ;
- [x] motifs incompatibles refusés ;
- [x] note obligatoire pour les motifs ouverts/incident ;
- [x] endpoint de consultation protégé par `HOSPITALIZATION_READ` ;
- [x] compatibilité de l'ancien endpoint `/status` préservée ;
- [ ] CI complète verte ;
- [ ] validation du catalogue par responsable hospitalisation, hygiène et maintenance ;
- [ ] validation RSSI de la consultation de l'historique.

## Risques résiduels

- pas encore de tâche de turnover assignée ;
- pas d'ordre de travail maintenance ;
- pas de preuve structurée de contrôle ou de validation ;
- pas de correction append-only d'un événement erroné ;
- timestamps toujours alignés sur la stratégie actuelle `TIMESTAMP`, en attente de GAP-038.
