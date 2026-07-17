# ADR-20260717 — Limites et ports de l’assistant vocal de consultation

## Statut

Accepté pour l’architecture interne. Activation production bloquée par validation
DPO, fournisseur et évaluation clinique.

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
6. Aucun audio ni contenu clinique n’est persisté ou journalisé.
7. Le store de session est derrière un port ; mémoire seulement en pilote.

## Raisons

- Respect de SOLID/ISP et transparence des sous-traitants.
- Réduction du risque de sauvegarde autonome.
- Data minimization et rollback immédiat.
- Tests indépendants des APIs externes.

## Conséquences positives

- STT et extraction évoluent indépendamment.
- Responsabilités provider/métier testables.
- Mode manuel intact.

## Conséquences négatives / risques

- Deux contrats externes potentiels.
- Latence et coût cumulés.
- Validation DPO et médicale obligatoire.
- Store mémoire non multi-instance.

## Alternatives rejetées

| Alternative | Raison |
|---|---|
| Interface unique STT + chat | viole ISP et masque les fallbacks |
| Web Speech API STT | qualité/compatibilité non maîtrisées |
| Streaming temps réel v1 | complexité non justifiée |
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
- `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`
- Documentation officielle OpenAI Speech-to-text et Data controls (2026-07-17)
