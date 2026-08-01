# TICKET: MOB-2816 — Assistant Vocal Clinique (Capture, transcription et corrections)

## Infos
- Mode: Engineering
- Date: 2026-07-30
- Priorité: P0 - Web/Mobile Parity & UX Excellence
- Statut: IN_REVIEW — ID canonique réconcilié et livraison fusionnée par la PR
  `#258` ; dépendance MOB-2815, cible 300, tests runtime et recette micro/appareil
  restent ouverts ; voir TASK-20260801
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
- [x] Réattribution de l'ancien ticket MOB-2815 à MOB-2816, conformément au lot
  canonique capture/transcription/corrections d'EPIC-0028
- [ ] Exécution runtime Flutter et recette appareil exact-HEAD

## Reste à faire
- Ne pas déclarer la reprise audio durable disponible avant livraison de la file
  locale chiffrée canonique MOB-2815 et du contrat segmenté MOB-2814.
- Poursuivre le découpage de l'orchestration (379) et de la surface d'écoute
  (326) vers la cible d'alerte de 300 lignes.
- Valider le rendu final sur appareil physique.
- Obtenir les gates Flutter et APK sur le HEAD exact.
