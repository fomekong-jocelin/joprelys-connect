# HOS-RBAC-001-B — Conception technique de la séparation des opérations hospitalières

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
3. conserve les rôles personnalisés et leurs affectations ;
4. expose les nouvelles permissions à l'administration RBAC.

Deux rôles système assignables sont ajoutés :

- `AGENT_HYGIENE` ;
- `TECHNICIEN_MAINTENANCE`.

Leurs identifiants restent déterministes via `RbacCatalog.roleId(code)`.

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

Les raisons restent textuelles dans cet incrément. HOS-MOV-001 et HOS-BED-002-D devront introduire des événements structurés.

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

Les endpoints spécialisés sont additifs. Deux endpoints existants changent de permission :

- `/api/spatial/transfers` ;
- `/api/hospitalizations/{id}/discharge`.

Il s'agit d'un changement de contrôle d'accès potentiellement incompatible pour les rôles personnalisés. Une resynchronisation et une revue des rôles personnalisés sont obligatoires avant déploiement. L'impact de release est au minimum `MINOR`, avec note de migration RBAC.

## Déploiement

1. sauvegarder les associations RBAC ;
2. exécuter le démarrage backend afin de resynchroniser le catalogue ;
3. identifier les rôles personnalisés contenant `HOSPITALIZATION_MANAGE` ou `BED_OPERATIONAL_STATUS_MANAGE` ;
4. leur attribuer explicitement les nouveaux droits nécessaires ;
5. renouveler les JWT actifs ;
6. déployer le frontend ;
7. tester chaque profil avec des cas positifs et négatifs.

## Rollback

Un rollback applicatif rétablirait les anciennes annotations, mais les nouvelles permissions et rôles peuvent rester dans le catalogue sans affecter les anciens binaires. Ne pas supprimer les permissions avant d'avoir vérifié qu'aucun rôle personnalisé ne les référence.

## Tests

- présence des permissions dans le catalogue ;
- matrice des rôles système ;
- annotations de contrôleur ;
- transitions positives nettoyage/maintenance ;
- refus inter-circuits ;
- refus sur lit affecté ;
- appels Angular aux endpoints spécialisés ;
- affichage des actions selon permission ;
- suite complète Maven, PostgreSQL/Testcontainers, Angular et build production.
