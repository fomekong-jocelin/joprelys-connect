# Conception technique — Structure hospitalière

## Architecture

```text
Angular page/facade
    → API /api/spatial/configuration
        → SpatialConfigurationUseCase
            → DefaultSpatialConfigurationService
                → repositories tenant-aware + AuditService
```

Le backend reste maître des capacités, de l'unicité, des autorisations et des suppressions.

## Backend

- Contrôleur dédié sans logique métier.
- Interface de use case injectée dans le contrôleur.
- Implémentation transactionnelle dédiée.
- DTO immutables avec validation Jakarta.
- Repositories enrichis uniquement avec les requêtes nécessaires.
- `@TenantId` et contexte d'organisation existants conservés.
- Pour un administrateur plateforme, le contexte tenant est établi transactionnellement après validation de `organizationId`, puis restauré après l'opération.
- erreurs métier en `ResponseStatusException` 404/409.

## Frontend

- Route lazy-loaded `/clinic/spatial/configuration`.
- Page standalone réservée aux administrateurs.
- Service HTTP existant enrichi pour les opérations CRUD.
- Hiérarchie présentée sous forme de panneaux sobres et responsives.
- Tokens du design system existant, rayons maximum 6 px, light/dark compatibles.
- Textes via i18n FR/EN.

## Sécurité

- Autorisation backend obligatoire, indépendamment du menu Angular.
- Validation de toutes les entrées.
- Aucun identifiant d'organisation accepté depuis le client.
- Audit des créations, modifications et suppressions.
- Suppressions refusées lorsque des dépendances métier existent.

## Migration

Ajout d'index uniques tenant-aware sur :

- `(organization_id, service.name)` ;
- `(organization_id, ward_id, room_number)` ;
- `(organization_id, room_id, bed_number)`.

Le service applicatif complète ces index par un contrôle insensible à la casse avant écriture.
