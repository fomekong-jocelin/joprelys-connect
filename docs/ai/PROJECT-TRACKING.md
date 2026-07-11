# PROJECT TRACKING — Suivi central technique et delivery

> Ce fichier reflète les travaux actifs. L'historique détaillé reste disponible dans Git.

## Statut global

| Champ | Valeur |
|---|---|
| Dernière mise à jour | 2026-07-11 — report du fournisseur OTP ; préparation sessions/révocation et URG-TEMP |
| Responsable mise à jour | Codex |
| État global | EPIC-0021 en cours ; EPIC-0022 et EPIC-0023 prêts à planifier |
| Risques majeurs | Sessions et révocation en mémoire ; vrai patient URG-TEMP impossible avec le modèle actuel ; rapprochement DPU sensible |
| Prochaine priorité | Démarrer en parallèle STORY-2401 (#31) et STORY-2301 (#40) |
| Capacité | À calculer avant engagement sprint |
| Charge préparée | 66 SP : sessions/révocation 18 SP + URG-TEMP 48 SP |

## Backlog actif

| ID | Titre | Statut | Priorité | SP | Dépendances | Issue |
|---|---|---|---:|---:|---|---:|
| EPIC-0021 | Complétion module par module du CDC V3.1 | IN_PROGRESS | P0 | — | — | #25 |
| EPIC-0022 | Sessions persistantes et révocation distribuée | READY | P0 | 18 | — | #29 |
| STORY-2401 | Sessions persistantes et refresh tokens rotatifs | READY | P0 | 8 | — | #31 |
| STORY-2402 | Révocation, logout-all et détection du rejeu | BLOCKED | P0 | 5 | STORY-2401 | #33 |
| STORY-2403 | Renouvellement et gestion des sessions Angular | BLOCKED | P0 | 5 | STORY-2401, STORY-2402 | #34 |
| EPIC-0023 | Patient URG-TEMP de l'arrivée à la régularisation | READY | P0 | 48 | — | #36 |
| STORY-2301 | Modèle patient provisoire et identifiant URG-TEMP | READY | P0 | 8 | — | #40 |
| STORY-2302 | Admission urgence et triage sans identité définitive | BLOCKED | P0 | 8 | STORY-2301 | #42 |
| STORY-2303 | Tiers, incapacité, urgence légale et effets personnels | BLOCKED | P0 | 8 | STORY-2302 | #44 |
| STORY-2304 | Régularisation et rapprochement avec le DPU | BLOCKED | P0 | 8 | STORY-2301, STORY-2303, ADR | #45 |
| STORY-2305 | Hospitalisation, documents et finance différée | BLOCKED | P0 | 8 | STORY-2302 à STORY-2304 | #46 |
| STORY-2306 | Workspace URG-TEMP, E2E et UAT | BLOCKED | P0 | 8 | STORY-2301 à STORY-2305 | #47 |

## Décisions

- Le fournisseur OTP SMS/e-mail est reporté à la fin du projet.
- Les sessions et la révocation sont traitées immédiatement sans dépendance payante.
- Les mesures gratuites de sécurité restent obligatoires : aucun OTP en log de production ni exposé au frontend hors profil local/test.
- Les deux lanes peuvent avancer en parallèle avec deux profils backend seniors distincts.

## Références

- `docs/ai/tickets/EPIC-0022-SESSIONS-AND-REVOCATION.md`
- `docs/ai/tickets/EPIC-0023-URG-TEMP-END-TO-END.md`
- `docs/pm/backlog/WAVE-1-SESSIONS-AND-URG-TEMP.md`
