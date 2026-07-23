# HOS-RBAC-001-B — Conception technique de la séparation des opérations hospitalières

> **Note d'évolution** : ce document décrit la conception livrée avec #102. Les étapes historiques de migration basées sur `HOSPITALIZATION_MANAGE` sont supersédées par HOS-RBAC-001-C/D. À partir de V86, cette permission n'existe plus dans le catalogue ni en base et aucun remapping automatique n'est effectué.

## Architecture

```text
RbacCatalog
  ├── HOSPITALIZATION_TRANSFER
  ├── HOSPITALIZATION_DISCHARGE_DECIDE
  ├── BED_CLEANING_MANAGE
  └── BED_MAINTENANCE_MANAGE

SpatialController
  ├── /transfers            → HOSPITALIZATION_TRANSFER
  ├── /cleaning-status      → BED_CLEANING_MANAGE
  ├── /maintenance-status   → BED_MAINTENANCE_MANAGE
  ├── /capacity-status      → BED_OPERATIONAL_STATUS_MANAGE
  └── /status legacy        → BED_OPERATIONAL_STATUS_MANAGE

HospitalizationController
  └── /{id}/discharge       → HOSPITALIZATION_DISCHARGE_DECIDE
```

## Catalogue RBAC

Les permissions sont ajoutées au catalogue Java, puis `RbacStore.seedCatalog()` :

1. insère ou actualise les définitions ;
2. recrée les associations des rôles système ;
3. conserve les rôles personnalisés et leurs affectations encore valides ;
4. expose les nouvelles permissions à l'administration RBAC.

Deux rôles système assignables sont ajoutés :

- `AGENT_HYGIENE` ;
- `TECHNICIEN_MAINTENANCE`.

Leurs identifiants restent déterministes via `RbacCatalog.roleId(code)`.

HOS-RBAC-001-D ajoute une règle de convergence que `seedCatalog()` ne pouvait pas fournir seul : Flyway V86 supprime explicitement la permission obsolète `HOSPITALIZATION_MANAGE`, et la FK `ON DELETE CASCADE` retire ses associations de rôles personnalisés.

## Transitions de préparation

`BedStatusTransitionPolicy` sépare les domaines :

```text
Circuit nettoyage
  READY → CLEANING
  CLEANING → READY

Circuit maintenance
  READY → MAINTENANCE
  MAINTENANCE → READY
```

Une transition est refusée lorsque :

- une affectation active existe ;
- le statut legacy est `OCCUPIED` sans affectation et exige une réconciliation ;
- le circuit demandé tente de terminer l'état de l'autre circuit ;
- une remise à `READY` est demandée alors que la capacité est fermée.

## Compatibilité du modèle lit

`BedEntity.setReadinessStatus()` modifie directement l'axe de préparation puis recalcule la projection legacy. Cette méthode est indispensable pour distinguer :

```text
capacityStatus = CLOSED
readinessStatus = READY
status legacy = MAINTENANCE
```

Le circuit maintenance ne doit pas interpréter cette projection legacy comme une maintenance technique réelle.

## Audit

Les actions sont distinguées :

- `UPDATE_BED_CAPACITY` ;
- `UPDATE_BED_CLEANING` ;
- `UPDATE_BED_MAINTENANCE` ;
- `UPDATE_BED_READINESS` pour la supervision legacy.

Les raisons textuelles de cet incrément ont ensuite été structurées par HOS-BED-002-D.

## Interface Angular

La page d'occupation interroge séparément :

- `BED_OPERATIONAL_STATUS_MANAGE` pour ouvrir/fermer ;
- `BED_CLEANING_MANAGE` pour démarrer/terminer le nettoyage ;
- `BED_MAINTENANCE_MANAGE` pour démarrer/terminer la maintenance.

Le bandeau du séjour évalue :

- `HOSPITALIZATION_TRANSFER` ;
- `HOSPITALIZATION_DISCHARGE_DECIDE`.

Le masquage UI améliore le parcours utilisateur, mais chaque endpoint est également protégé par `@PreAuthorize`.

## API et SemVer

Les endpoints spécialisés sont additifs. Deux endpoints existants ont changé de permission dans #102 :

- `/api/spatial/transfers` ;
- `/api/hospitalizations/{id}/discharge`.

HOS-RBAC-001-D va plus loin : la suppression de `HOSPITALIZATION_MANAGE` est un breaking change explicite du modèle d'autorisation pour tout rôle personnalisé qui la référençait. Selon les règles internes de SemVer, la release qui l'embarque porte un impact **MAJOR**.

## Déploiement — état actuel

Le plan historique de remappage de `HOSPITALIZATION_MANAGE` avant déploiement est abandonné pendant la phase de développement.

Le plan courant est :

1. appliquer V86 ;
2. constater la suppression fail-closed de la permission et de ses associations ;
3. démarrer le backend et resynchroniser le catalogue explicite ;
4. attribuer manuellement uniquement les permissions métier réellement nécessaires aux rôles personnalisés ;
5. renouveler les JWT/sessions ;
6. déployer le frontend ;
7. tester chaque profil avec des cas positifs et négatifs.

`BED_OPERATIONAL_STATUS_MANAGE` reste une permission active distincte ; elle n'est pas concernée par V86.

## Rollback

Après V86, un rollback qui réintroduit `HOSPITALIZATION_MANAGE` est interdit. Le modèle de sécurité est forward-only : corriger une permission dédiée ou une matrice de rôle est préférable à restaurer l'autorité générique.

## Tests

- présence des permissions dédiées dans le catalogue ;
- absence de `HOSPITALIZATION_MANAGE` à partir de 001-D ;
- matrice des rôles système ;
- annotations de contrôleur ;
- transitions positives nettoyage/maintenance ;
- refus inter-circuits ;
- refus sur lit affecté ;
- appels Angular aux endpoints spécialisés ;
- affichage des actions selon permission ;
- test PostgreSQL V85→V86 avec rôle personnalisé legacy ;
- suite complète Maven, PostgreSQL/Testcontainers, Angular et build production.
