# Services hospitaliers typés — Conception technique

## Architecture

Le modèle reste organisé par couches :

```text
API REST
  → SpatialConfigurationUseCase
    → DefaultSpatialConfigurationService
      → WardEntity / HospitalServiceType
      → WardRepository / RoomRepository / BedRepository
```

Le backend porte la règle métier. Angular consomme `allowsRooms` et n'essaie pas de recalculer les capacités d'un type.

## Domaine

Une énumération `HospitalServiceType` décrit chaque catégorie et sa capacité à recevoir des chambres :

```java
HOSPITALIZATION(true)
EMERGENCY(true)
OUTPATIENT(false)
MEDICO_TECHNICAL(false)
PHARMACY(false)
ADMINISTRATIVE(false)
```

`WardEntity` reçoit obligatoirement le type à la construction. Aucun constructeur historique sans type n'est conservé.

## Persistance

La colonne `wards.service_type` est :

- `VARCHAR(40)` ;
- obligatoire ;
- sans valeur par défaut ;
- contrainte à l'ensemble fermé des types autorisés.

La migration ne devine pas les services historiques à partir du nom. Elle qualifie uniquement les services ayant déjà des chambres comme `HOSPITALIZATION`. Les services sans chambre doivent être classés avant déploiement ; sinon la migration échoue lors du passage en `NOT NULL`.

## Règles applicatives

### Création et modification d'un service

- normalisation du nom ;
- type obligatoire et directement désérialisé vers l'énumération ;
- refus du passage vers un type sans chambres lorsqu'une chambre existe ;
- audit incluant le type.

### Création ou déplacement d'une chambre

Le service cible est chargé dans le tenant courant. L'opération est refusée lorsque `serviceType.allowsRooms()` vaut `false`.

### Admission

L'admission recherche le lit par service, chambre et numéro. Le lit doit :

- exister ;
- appartenir à un service autorisant les chambres ;
- être libre.

L'ancien bloc d'auto-provisioning est supprimé. Aucun fallback n'est conservé.

### Initialisation

`AdminUserSeeder` reste responsable du compte administrateur uniquement. Les dépendances et le code de création automatique de services, chambres et lits sont retirés.

## Contrats Angular

```ts
export type HospitalServiceType =
  | 'HOSPITALIZATION'
  | 'EMERGENCY'
  | 'OUTPATIENT'
  | 'MEDICO_TECHNICAL'
  | 'PHARMACY'
  | 'ADMINISTRATIVE';
```

Les réponses de service exposent :

- `serviceType` ;
- `allowsRooms`.

Le formulaire exige `serviceType`. L'action « Ajouter une chambre » dépend uniquement de `allowsRooms` renvoyé par le backend.

## Internationalisation

Un dictionnaire feature-scoped `features/spatial/{fr,en}.json` porte les nouveaux libellés. `I18nService` le charge avec les autres dictionnaires optionnels.

## Sécurité

- permission `SPATIAL_CONFIGURATION_MANAGE` inchangée ;
- résolution du tenant inchangée ;
- aucune requête cross-tenant ;
- aucune autorisation fondée sur le frontend ;
- absence de création implicite lors d'une admission invalide.

## Observabilité et audit

Les événements CREATE/UPDATE de service mentionnent le nom et le type. Les refus utilisent des messages métier explicites et un statut `409 Conflict` pour une incohérence d'état.

## Limites de taille

La logique de capacité réside dans l'énumération. Les règles d'orchestration restent dans le service applicatif sans ajouter de logique aux contrôleurs.

## Impact SemVer

MAJOR :

- champ REST obligatoire ajouté ;
- ancien payload rejeté ;
- admission sans structure préconfigurée désormais refusée ;
- seeding spatial supprimé ;
- migration stricte des données.
