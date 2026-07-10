# FUNCTIONAL-SPEC — Cohérence visuelle des informations médicales patient

## Problème

Les sections médicales du profil patient utilisent des emojis dépendants du système d'exploitation et certaines traductions Urgences sont absentes. Le résultat ne correspond pas au langage visuel SVG du reste de Joprelys Connect.

## Utilisateurs

- Médecins, infirmiers et administrateurs de clinique consultant le DPU.

## Parcours attendu

1. Ouvrir la fiche Profil d'un patient.
2. Identifier Allergies, Antécédents, Vaccinations et Urgences grâce aux icônes SVG homogènes de l'application.
3. Lire tous les libellés dans la langue active sans voir de clé technique.

## Critères d'acceptation

- Aucun emoji décoratif dans les titres, statuts et sous-sections du périmètre médical.
- Les icônes utilisent le composant partagé `app-ui-icon` et les couleurs sémantiques du thème.
- Les libellés sont disponibles en français et en anglais.
- Les thèmes light/dark, le contraste et les arrondis sobres sont conservés.

## Diagnostic consultation

Une visite sans consultation sauvegardée est un état initial valide. L'API existante signale cette absence par `404`; l'écran de saisie conserve alors un formulaire vierge. Ce ticket n'altère pas ce contrat.
