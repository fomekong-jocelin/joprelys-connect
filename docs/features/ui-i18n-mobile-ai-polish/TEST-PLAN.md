# Plan de test — Finitions UI i18n, thème et mobile IA

## Automatisé

- Login : rendu du contenu de vitrine via clés i18n.
- Login : logo `on-dark` sans classe d'inversion.
- Assistant de constantes : état initial mobile replié et desktop ouvert.
- Assistant de constantes : le repli coupe Realtime et le micro.
- Dashboard : assistant déclaré dans la modale et proposition relayée.
- Composants IA : libellés de champs et aides obtenus depuis i18n.
- Contrôle de parité i18n FR/EN.
- Build Angular production.
- Inspection IntelliJ des fichiers modifiés.

## Recette manuelle

- Viewports : 320×568, 390×844, 768×1024, 1366×768 et 1920×1080.
- Variantes : FR/EN, light/dark, Personnel/Patient.
- Vérifier focus clavier, contraste, absence de débordement et actions tactiles.
- Dans les constantes, vérifier repli/ouverture, activation explicite du micro, préremplissage et enregistrement manuel.
- Dans la consultation IA, vérifier clarification, proposition, acceptation/rejet et brouillon.

## Non-régression métier

- OTP professionnel et patient inchangés.
- Permissions `VISIT_VITALS_WRITE` inchangées.
- Contrats API et payload de constantes inchangés.
- Aucune prescription ni constante sauvegardée automatiquement.

## Résultats du 2026-07-26

- Suite Angular complète : 81 fichiers réussis, 388 tests réussis.
- Contrôle i18n shell : 49 clés FR/EN présentes.
- Build Angular production : succès, bundle initial 527,82 kB.
- Inspections IntelliJ des sources modifiées : aucune erreur.
- Recette visuelle authentifiée : à exécuter sur les viewports et variantes listés ci-dessus.
