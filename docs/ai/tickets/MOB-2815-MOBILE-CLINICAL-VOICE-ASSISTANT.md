# TICKET: MOB-2815 — Assistant Vocal Clinique (Alignement Web Angular)

## Infos
- Mode: Engineering
- Date: 2026-07-30
- Priorité: P1 - Web/Mobile Parity & UX Excellence
- Statut: IN_REVIEW — widget sous 500 lignes ; ID, cible 300, tests runtime et
  recette micro/appareil restent ouverts ; voir TASK-20260801
- Target Release: candidat MINOR `0.11.0` ; aucune release préparée

## Description
Portage et alignement rigoureux du composant d'écoute vocale Angular Web (`VoiceListeningSurfaceComponent` / `VoiceWaveVisualizerComponent`) vers l'application mobile Flutter :
- Header d'écoute `[✨ IA en cours...]` avec bouton `[⏹ Arrêter]`
- Surface d'ondes sinusoïdales animées via `CustomPainter` (`_SineWavePainter`)
- Halos concentriques de résonance (`voice-halo-outer`, `voice-halo-middle`, `voice-halo-inner`) avec opacités et pulsations synchronisées
- Orbe centrale de microphone avec dégradé radial et ombre portées
- Conseil d'utilisation et synthèse d'extraction des constantes en direct

## Check-list d'intervention
- [x] Inspection du code source Angular Web (`voice-listening-surface.component.ts` et `voice-wave-visualizer.component.ts`)
- [x] Implémentation du peintre de courbes sinusoïdales Canvas `_SineWavePainter`
- [x] Réplication exacte des triple halos d'ondes et du micro orb central
- [x] Intégration du bandeau d'aide et puces de dictée rapide
- [x] Validation des 66 tests automatisés Flutter et goldens
- [x] Documentation et traçabilité projet à jour
- [x] Découpage en orchestration (379 lignes), surface d'écoute (326 lignes) et
  widgets de transcription (152 lignes)
- [x] Analyse Dart ciblée sans diagnostic
- [ ] Exécution runtime Flutter et recette appareil exact-HEAD

## Reste à faire
- Réconcilier l'ID avec la file audio chiffrée MOB-2815 prévue par EPIC-0028.
- Poursuivre le découpage de l'orchestration (379) et de la surface d'écoute
  (326) vers la cible d'alerte de 300 lignes.
- Valider le rendu final sur appareil physique.
- Obtenir les gates Flutter et APK sur le HEAD exact.
