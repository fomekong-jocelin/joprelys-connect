# Conception technique — Compteur des lits disponibles

## Architecture

Le backend reste maître de la règle de disponibilité.

```text
GET occupation
  → SpatialController
  → SpatialService.getWardOccupancy
  → comptage par BedStatus
  → WardOccupancyResponse.availableBedsCount
  → SpatialApiService
  → SpatialManagementPageComponent
```

## Backend Spring Boot

- ajouter `availableBedsCount` à `WardOccupancyResponse` ;
- incrémenter ce compteur uniquement pour `BedStatus.FREE` dans `SpatialService` ;
- conserver les sémantiques et noms des deux compteurs existants ;
- ne modifier ni entité, ni migration, ni endpoint d'écriture.

Le correctif reste dans le service applicatif transactionnel en lecture seule. Le contrôleur ne porte aucune règle métier.

## Angular

- ajouter `availableBedsCount` au type `WardOccupancy` ;
- exposer une projection de présentation testable qui retourne ce champ ;
- remplacer la soustraction locale du template par cette projection ;
- ne modifier ni texte visible, ni token, ni thème.

## API et compatibilité

Le JSON existant reçoit un champ additif. Aucun champ n'est supprimé ou renommé. Le frontend et le backend doivent néanmoins être déployés backend-first.

## Données et configuration

- migration : aucune ;
- `application.yml` : aucun changement ;
- proxy Angular : inchangé, appel relatif existant conservé ;
- secrets : aucun.

## Sécurité

Les protections existantes `HOSPITALIZATION_READ` sont conservées. Le nouveau compteur est un agrégat non nominatif et n'ajoute aucune donnée patient.

## Observabilité

Aucun nouveau log contenant des données de santé. Les métriques de divergence historique seront ajoutées avec la refonte HOS-KPI-001.

## Tests

- intégration MockMvc : `availableBedsCount` exclut `CLEANING` et `MAINTENANCE` ;
- Angular : le compteur affichable reprend la valeur backend même si `total - occupied` serait différent ;
- compilation TypeScript et tests ciblés ;
- suite Maven selon disponibilité de l'environnement.

## Impact SemVer

Champ API additif : `MINOR` probable, sans bump immédiat faute de préparation de release.
