# EPIC-0019 — Postes métier professionnels : hospitalisation et caisse

Référence de cadrage : `docs/ai/tickets/EPIC-0019-professional-clinical-financial-workspaces.md`.

| Story | Priorité | SP | Est. senior | Dépendance | Statut |
|---|---|---:|---:|---|---|
| STORY-2120 — Fondations UX et navigation par rôle | P0 | 3 | 1,0 j | Validation Médecin Chef / DAF | IN_PROGRESS |
| STORY-2121 — Synthèse de séjour, admission et transfert | P0 | 8 | 2,5 j | STORY-2120 | BACKLOG |
| STORY-2122 — Poste soignant et administration | P0 | 8 | 2,5 j | STORY-2120, validation cadre infirmier | BACKLOG |
| STORY-2123 — Poste médecin, bloc et sortie | P0 | 5 | 1,5 j | STORY-2121, validation Médecin Chef | BACKLOG |
| STORY-2124 — Poste caissier et exceptions | P0 | 5 | 1,5 j | EPIC-0018 consolidé, validation DAF | BACKLOG |
| STORY-2125 — E2E, RBAC et accessibilité | P0 | 5 | 1,5 j | Stories précédentes | BACKLOG |

## Capacité et recommandation

Charge estimée : 10,5 jours senior. Ce périmètre ne doit pas être engagé dans un sprint sans validation métier et capacité nette d'au moins 60 %, car il concerne des flux sensibles et l'existant contient du travail non versionné. Recommandation : réaliser STORY-2120 seule dans le prochain sprint, puis planifier les stories cliniques et financières par incréments validés.
