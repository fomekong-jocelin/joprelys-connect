# TASK-20260729-ENV-RECETTE-AI-MODELS-CHECK — Verification et Alignement des Variables d'Environnement IA Recette

## Metadata
- **ID** : TASK-20260729-ENV-RECETTE-AI-MODELS-CHECK
- **Epic** : EPIC-0024 / AI_VOICE_CONSULTATION
- **Type** : Task / Audit Configuration & Environnement
- **Statut** : DONE
- **Priorité** : P0
- **Auteur** : Antigravity
- **Date** : 2026-07-29

## Objectif
Vérifier et aligner la configuration des variables d'environnement OpenAI (`.env` recette et `application.yml`) pour prendre en compte l'ensemble des nouveaux modèles et clés configurables de l'assistant vocal.

## Matrice des Variables IA (Appliquée et Validée sur Recette `161.97.181.177`)

| Propriété YAML Spring | Variable d'environnement `.env` | Valeur Recette Appliquée | Statut `.env` Serveur |
|---|---|---|---|
| `joprelys.ai.enabled` | `JOPRELYS_AI_ENABLED` | `true` | Appliqué |
| `joprelys.ai.provider` | `AI_PROVIDER` | `openai` | Appliqué |
| `joprelys.ai.speech-provider` | `SPEECH_PROVIDER` | `openai` | Appliqué |
| `joprelys.ai.openai.api-key` | `OPENAI_API_KEY` | `sk-...` | Appliqué (masqué) |
| `joprelys.ai.openai.model` | `OPENAI_MODEL` | `gpt-4o-mini` | Appliqué |
| `joprelys.ai.openai.transcribe-model` | `OPENAI_TRANSCRIBE_MODEL` | `gpt-4o-mini-transcribe` | Appliqué |
| `joprelys.ai.openai.ambient-transcribe-model` | `OPENAI_AMBIENT_TRANSCRIBE_MODEL` | `gpt-4o-transcribe-diarize` | Conservé (sécurité) |
| `joprelys.ai.openai.tts-model` | `OPENAI_TTS_MODEL` | `tts-1` | Appliqué |
| `joprelys.ai.openai.tts-voice` | `OPENAI_TTS_VOICE` | `marin` | Appliqué |
| `joprelys.ai.openai.final-review-enabled` | `OPENAI_FINAL_REVIEW_ENABLED` | `true` | Appliqué |
| `joprelys.ai.openai.final-review-model` | `OPENAI_FINAL_REVIEW_MODEL` | `gpt-5.6-terra` | Appliqué |
| `joprelys.ai.openai.realtime-model` | `OPENAI_REALTIME_MODEL` | `gpt-realtime-2.1-mini` | Appliqué |
| `joprelys.ai.openai.realtime-fallback-model` | `OPENAI_REALTIME_FALLBACK_MODEL` | `gpt-realtime-2.1-mini` | Appliqué |
| `joprelys.ai.openai.realtime-voice` | `OPENAI_REALTIME_VOICE` | `marin` | Appliqué |
| `joprelys.ai.openai.realtime-transcribe-model` | `OPENAI_REALTIME_TRANSCRIBE_MODEL` | `gpt-4o-mini-transcribe` | Appliqué |

## Plan d'action
- [x] Connexion SSH au serveur `161.97.181.177` (utilisateur `jocelin` + `sudo`)
- [x] Sauvegarde horodatée de l'ancien `.env` dans `/root/server-fix-backups/20260729-014311/.env`
- [x] Injection et mise à jour de l'ensemble des clés OpenAI dans `/opt/joprelys-connect/api/.env`
- [x] Redémarrage réussi du service `joprelys-connect-api.service`
- [x] Vérification du port HTTP `8084` et confirmation des logs applicatifs : `Configuration OpenAI chargée : brouillon=gpt-4o-mini, transcription=gpt-4o-mini-transcribe`

## Résultats & Vérifications
- Serveur de recette `161.97.181.177` 100% configuré et actif sur le port `8084`.
- Logs applicatifs confirmant le démarrage sans erreur de `JoprelysBackendApplication`.
