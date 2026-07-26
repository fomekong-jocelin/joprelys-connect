# Spécification fonctionnelle — Finitions UI i18n, thème et mobile IA

## Objectif

Rendre les écrans de connexion et d'assistance clinique cohérents en FR/EN, lisibles en light/dark et utilisables en priorité sur mobile, sans modifier les règles métier ni les parcours d'authentification.

## Parcours concernés

1. Connexion Personnel et Patient.
2. Saisie des constantes depuis le tableau de bord.
3. Assistant IA de constantes.
4. Relecture, clarification et validation des propositions IA.

## Règles fonctionnelles

- Le panneau de marque affiche une promesse, trois bénéfices et un slogan traduits par le service i18n.
- Le logo choisi dépend du contexte de fond, pas du thème global.
- Sur un petit écran, l'assistant de constantes démarre replié.
- L'ouverture affiche d'abord l'action principale ; les résultats et validations apparaissent uniquement lorsqu'ils existent.
- Toute proposition IA reste un préremplissage. Le professionnel conserve l'action explicite d'enregistrement.
- Les clarifications, catégories cliniques, aides et attributs accessibles sont disponibles en français et en anglais.

## Critères d'acceptation

- Contraste suffisant du panneau de marque dans les deux thèmes.
- Aucun pseudo-contenu CSS localisé.
- Aucun libellé français résiduel dans les composants IA modifiés en mode anglais.
- Cibles tactiles d'au moins 44 px pour les actions principales.
- Aucun débordement horizontal à 320 px.
- Aucun changement de contrat API, d'autorisation ou de persistance clinique.
