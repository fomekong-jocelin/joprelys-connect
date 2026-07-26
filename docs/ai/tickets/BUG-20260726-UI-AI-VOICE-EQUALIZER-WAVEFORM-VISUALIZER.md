# BUG-20260726-UI-AI-VOICE-EQUALIZER-WAVEFORM-VISUALIZER

## Qualification

- **Mode** : Engineering + UI/UX Design + QA Review
- **Epic** : EPIC-0024 / Consultation IA Vocal
- **Priorité** : P0 — correction UX / retour visuel écoute vocale
- **Statut** : DONE — fusionné dans main@df00a904
- **Stack** : Angular 19 / Tailwind CSS v4 / Visualiseur audio / i18n
- **Estimation** : 0.5j senior frontend
- **Reviewer** : Lead Frontend + QA
- **Impact SemVer prévu** : MINOR (amélioration IHM & égaliseur dynamique)

## Incident / Retours Utilisateur

L'utilisateur signale sur mobile / desktop :
1. "rien ne marche, je parle dans le vide" : manque de feedback visuel réactif à la voix lorsque le micro écoute.
2. "normalement quand il écoute je dois avoir des vagues qui réagissent au son de la voix" : l'interface affichait uniquement un texte statique et une puce rouge/verte de 10px sans égaliseur sonore.
3. Décalage de version sur `recette.joprelys.com` qui nécessitait le déploiement des PRs #198 (flux Realtime simplifié) et #196 (fait clinique).

## Actions

- [x] Analyser l'absence de visualiseur d'ondes sonores dynamique dans `realtime-voice-controller` et `ai-assistant-input`.
- [x] Concevoir un égaliseur d'ondes sonores animé (`16 bars`) réagissant en temps réel à l'état de parole (`userSpeaking`, `assistantSpeaking`, `listening`) et au volume micro (`audioLevel`).
- [x] Ajouter les badges dynamiques de parole ("Vous parlez" / "Joprelys répond") avec animations de pulsation.
- [x] Mettre à jour les traductions FR et EN (`consultation.ai.userSpeakingBadge`, `consultation.ai.assistantSpeakingBadge`, `consultation.ai.recordingActive`).
- [x] Exécuter la suite complète de 463 tests unitaires Angular.
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## Critères d'acceptation

- [x] L'utilisateur visualise des vagues d'égaliseur animées en mode Temps Réel et en mode Dictée dès que le micro écoute.
- [x] Les vagues d'ondes changent dynamiquement de couleur et d'amplitude selon le locuteur (Vert/Emeraude quand le médecin parle, Indigo/Violet quand l'assistant répond, Cyan quand l'écoute est en attente).
- [x] Les tests Angular s'exécutent avec 0 échec et 0 régression.
- [x] Les arrondis respectent les standards UI (6px pour le visualiseur).

## Reste à faire

1. Redéployer `main` sur le serveur de recette (`recette.joprelys.com`).
2. Réaliser le test réel avec le téléphone mobile sur `recette.joprelys.com`.
