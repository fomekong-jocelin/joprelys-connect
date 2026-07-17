# TKT-AI-TRANSCRIPTION-QUALITY-DIAGNOSTIC — Diagnostic qualité transcription IA consultation

- **Mode** : Diagnostic + Engineering
- **Date** : 2026-07-17
- **Statut** : Correctifs code appliqués ; action env restante côté serveur

## Symptôme

La transcription de la consultation (assistant vocal IA) est de très mauvaise qualité.
Configuration utilisateur : `OPENAI_MODEL=gpt-4o-mini`, `OPENAI_TRANSCRIBE_MODEL=whisper-1`.

## Causes identifiées

1. **`whisper-1` (cause principale)** : c'est l'ancien Whisper large-v2 (2022), nettement
   moins bon en français accentué et vocabulaire médical que les modèles récents
   `gpt-4o-transcribe` / `gpt-4o-mini-transcribe`. Le défaut du projet
   (`application.yml:54` → `gpt-4o-mini-transcribe`) est écrasé par la variable d'env.
2. **Aucun `prompt` de transcription** : `OpenAiProvider.transcribeAudio()`
   (`backend/.../openai/OpenAiProvider.java:49-53`) n'envoie pas le paramètre `prompt`,
   qui guide fortement le vocabulaire (termes médicaux FR, noms de médicaments).
3. **`gpt-4o-mini` pour la synthèse** : si la mauvaise qualité concerne le compte-rendu
   structuré (pas seulement le texte brut), `gpt-4o-mini` est faible en synthèse
   médicale FR ; le défaut projet est `gpt-4.1` (`application.yml:53`).
4. **Capture audio perfectible** : `voice-assistant-panel.component.ts:373-378` —
   `getUserMedia({ audio: true })` sans contraintes (`echoCancellation`,
   `noiseSuppression`, `channelCount: 1`) et `MediaRecorder` sans `audioBitsPerSecond`
   (bitrate opus par défaut bas).
5. **Facteurs terrain** : micro à distance, deux locuteurs (médecin + patient) sans
   diarisation, bruit ambiant.

## Correctifs appliqués

- [x] Ajout d'un `prompt` de transcription (contexte consultation médicale FR) dans
      `OpenAiProvider.transcribeAudio()`, configurable via `OPENAI_TRANSCRIBE_PROMPT`
      (défaut FR médical dans `application.yml`, champ `transcribe-prompt`).
- [x] Capture micro renforcée dans `voice-assistant-panel.component.ts` :
      `channelCount: 1`, `echoCancellation`, `noiseSuppression`, `autoGainControl`,
      `audioBitsPerSecond: 128000`.
- [x] Garde `temperature` dans `OpenAiProvider.chat()` : non envoyée aux modèles à
      raisonnement (`gpt-5*`, `o1/o3/o4`) qui la refusent — débloque
      `gpt-5.6-terra` pour plus tard.
- [x] `SYSTEM_PROMPT` enrichi : interdiction de modifier silencieusement nom,
      dosage, unité, fréquence, durée, voie d'administration d'un médicament ;
      ambiguïté marquée `[À CONFIRMER]`.

## Actions restantes (hors code)

- [x] Sur le serveur : `OPENAI_TRANSCRIBE_MODEL=whisper-1` → `gpt-4o-transcribe`
      (2026-07-17, via `scripts/configure_openai_models.py`).
- [x] Sur le serveur : `OPENAI_MODEL=gpt-4o-mini` → `gpt-4.1` (2026-07-17).
- [ ] Redéployer le backend (nouveau jar) pour activer : prompt de transcription,
      règles ordonnance du `SYSTEM_PROMPT` et garde `temperature`. Sans ce
      redéploiement, `OPENAI_TRANSCRIBE_PROMPT` reste inerte.
- [ ] Option qualité+ après redéploiement : `OPENAI_MODEL=gpt-5.6-terra`
      (accès confirmé sur le compte, HTTP 200 ; nécessite la garde `temperature`).
- [ ] Redéployer le frontend Angular pour activer les contraintes de capture micro.

## Vérifications faites

- Mapping extension MIME OK : `normalizeMimeType()` retire `;codecs=opus`
  (`AiConsultationService.java:329-333`), le switch d'`OpenAiProvider.resolveExtension()`
  fonctionne.
- `language=fr` bien envoyé (`AI_LOCALE:fr`, `application.yml:50`).
- Tests backend module IA : `AiConsultationServiceTest` (4), `RoutingAiProviderTest` (2),
  `AiConsultationControllerTest` (3), `AiConsultationIntegrationTest` (2) — 11/11 verts.
- Build Angular développement (`ng build`) : succès.
- Accès modèles sur le compte OpenAI de production : `gpt-4o-transcribe`,
  `gpt-4o-mini-transcribe`, `gpt-4.1`, `gpt-5.6-terra` → HTTP 200.
- Production : `.env` sauvegardé (`/root/server-fix-backups/20260717-214126/.env`),
  service redémarré, log de démarrage confirmant
  `brouillon=gpt-4.1, transcription=gpt-4o-transcribe`, Tomcat sur 8084.
