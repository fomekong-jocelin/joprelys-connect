# Consultation IA interactive — Conception technique

## 1. Architecture cible

Le changement reste dans le module IA existant et respecte le flux :

```text
Angular presentation
  → AiConsultationFacade
    → REST controller
      → application use cases
        → domain session model
          ← AI provider infrastructure
```

Le backend reste maître de l’état métier, des validations, des décisions de proposition et de l’isolation tenant.

## 2. Refactor backend

La classe `AiConsultationService` actuelle concentre trop de responsabilités. Elle sera découpée sans casser les endpoints existants :

- `AiSessionApplicationService` : démarrage, lecture, expiration et suppression.
- `AiConversationApplicationService` : messages texte et historique.
- `AiTranscriptionApplicationService` : transcription, relecture, confirmation et abandon.
- `AiRevisionApplicationService` : propositions, acceptation et rejet.
- `AiProviderResponseParser` : validation/parsing strict de la sortie IA.
- `AiSessionStore` : interface de stockage ; implémentation mémoire pour le pilote.

## 3. Modèle de domaine

### AiSession

- sessionId
- visitId
- userId
- organizationId
- expiresAt
- acceptedDraft
- messages
- pendingTranscription
- clarifications
- proposals
- revision

### ConversationMessage

- id
- role
- type
- content
- createdAt
- status
- relatedEntityId facultatif

### PendingTranscription

- id
- originalText
- editedText
- mimeType
- status
- createdAt

### Clarification

- id
- field
- question
- options
- status
- answer
- createdAt
- resolvedAt

### FieldProposal

- id
- field
- previousValue
- proposedValue
- reason
- uncertainty
- decision
- createdAt
- decidedAt

## 4. Concurrence et synchronisation

- Chaque session possède un compteur `revision` incrémenté à chaque mutation.
- Les commandes de confirmation et de décision reçoivent `expectedRevision`.
- En cas de révision obsolète, retourner `409 AI_SESSION_CONFLICT` avec l’état courant récupérable.
- Les mutations restent synchronisées dans le store mémoire du pilote.

## 5. Compatibilité API

Les endpoints actuels restent disponibles :

- création/lecture/suppression de session ;
- message texte ;
- message audio.

Le endpoint audio change de sémantique de manière additive : il produit une transcription en attente et ne lance plus l’analyse. Pour éviter un changement silencieux, un nouvel endpoint explicite sera utilisé par le nouveau frontend, tandis que l’ancien endpoint pourra conserver temporairement son comportement avec dépréciation documentée, ou être versionné selon validation de l’API contract.

Approche retenue pour la première PR : endpoints additifs sous le même préfixe, sans supprimer les contrats existants.

## 6. Endpoints nouveaux

- `POST /api/ai/consultations/{visitId}/transcriptions`
- `PATCH /api/ai/consultations/{visitId}/transcriptions/{id}`
- `POST /api/ai/consultations/{visitId}/transcriptions/{id}/confirm`
- `POST /api/ai/consultations/{visitId}/transcriptions/{id}/discard`
- `POST /api/ai/consultations/{visitId}/clarifications/{id}/responses`
- `POST /api/ai/consultations/{visitId}/proposals/{id}/accept`
- `POST /api/ai/consultations/{visitId}/proposals/{id}/reject`
- `POST /api/ai/consultations/{visitId}/proposals/accept-all`
- `POST /api/ai/consultations/{visitId}/proposals/reject-all`

## 7. Sortie IA

Le modèle doit retourner un JSON strict :

```json
{
  "assistantMessage": "...",
  "clarification": {
    "field": "symptoms",
    "question": "...",
    "options": []
  },
  "proposals": [
    {
      "field": "symptoms",
      "previousValue": "...",
      "proposedValue": "...",
      "reason": "...",
      "uncertainty": false
    }
  ]
}
```

Le backend recalcule `previousValue` depuis l’état serveur et ne fait jamais confiance à cette valeur fournie par le modèle.

## 8. Frontend Angular

Le composant actuel sera décomposé :

```text
consultation/ai/
  ai-consultation.facade.ts
  ai-consultation.models.ts
  ai-assistant-panel.component.ts
  conversation-thread.component.ts
  transcription-review.component.ts
  clarification-card.component.ts
  proposal-list.component.ts
  proposal-card.component.ts
  voice-recorder.service.ts
```

Règles :

- `HttpClient` uniquement dans la façade/service.
- Composants de présentation par `@Input`/`@Output`.
- Signals pour l’état local.
- URLs API relatives.
- Tailwind CSS v4 et tokens existants.
- Arrondis 4 à 6 px, 8 px maximum pour panneaux.
- Textes FR/EN via `I18nService`.
- Aucun contenu clinique dans `localStorage`.

## 9. Sécurité

- Conserver `@PreAuthorize` actuel.
- Revalider visite `EN_COURS` à chaque commande.
- Vérifier ownership session/visit/user/organization.
- Taille texte maximale 12 000 caractères.
- Audio limité à 10 MiB et types allowlistés.
- Aucun contenu clinique dans les logs.
- Ne jamais exposer les messages système ni les prompts internes à l’API.
- Protection contre prompt injection : le texte utilisateur est traité comme donnée clinique ; les instructions utilisateur ne peuvent pas changer les règles système ni les champs autorisés.

## 10. Observabilité

Journaliser uniquement :

- provider ;
- identifiant technique de session haché ou corrélation ;
- type d’opération ;
- durée ;
- taille audio ;
- code d’erreur.

Ne jamais journaliser transcript, réponse IA, brouillon ou propositions.

## 11. Tests

- Unitaires domaine : états, transitions, révision, accept/reject.
- Services : multi-tour, clarification, transcription confirm/discard.
- Controller : validation, auth, conflits, erreurs.
- Provider parser : JSON invalide, champs inconnus, ambiguïtés.
- Angular : façade, fil, éditeur de transcription, décisions.
- E2E : texte, audio, clarification et application finale.

## 12. Impacts

- Spring Boot : nouveau modèle applicatif et DTO additifs.
- Angular : refactor du panneau actuel et nouveaux composants.
- DB : aucune migration dans le pilote mémoire.
- CI : suites Maven/Angular existantes.
- SemVer : MINOR.
