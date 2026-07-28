# ADR-20260717 — Limites et ports de l’assistant vocal de consultation

## Statut

Accepté pour l’architecture interne, amendé le 2026-07-28 pour le transport
Realtime. Activation production bloquée par validation DPO, fournisseur,
évaluation clinique et recette réelle du hotfix vocal P0.

## Date

2026-07-17

## Contexte

L’epic initiale proposait une interface `AiProvider` unique faisant transcription
et conversation pour OpenAI, Gemini et Claude. Elle force des capacités que
certains fournisseurs n’offrent pas et masque les transferts entre sous-traitants.

## Décision

1. Séparer `AudioTranscriptionPort` et `ConsultationDraftAssistantPort`.
2. L’IA produit un brouillon, jamais une autorité clinique.
3. La sauvegarde existante intervient après validation explicite du médecin.
4. La feature est désactivée par défaut.
5. Un seul adaptateur approuvé est activé ; les autres restent expérimentaux.
6. Aucun audio ni contenu clinique n’est écrit dans les logs techniques.
   L’audio ambiant est journalisé temporairement uniquement par le mécanisme
   sécurisé prévu à cet effet, puis purgé selon sa politique ; les transcripts
   finalisés sont conservés dans le ledger clinique append-only existant afin
   d’empêcher les pertes silencieuses.
7. Le store conversationnel de session reste derrière un port.
8. La session OpenAI Realtime sert uniquement de transport ASR :
   `output_modalities=["text"]`, `create_response=false`, aucune sortie audio
   autonome et aucun `response.create` émis par le client.
9. La conversation clinique reste sous le contrôle du backend Joprelys. En
   Realtime seulement, un `assistantMessage` ou une clarification validée peut
   être lu une fois par le TTS dédié ; la Dictée reste passive.
10. La voix TTS ne coupe jamais le sender. La reprise de parole interrompt la
    lecture ; seuls la pause, la finalisation et l’échec durable fail-closed
    peuvent couper la capture.
11. Le dernier transcript Realtime reste corrigeable par le médecin, quelle que
    soit la confiance ASR. « Terminer » draine les files et applique le brouillon
    avant de détruire le contrôleur.

## Raisons

- Respect de SOLID/ISP et transparence des sous-traitants.
- Réduction du risque de sauvegarde autonome.
- Data minimization et rollback immédiat.
- Tests indépendants des APIs externes.

## Conséquences positives

- STT et extraction évoluent indépendamment.
- Responsabilités provider/métier testables.
- Mode manuel intact.
- Aucun modèle Realtime externe ne peut produire de réponse clinique ou lire
  une instruction interne de sa propre initiative.
- Une correction humaine et une finalisation ne détruisent plus les tours déjà
  captés.

## Conséquences négatives / risques

- Deux contrats externes potentiels.
- Latence et coût cumulés.
- Validation DPO et médicale obligatoire.
- Store mémoire non multi-instance.
- Le TTS backend reste un canal de divulgation sonore à maîtriser par la recette
  clinique, le barge-in et le futur contrôle mute utilisateur.

## Alternatives rejetées

| Alternative | Raison |
|---|---|
| Interface unique STT + chat | viole ISP et masque les fallbacks |
| Web Speech API STT | qualité/compatibilité non maîtrisées |
| Modèle Realtime full-duplex autorisé à répondre directement | contourne le pipeline clinique backend, double le canal TTS et expose les instructions internes |
| Sauvegarde automatique | risque clinique |
| Historique IA en base | finalité/rétention non validées |

## Impact planning

| Élément | Impact |
|---|---|
| Charge | 25–33 jours senior |
| Risque | élevé |
| Profils | backend sécurité, Angular, médecin, DPO, QA |
| Sprint | 3–4 indicatifs, capacité à confirmer |

## Références

- `docs/features/ai-voice-consultation/TECHNICAL-DESIGN.md`
- `docs/ai/tickets/TASK-20260728-P0-VOICE-HOTFIX-B.md`
- `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`
- Documentation officielle OpenAI Speech-to-text, Realtime VAD et événement
  `input_audio_transcription.completed` (revue le 2026-07-28)
