# FIX-20260728-UNIFIED-VOICE-LISTENING-SURFACE

## Identité

| Champ | Valeur |
|---|---|
| Type | Engineering UI / correction de cohérence UX |
| Epic | EPIC-0024 — AI Voice Consultation |
| Priorité | P0 |
| Statut | IMPLEMENTED — QA_VISUELLE_MANUELLE_REQUISE |
| Date | 2026-07-28 |
| Sprint | SPRINT-0014 |
| Estimation | 2 SP / 1 à 2 jours senior |
| Profil recommandé | Senior Frontend Angular / UX santé |
| Reviewer | Tech Lead + médecin référent + QA |

## Problème

La carte Soft UI d’écoute est présente en Realtime Consultation, mais les trois
autres parcours utilisent encore des présentations différentes :

- Dictée Consultation : indicateur compact dans `AiAssistantInputComponent` ;
- Realtime Constantes : carte technique dans `RealtimeVitalsControllerComponent` ;
- Dictée Constantes : bouton d’enregistrement sans surface Soft UI.

Le ticket `FEAT-20260728-UI-MOCKUPS-AI-VOICE-DICTATION` a donc été clôturé avant
que son critère d’uniformité visuelle ne soit satisfait dans les quatre parcours.

## Objectif

Fournir une unique surface d’écoute présententionnelle, fidèle à
`docs/ai/mockups/exact_soft_voice_card_1785238037119.jpg`, puis la réutiliser
sans duplication dans :

1. Consultation / Realtime ;
2. Consultation / Dictée ;
3. Constantes / Realtime ;
4. Constantes / Dictée.

## Périmètre inclus

- badge d’activité IA ;
- bouton d’arrêt ;
- microphone central avec halo ;
- visualiseur d’ondes animé ;
- message d’état d’écoute ;
- bandeau conseil contextualisé Consultation ou Constantes ;
- thèmes light/dark, responsive et i18n FR/EN ;
- préservation de l’historique de transcription dans une zone séparée et bornée.

## Hors périmètre

- modification des moteurs Dictée ou WebRTC ;
- modification du backend, des API ou de la base ;
- modification de l’extraction clinique ;
- modification des règles de validation ou de persistance ;
- refonte des formulaires Consultation ou Constantes.

## Critères d’acceptation

- [x] `AC-01` Un seul composant Angular de surface d’écoute est utilisé par les quatre parcours.
- [x] `AC-02` Les quatre parcours affichent la même hiérarchie visuelle et les mêmes dimensions à état équivalent.
- [x] `AC-03` Le bouton d’arrêt déclenche l’action propre au moteur consommateur sans logique métier dans le composant partagé.
- [x] `AC-04` Le texte conseil est contextualisé sans modifier la structure de la carte.
- [x] `AC-05` La transcription existante reste disponible hors de la carte d’écoute, dans une zone fixe scrollable.
- [x] `AC-06` Tous les libellés sont disponibles en français et en anglais.
- [x] `AC-07` Les thèmes light/dark et les largeurs mobile/desktop sont couverts par le design system et les tests de structure.
- [x] `AC-08` Les actions tactiles mesurent au moins 44 px et le focus clavier reste visible.
- [x] `AC-09` Aucun contrat API, flux audio, staging clinique ou sauvegarde n’est modifié.
- [x] `AC-10` Les tests Angular ciblés, i18n et le build production sont verts.
- [ ] `AC-11` La comparaison visuelle avec la maquette ne conserve aucun écart P0/P1/P2.

## Action plan

- [x] Comprendre le comportement actuel.
- [x] Identifier les quatre intégrations et les fichiers impactés.
- [x] Vérifier les risques sécurité, régression, planning et SemVer.
- [x] Mettre à jour la documentation fonctionnelle, technique, design et le plan de test.
- [x] Extraire la surface d’écoute réutilisable.
- [x] Intégrer Consultation / Realtime.
- [x] Intégrer Consultation / Dictée.
- [x] Intégrer Constantes / Realtime.
- [x] Intégrer Constantes / Dictée.
- [x] Ajouter ou adapter les tests.
- [x] Exécuter les gates Angular.
- [ ] Effectuer la recette visuelle et la comparaison avec la maquette.
- [x] Mettre à jour changelog et suivi projet.

## Définition de prêt

- cible visuelle sélectionnée et versionnée ;
- périmètre limité à la surface d’écoute ;
- moteurs vocaux existants identifiés ;
- règles design, i18n, thèmes et accessibilité documentées.

## Définition de fini

- tous les critères d’acceptation cochés ;
- aucune duplication de la surface d’écoute ;
- tests et build documentés ;
- comparaison visuelle passée ;
- suivi projet et changelog à jour ;
- aucune régression fonctionnelle des moteurs vocaux.

## Impact sécurité et OWASP

Changement de présentation uniquement. Aucun nouveau traitement de donnée, HTML
non sûr, stockage, secret, permission ou appel réseau. Les transcripts restent
affichés par interpolation Angular.

## Impact 12-Factor et configuration

Aucune configuration ou variable d’environnement ajoutée. `proxy.conf.json`,
les chemins API relatifs et la configuration YAML restent inchangés.

## Impact version

PATCH proposé : correction rétrocompatible d’une incohérence UX déjà couverte
par la spécification. Aucun breaking change.

## Preuves de vérification

- tests ciblés : 46/46 ;
- suite Angular complète : 507/507 ;
- contrôle i18n : 49 clés shell présentes en français et en anglais ;
- build Angular production : succès, bundle initial 542,11 kB ;
- comparaison visuelle : tentative documentée dans `design-qa.md`, capture
  locale bloquée par la politique de sécurité du navigateur de contrôle ;
- avertissement restant non introduit : import Angular
  `AmbientSafetyPanelComponent` inutilisé dans `VoiceAssistantPanelComponent`.

## Reste à faire

Recette visuelle manuelle sur une session applicative authentifiée :
Consultation et Constantes, Dictée et Realtime, desktop/mobile, light/dark.
Confirmer ensuite `AC-11` et faire signer le médecin référent et la QA.
