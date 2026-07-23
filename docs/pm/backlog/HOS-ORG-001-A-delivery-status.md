# HOS-ORG-001-A — Statut de livraison backlog

## Référence

- Epic : EPIC-0027 — Hospital Organization, Capacity & Patient Flow
- Parent : HOS-ORG-001
- Issue : #130
- PR : #133
- Merge : `main@72b5e139592b20a9ea14ae366d3fcbab99c46cf1`
- Statut : DONE
- Estimation : 9 SP
- Migration : V87

## Périmètre livré

| Task | Livrable | Statut |
|---|---|---|
| A1 | Référentiels service/spécialité, types d'unités, modèle tenant-scoped, V87 | DONE |
| A2 | API, validations hiérarchiques, RBAC, audit et isolation tenant | DONE |
| A3 | UI mobile-first FR/EN light/dark, route/menu permission-first et tests Angular | DONE |

## Critères d'acceptation

- [x] Organisation et géographie séparées.
- [x] Niveaux POLE/DEPARTMENT/SERVICE/CARE_UNIT facultatifs.
- [x] Service directement sous l'établissement possible.
- [x] Hiérarchie complète possible pour un hôpital complexe.
- [x] Services créés depuis un catalogue codifié.
- [x] Spécialités issues d'un catalogue codifié.
- [x] Aucun nom de SERVICE libre persisté.
- [x] Codes uniques par tenant.
- [x] Désactivation non destructive.
- [x] Isolation tenant backend et base.
- [x] Permission dédiée `ORGANIZATION_STRUCTURE_MANAGE`.
- [x] API et migrations testées sur PostgreSQL 16.
- [x] UI mobile-first, FR/EN, light/dark et sans Angular Material.
- [x] CI backend stricte et frontend vertes.

## Dette explicitement non acceptée

- aucun alias vers `Ward` comme nouveau service organisationnel ;
- aucun mapping automatique depuis les champs texte historiques ;
- aucun fallback de saisie libre pour le service ou la spécialité ;
- aucune logique métier dupliquée dans Angular.

## Dépendances du backlog

### Débloqué

- HOS-LOC-001-A / #131.

### Reste bloqué

- HOS-STAFF-001-A / #132 jusqu'à disponibilité du socle géographique et des rattachements unité-espace nécessaires au parcours complet.

## Prochaine action

Créer une branche depuis le `main` contenant #133, documenter HOS-LOC-001-A avant code, puis implémenter la structure géographique sans modifier directement PROD ou RECETTE.
