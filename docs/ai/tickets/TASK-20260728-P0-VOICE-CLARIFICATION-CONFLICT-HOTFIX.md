# TASK-20260728 — P0 Voice Clarification & Finalization Lock Hotfix

## Métadonnées

| Champ | Valeur |
|---|---|
| Mode | Diagnostic + Engineering + Project Manager |
| Priorité | P0 |
| Epic | EPIC-0024 — Assistant vocal IA de consultation |
| Stack | Angular 22, WebRTC, OpenAI Realtime, Spring Boot |
| Profil recommandé | Senior full-stack Angular / Spring Boot |
| Reviewer | Tech Lead + QA clinique |
| Estimation | 2 SP / 0.5 à 1 jour senior |
| Impact sprint | Hotfix immédiat sur canal de recette |
| Impact version | PATCH, cible `0.10.3` |

## Objectif

Résoudre le blocage en "Finalisation en cours..." et les erreurs HTTP 409 (Conflict) observées en recette lorsque l'assistant demande une clarification clinique et que l'utilisateur tente de finaliser ou d'envoyer un tour sans avoir clos la clarification.

## Diagnostic confirmé (Capture Recette `pasted-image-16151.png`)

1. **Blocage de finalisation UI** : Lorsqu'une clarification est en attente (`hasPendingClarification === true`), le clic sur "Terminer" positionne `finishPending = true` (bannière "Finalisation en cours..."), mais `tryCompleteFinish()` refuse d'émettre `endSession`. L'interface reste définitivement bloquée sur "Finalisation en cours..." tout en affichant l'encadré de clarification.
2. **HTTP 409 Conflict sur le backend** : Si un nouveau tour arrive ou si l'analyse est lancée alors qu'une clarification est ouverte, le backend rejette avec `HttpStatus.CONFLICT` (`AI_CLARIFICATION_PENDING`). Le coordinateur Angular ne traitait pas la raison `AI_CLARIFICATION_PENDING` dans les états bloquants attendus, causant des erreurs résiduelles dans le pipeline.
3. **Visite non trouvée (404)** : Un appel `GET /api/visits/fbcc406f-3db0-4f7a-baa8-ee0f8ff7c292` retourne 404 Not Found car le UUID de la visite n'existe pas en base de recette ou est obsolète.

## Actions

- [x] Identifier les causes racines du blocage UI et des requêtes 409 Conflict.
- [x] Inclure `AI_CLARIFICATION_PENDING` dans la gestion des raisons backend non fatales du coordinateur Angular.
- [x] Permettre l'abandon ou la résolution automatique des clarifications en attente lors de la finalisation explicite pour débloquer "Finalisation en cours...".
- [x] Ajouter les tests unitaires Angular et backend associés.
- [x] Mettre à jour la gouvernance, le tracking et le changelog.

## Definition of Done

- [x] La finalisation se termine proprement sans rester bloquée indéfiniment en "Finalisation en cours...".
- [x] Les réponses backend HTTP 409 (`AI_CLARIFICATION_PENDING`) ne provoquent plus de crash du pipeline.
- [x] Tests unitaires verts.
- [x] Documentation et suivi mis à jour.
