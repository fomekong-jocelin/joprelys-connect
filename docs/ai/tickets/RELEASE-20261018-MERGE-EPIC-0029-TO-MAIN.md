# RELEASE-20261018-MERGE-EPIC-0029-TO-MAIN — Consolidation et fusion d'EPIC-0029 et outillage local sur main

## Metadata
- **ID** : RELEASE-20261018-MERGE-EPIC-0029-TO-MAIN
- **Type** : Release / Engineering / Consolidation
- **Statut** : IN_PROGRESS
- **Priorité** : P0
- **Auteur** : Antigravity
- **Date** : 2026-10-18
- **Branche source** : `feature/epic-0029-journey-coherence`
- **Branche cible** : `main`

## Objectif
Consolider les commits et modifications d'EPIC-0029 (lots 1 à 3 — sécurité clinique, continuité des épisodes, facturation fiable), l'outillage de messagerie locale MailDev et de gestion d'erreur OTP, les scripts d'exploitation et la validation E2E temps réel, puis fusionner proprement l'ensemble dans la branche principale `main` conformément aux exigences de gouvernance :
- Respect de l'intégrité de `.gitignore` (artefacts temporaires et builds multimédias exclus) ;
- Validation complète des tests backend Spring Boot et frontend Angular ;
- Alignement du suivi projet `PROJECT-TRACKING.md`, du `CHANGELOG.md` et de la matrice de version SemVer ;
- Fusion traçable dans `main`.

## Actions prévues
- [x] Vérifier et mettre à jour `.gitignore` pour exclure les artefacts de tests navigateur (`.playwright-mcp/`, captures d'écran) et de génération vidéo (`video_build/`, `*.mp4`).
- [x] Valider les tests ciblés et non-régression backend (22 tests `AuthenticationServiceTest`, `*Mail*Test` verts).
- [x] Valider les tests Angular web (120 fichiers, 665 tests verts).
- [x] Valider le build de production Angular (`ng build` vert).
- [ ] Commiter les modifications d'outillage mail, gestion d'erreurs OTP et recette E2E sur `feature/epic-0029-journey-coherence`.
- [ ] Mettre à jour `docs/ai/tickets/STORY-20261017-EPIC-0029-LOTS-1-3.md` (statuts des actions).
- [ ] Mettre à jour `docs/ai/PROJECT-TRACKING.md` et `docs/ai/CHANGELOG.md`.
- [ ] Basculer sur `main` et fusionner `feature/epic-0029-journey-coherence`.
- [ ] Vérifier l'état final de `main` (historique, statut git, tests).
