# EPIC-0026 — Cloisonnement permission-first de tous les rôles

## Valeur

Éliminer les accès implicites par nom de rôle et garantir qu'une révocation de permission retire réellement l'accès dans le menu, la route et l'API.

## Planning

| Story | SP | Effort senior | Dépendance | Sprint proposé |
|---|---:|---:|---|---|
| STORY-2701 Matrice canonique | 3 | 1,0 j | RSSI + métiers | SPRINT-0015 |
| STORY-2702 Angular permission-first | 5 | 2,0 j | STORY-2701 | SPRINT-0015 |
| STORY-2703 APIs cliniques/patient | 5 | 2,5 j | STORY-2701 | SPRINT-0016 |
| STORY-2704 APIs admin/finance | 5 | 2,5 j | STORY-2701 | SPRINT-0016 |
| STORY-2705 Tests matriciels/E2E | 3 | 1,5 j | STORY-2702 à 2704 | SPRINT-0016 |

Implémentation technique terminée le 2026-07-18. L'estimation de référence reste 21 SP / 9,5 j senior ; la livraison demeure conditionnée à la validation de la matrice et à la disponibilité des reviewers RSSI/métiers.

## DoR

- Incident P0 corrigé et diagnostic validé.
- Matrice initiale produite.
- Product Owner, RSSI, DAF et référents cliniques disponibles.
- Environnement Maven/CI opérationnel.

## DoD

- Aucun fallback rôle-only non documenté sur une action permissionnée.
- Tests positifs/négatifs par rôle et permission.
- Revue sécurité et recette E2E approuvées.
- Documentation et changelog à jour.
