# Test Plan — AI Realtime Continuous Conversation

## Automatisé

### RealtimeVoiceControllerComponent
- Un transcript normal appelle `sendText()` et émet directement un `AiMessageResponse`.
- Une clarification `PENDING` route le prochain transcript vers `answerClarification()` avec l'ID attendu.
- Le micro est coupé uniquement pendant `processing`, puis réactivé automatiquement après réponse backend.
- Une révision `PENDING` reste bloquante.
- `assistantSpeaking=true` ne force plus le mute.

### RealtimeVoiceBridgeService
- `response.done` avec statut `completed` ne déclenche pas prématurément `assistantTurnCompleted`.
- `output_audio_buffer.stopped` déclenche la fin réelle du tour assistant.
- `output_audio_buffer.cleared` termine proprement un tour interrompu.
- `input_audio_buffer.speech_started` annule une réponse encore en génération.
- Si la génération est terminée mais que le buffer audio joue encore, la reprise de parole envoie `output_audio_buffer.clear`.

### VoiceAssistantPanelComponent
- Un `AiMessageResponse` Realtime met à jour conversation, clarifications, revisions et assistantMessage sans créer de `pendingTranscript`.
- La clarification pending n'entre plus dans le `blocked` transmis au contrôleur Realtime.

## CI
- `npm run test`
- `npm run build`
- lint si script disponible.

## Recette manuelle
1. Activer une seule fois le copilote vocal.
2. Enchaîner au moins cinq tours sans toucher au bouton micro.
3. Faire poser une clarification par Joprelys et y répondre oralement.
4. Pendant une réponse de Joprelys, l'interrompre par une correction ; vérifier coupure audio et prise en compte du nouveau tour.
5. Générer une proposition clinique ; vérifier que le micro se met en pause uniquement pour la validation de la révision.
6. Accepter/rejeter la proposition ; vérifier reprise du mode conversationnel.
7. Tester Chrome desktop et Chrome Android avec haut-parleur actif.

## Critères de sortie
- Aucun clic micro entre deux tours normaux.
- Aucun `PENDING_REVIEW` créé par un transcript Realtime normal.
- Aucune sauvegarde clinique automatique.
- Aucune régression du mode dictée classique.
