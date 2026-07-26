# Technical Design — AI Realtime Continuous Conversation

## Stack
Angular + RxJS + WebRTC/OpenAI Realtime. Aucun changement DB, Flyway ou contrat Spring public.

## Architecture actuelle
`RealtimeVoiceBridgeService` maintient WebRTC et émet les transcriptions. Le contrôleur déposait chaque transcript dans `stageRealtimeTranscript()`, créant `pendingTranscript=PENDING_REVIEW`; le parent passait alors `blocked=true` et coupait la piste micro.

## Architecture cible

```text
RealtimeVoiceBridgeService
  └─ transcript$
      └─ RealtimeVoiceControllerComponent
          ├─ clarification pending ? answerClarification(...)
          └─ sinon sendText(...)
                ↓
          pipeline clinique backend existant
                ↓
          AiMessageResponse
                ↓
          VoiceAssistantPanelComponent actualise la session
                ↓
          vocalisation assistantMessage/question sur la même connexion Realtime
```

Le frontend orchestre uniquement le choix de l'endpoint en fonction de l'état de session. Les règles et garde-fous cliniques restent côté backend.

## Gestion du microphone
- `manualMuted`, `blocked` et `processing` peuvent couper temporairement la piste.
- `assistantSpeaking` ne coupe plus le micro afin de permettre le barge-in.
- `processing` coupe brièvement la piste pour éviter plusieurs analyses concurrentes sur un même état de session.
- Après réponse backend, la piste est automatiquement réactivée sauf si une révision exige une décision.

## Clarifications
Une clarification `PENDING` n'est plus un blocage Realtime. Le prochain transcript appelle `answerClarification(visitId, clarification.id, transcript)`.

## Révisions cliniques
Une révision `PENDING` reste bloquante. Le professionnel doit accepter/rejeter explicitement dans l'UI.

## Lifecycle audio Realtime
Selon l'API Realtime officielle :
- `input_audio_buffer.speech_started` signale une nouvelle prise de parole ;
- `response.done` signifie que la génération est terminée mais l'audio WebRTC peut encore jouer ;
- `output_audio_buffer.stopped` signifie que le buffer audio est réellement vidé ;
- `output_audio_buffer.cleared` signale une interruption/vidage du buffer.

Le bridge attend donc `output_audio_buffer.stopped/cleared` pour terminer le tour audio. Si l'utilisateur reprend la parole après `response.done` mais avant la vidange du buffer, le client envoie `output_audio_buffer.clear`.

## Fichiers impactés
- `web/src/app/consultation/realtime-voice-controller.component.ts`
- `web/src/app/consultation/realtime-voice-bridge.service.ts`
- `web/src/app/consultation/voice-assistant-panel.component.html`
- `web/src/app/consultation/voice-assistant-panel.component.ts`
- tests associés.

## Sécurité
- Aucun nouveau secret ni endpoint.
- Aucune donnée clinique persistée automatiquement.
- Les propositions restent soumises aux gardes backend et à validation explicite.
- La dictée classique conserve la relecture.

## SemVer
PATCH — correction rétrocompatible d'un comportement Realtime existant.
