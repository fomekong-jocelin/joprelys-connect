# Design QA — Surface d’écoute vocale unifiée

Date : 2026-07-28
Résultat : **BLOCKED — comparaison pixel à pixel à réaliser sur l’application authentifiée**

## Références

- `docs/ai/mockups/exact_soft_voice_card_1785238037119.jpg`
- `docs/ai/mockups/soft_consultation_dictation_1785236612659.jpg`
- `docs/ai/mockups/soft_vitals_dictation_1785236625256.jpg`

## Contrôles réalisés

- même composant `VoiceListeningSurfaceComponent` dans Consultation/Dictée,
  Consultation/Realtime, Constantes/Dictée et Constantes/Realtime ;
- hiérarchie conforme à la cible : badge IA, arrêt, microphone central avec
  halos, rubans d’ondes, état d’écoute et bandeau conseil séparé ;
- bouton d’arrêt de 44 px minimum, focus visible et libellé accessible ;
- couleurs, rayons, ombres et thèmes issus des tokens centraux ;
- textes visibles injectés depuis l’i18n FR/EN ;
- animation non essentielle désactivée avec `prefers-reduced-motion` ;
- build de l’aperçu réel compilé avec succès.

## Blocage de la comparaison

Le navigateur de contrôle a refusé l’ouverture de l’adresse locale de
l’aperçu. Conformément à sa politique de sécurité, aucune autre surface de
navigateur ni commande indirecte n’a été utilisée pour contourner ce refus.
Il n’a donc pas été possible de capturer l’implémentation et de la juxtaposer à
la maquette dans cette intervention.

## Écarts connus

Aucun écart P0/P1/P2 n’est identifié par la revue statique. Cette conclusion
reste à confirmer visuellement sur une session applicative authentifiée en
desktop et mobile, light et dark.
