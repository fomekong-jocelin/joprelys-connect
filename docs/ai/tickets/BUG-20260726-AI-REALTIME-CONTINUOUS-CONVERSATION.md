# BUG-20260726-AI-REALTIME-CONTINUOUS-CONVERSATION

## Type
Bug P0 — UX clinique / Realtime vocal / régression conversationnelle

## Contexte
Le handshake WebRTC Realtime fonctionne, mais la consultation n'était pas réellement interactive : après un premier tour de parole, la transcription Realtime était déposée en `PENDING_REVIEW`, le contrôleur passait en état bloqué et coupait le microphone. Le professionnel devait relancer ou valider manuellement la transcription avant de poursuivre.

Le hotfix de sécurité précédent avait imposé une relecture à toutes les transcriptions audio. Cette règle reste pertinente pour la dictée classique, mais elle cassait le mode WebRTC conversationnel.

## Objectif
Obtenir le cycle continu :

`médecin parle → analyse Joprelys → réponse vocale → médecin répond/interrompt → analyse Joprelys → ...`

sans redémarrage manuel du microphone entre les tours.

## Règles métier / sécurité
- Le backend reste la source de vérité clinique.
- Une transcription Realtime est analysée immédiatement comme tour conversationnel, mais aucune proposition clinique n'est automatiquement acceptée.
- Les propositions/révisions cliniques restent soumises à validation explicite.
- Une révision clinique en attente continue de mettre la conversation en pause.
- Une clarification en attente ne coupe plus le micro : la phrase suivante répond automatiquement à cette clarification.
- La dictée classique conserve son workflow `PENDING_REVIEW`.
- Le microphone Realtime reste actif pendant la réponse vocale Joprelys pour permettre le barge-in.

## Critères d'acceptation
- [x] Une transcription Realtime sans clarification appelle directement `sendText`.
- [x] Une transcription Realtime avec clarification pending appelle automatiquement `answerClarification`.
- [x] Le mode Realtime ne crée plus de `pendingTranscript` à chaque tour normal.
- [x] Une clarification pending ne bloque plus le contrôleur Realtime.
- [x] Le microphone n'est pas désactivé simplement parce que Joprelys parle.
- [x] `response.done` ne marque plus prématurément la fin audio ; `output_audio_buffer.stopped/cleared` termine le tour.
- [x] Une révision clinique pending reste bloquante.
- [x] La dictée classique conserve la relecture manuelle.
- [x] Tests Angular et build production verts sur gate #1451.
- [ ] Recette réelle de 5 tours successifs sans clic micro.
- [ ] Recette barge-in sur Chrome desktop et Android.

## Action plan
- [x] Comprendre le comportement actuel.
- [x] Identifier `stageRealtimeTranscript → pendingTranscript → blocked`.
- [x] Vérifier les événements Realtime officiels OpenAI.
- [x] Router les transcripts Realtime vers l'analyse clinique existante.
- [x] Router automatiquement les clarifications.
- [x] Retirer la clarification du critère de blocage Realtime.
- [x] Autoriser le barge-in pendant la parole assistant.
- [x] Corriger le lifecycle `response.done` / `output_audio_buffer.stopped`.
- [x] Ajouter les tests de non-régression.
- [x] Exécuter CI frontend : tests Angular + build production verts sur #1451.
- [x] Mettre à jour changelog et suivi global.

## Estimation / profil
- Senior : 0,5–1 j.
- Profil : senior Angular/WebRTC + QA clinique.
- Reviewer : Tech Lead + QA clinique.

## Risques
- Echo haut-parleur → micro : `echoCancellation` reste activé, recette matérielle obligatoire.
- Pendant la courte analyse backend, le micro est temporairement suspendu pour éviter les requêtes concurrentes ; il se réactive automatiquement dès la réponse.
- Les décisions cliniques restent volontairement bloquantes.
