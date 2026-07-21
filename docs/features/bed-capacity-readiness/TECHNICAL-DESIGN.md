# HOS-BED-002-C — Conception technique capacité et préparation des lits

## Architecture

```text
beds.status                          (projection legacy conservée)
beds.capacity_status                 OPEN | CLOSED
beds.readiness_status                READY | CLEANING | MAINTENANCE
bed_assignments.released_at IS NULL  usage OCCUPIED dérivé

GET /api/spatial/wards/{id}/occupancy
  → lecture des lits du service
  → lecture groupée des affectations actives
  → projection des axes et compteurs
  → JSON additif

POST /api/spatial/beds/{id}/status
  → change la préparation legacy-compatible

POST /api/spatial/beds/{id}/capacity-status
  → ouvre ou ferme la capacité du lit
```

## Migration V81

V81 ajoute :

```sql
capacity_status  VARCHAR(20) NOT NULL DEFAULT 'OPEN'
readiness_status VARCHAR(20) NOT NULL DEFAULT 'READY'
```

Le backfill est déterministe :

- `FREE` et `OCCUPIED` deviennent `READY` ;
- `CLEANING` devient `CLEANING` ;
- `MAINTENANCE` devient `MAINTENANCE` ;
- tous les lits existants restent `OPEN` afin de ne pas réduire silencieusement la capacité.

Trois contraintes contrôlent les valeurs et la cohérence de la projection legacy. Un index `(organization_id, capacity_status, readiness_status)` prépare les lectures de capacité.

## Entité

`BedEntity` porte les deux nouveaux enums :

- `BedCapacityStatus` ;
- `BedReadinessStatus`.

`setStatus()` synchronise les axes lorsqu'un ancien workflow utilise encore les valeurs `FREE`, `CLEANING`, `MAINTENANCE` ou `OCCUPIED`.

`setCapacityStatus()` recalcule la projection legacy :

- fermeture d'un lit prêt → `MAINTENANCE` côté legacy ;
- réouverture d'un lit prêt → `FREE` ;
- nettoyage et maintenance conservent leur projection.

La méthode `isOperationallyAvailable(hasActiveAssignment)` centralise la règle :

```text
OPEN && READY && !activeAssignment && status == FREE
```

## Claim atomique

`BedRepository.claimIfAvailable()` remplace `claimIfFree()` et garde une écriture atomique :

```text
WHERE id = :bedId
  AND status = FREE
  AND capacityStatus = OPEN
  AND readinessStatus = READY
```

L'admission et le transfert utilisent cette même méthode. Une fermeture ou une maintenance concurrente fait donc perdre le claim sans créer d'affectation.

## Projection d'occupation

`SpatialService.getWardOccupancy()` :

1. charge les chambres ;
2. charge tous les lits du service ;
3. récupère en une requête les IDs de lits ayant une affectation active ;
4. groupe les lits par chambre ;
5. calcule les compteurs et `BedResponse`.

L'approche évite une requête d'affectation par lit.

## API

`BedResponse` conserve :

- `id`, `roomId`, `bedNumber`, `status`, `version`.

Il ajoute :

- `capacityStatus` ;
- `readinessStatus` ;
- `usageStatus` ;
- `available`.

`WardOccupancyResponse` ajoute :

- `openBedsCount` ;
- `readyBedsCount`.

Aucun champ existant n'est supprimé ou renommé.

## Sécurité

Les deux endpoints de mutation exigent :

```text
BED_OPERATIONAL_STATUS_MANAGE
```

`BedStatusTransitionPolicy` refuse toute mutation manuelle lorsqu'une affectation active existe ou lorsqu'un lit est projeté `OCCUPIED` sans affectation et nécessite une réconciliation.

## Interface Angular

L'écran affiche six indicateurs :

- installés ;
- ouverts ;
- prêts ;
- occupés ;
- disponibles ;
- taux d'occupation sur lits ouverts.

Chaque carte de lit affiche trois badges indépendants : capacité, préparation et usage. Les commandes fermer/ouvrir et préparation restent masquées sans la permission dédiée.

Les traductions sont stockées dans `features/spatial-services/fr.json` et `en.json`.

## Tests

### Base de données

`BedCapacityPostgresqlMigrationTest` vérifie :

- présence des colonnes et contraintes ;
- valeurs par défaut ;
- fermeture cohérente ;
- rejet d'une projection `FREE` sur un lit fermé.

### Repository

`BedRepositoryAvailabilityTest` vérifie que seul un lit ouvert, prêt et libre peut être claimé.

### Service

- transitions ouverture/fermeture ;
- refus sur lit affecté ;
- refus de `FREE` sur lit fermé ;
- compteurs séparés ;
- occupation dérivée des affectations actives.

### Frontend

- disponibilité reprise du backend ;
- taux calculé sur les lits ouverts ;
- endpoint de capacité dédié ;
- permission opérationnelle conservée.

## Déploiement

1. sauvegarder et appliquer V81 ;
2. vérifier que tous les lits existants sont `OPEN` ;
3. comparer les compteurs installés/ouverts/prêts/disponibles ;
4. déployer le backend avant le frontend ;
5. renouveler le contexte RBAC des utilisateurs ;
6. fermer manuellement uniquement les lits validés par le responsable hospitalisation.

## Rollback

Un rollback applicatif peut continuer à lire `status`, car il reste présent. Avant suppression des nouvelles colonnes, vérifier qu'aucun lit n'est `CLOSED`, faute de quoi un ancien client le verrait uniquement comme `MAINTENANCE` et perdrait l'information de fermeture.

## Limites connues

- `status` reste dupliqué comme projection de compatibilité ;
- les raisons et dates de fermeture ne sont pas historisées ;
- le statut de préparation reste encore regroupé pour hygiène et maintenance ;
- la cohérence d'une affectation active sur un lit fermé doit être surveillée pour les données historiques ;
- la capacité théorique de chambre n'est pas encore un axe métier complet.
