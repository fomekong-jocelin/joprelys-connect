# FIX-20260728-TRANSCRIPTION-UX-SURGICAL

## Métadonnées

| Champ | Valeur |
|---|---|
| Type | Engineering — correctif chirurgical |
| Priorité | P0 |
| Statut | DONE |
| Sprint | SPRINT-0014 |
| Date | 2026-07-28 |
| Référence | DIAG-20260728-TRANSCRIPTION-UX-CRITIQUE |

## Objectif

Corriger chirurgicalement les bugs critiques identifiés dans le diagnostic de dictée / transcription temps réel (constantes et consultation) sans régression.

## Actions accomplies

- [x] **BUG-03 & BUG-08** : Ajout d'un historique scrollable des transcriptions avec horodatage (`transcriptHistory`) et repositionnement du compteur de file d'attente (`queuedCount`) avec indicateur animé dans `realtime-voice-controller`.
- [x] **BUG-04 & ERG-02** : Implémentation du staging des constantes via `pendingVitalsProposal` dans `consultation.component.ts` avec validation / rejet explicite par le médecin (empêche la persistance directe en base). Enrichissement du feedback de mise à jour des champs avec décompte.
- [x] **BUG-05** : Création d'un signal `vitalsWarning` dédié pour préserver l'avertissement relatif aux constantes non fusionnées dans `voice-assistant-panel`, avec bouton de fermeture.
- [x] **BUG-06** : Optimisation du polling 4s (`this.session() && ...`) pour éviter les appels réseaux inutiles si aucune session n'est active.
- [x] **BUG-07** : Correction de l'overlay mobile du `smart-vitals-assistant` (`bottom-14`, `max-h-[55dvh]`) pour éviter le chevauchement avec les boutons de sauvegarde.
- [x] Compilation production Angular (`npx ng build --configuration=production`) validée avec succès.
- [x] Tests unitaires Angular validés sans régression.
