# Cause racine confirmée — sender WebRTC coupé

## Consultation
`RealtimeVoiceControllerComponent.syncMute()` transmet encore au bridge :

```text
manualMuted || backlogPaused || assistantSpeaking
```

`RealtimeVoiceBridgeService.setMuted(true)` appelle ensuite `RTCRtpSender.replaceTrack(null)`.

Conséquence : pendant la voix assistant ou le backlog, aucun audio n'est envoyé au Realtime OpenAI. Les tests #216 injectaient directement des événements `transcript$` et ne simulaient donc pas cette absence d'événements en amont.

## Constantes
Le contrôleur Constantes coupe également le sender pendant `assistantSpeaking`.

## Décision P0
- supprimer toute coupure automatique liée à la voix IA ;
- supprimer toute coupure automatique liée au backlog ;
- désactiver la restitution vocale automatique non critique pendant la capture ;
- conserver uniquement pause explicite et fail-closed durable.
