# FEAT-20260728-UI-MOCKUPS-AI-VOICE-DICTATION.md

## Identité du ticket
- **ID** : `FEAT-20260728-UI-MOCKUPS-AI-VOICE-DICTATION`
- **Titre** : Spécification & Implémentation du Bloc Micro Soft UI avec Flux Scrollable Fixe
- **Mode** : Engineering / UI Design & Architecture
- **Date** : 2026-07-28
- **Auteur** : Antigravity (Google DeepMind Team)
- **Statut** : DONE

## 1. Directives Ergonomiques Stricte (Rappel Utilisateur)

1. **Isolation stricte du bloc micro** :
   - Seul le composant du micro (`soft-voice-card`) est modifié/remplacé.
   - Aucun impact sur la disposition globale de la page, les formulaires ou le reste de l'écran.

2. **Respect des thèmes et de l'i18n** :
   - Intégration directe des variables CSS de `DESIGN.md` (`--app-surface`, `--app-border`, `--brand-primary`, `--text-primary`).
   - Gestion native des thèmes `light` et `dark`.
   - Internationalisation 100% `FR` / `EN`.

3. **Stabilité de mise en page (Anti-Layout Shift / Anti-Déformation)** :
   - Lorsque l'utilisateur dicte, le flux de transcription s'affiche sous le bloc du micro dans un **conteneur scrollable à hauteur bornée** (`max-h-48`, `overflow-y-auto` avec auto-scroll vers le bas).
   - La hauteur globale de la carte et de la page reste rigoureusement fixe et stable (`CLS = 0`). L'écran ne s'allonge ni ne se rétrécit pendant la dictée.

## 2. Actions réalisées

- [x] Spécification technique enregistrée dans `docs/features/ai-voice-consultation/TECHNICAL-DESIGN.md`.
- [x] Maquette Soft UI générée : `soft_consultation_dictation_1785236612659.jpg`.
- [x] Définition des règles d'isolation de bloc micro et du conteneur de transcription scrollable fixe.
- [x] Validation de la conformité aux tokens CSS de `DESIGN.md` et à l'i18n.
- [x] Mise à jour du fichier de suivi `docs/ai/PROJECT-TRACKING.md`.

## 3. Assets & Fichiers

- Maquette Consultation Soft UI : [soft_consultation_dictation_1785236612659.jpg](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/mockups/soft_consultation_dictation_1785236612659.jpg)
- Maquette Constantes Soft UI : [soft_vitals_dictation_1785236625256.jpg](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/mockups/soft_vitals_dictation_1785236625256.jpg)
- Documentation technique : [TECHNICAL-DESIGN.md](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/features/ai-voice-consultation/TECHNICAL-DESIGN.md)

## 4. Prochaines étapes & Validation

- [x] Composant Angular réutilisable (`RealtimeVoiceControllerComponent` & `VoiceWaveVisualizerComponent`) implémenté avec le design Soft UI (`pasted-image-4.png`).
- [x] Clés i18n FR/EN intégrées (`aiInProgress`, `listenNaturally`, `tipDictateNaturally`, `tipVitalsNaturally`).
- [x] Auto-scroll et conteneur fixe `h-32` / `max-h-32` avec `overflow-y-auto` validés pour garantir l'absence totale de saut de mise en page (`CLS = 0`).
- [x] Suite de tests Angular lancée pour vérification.
