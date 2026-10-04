# Vérification du correctif de présentation

- Suite Angular existante : vérifier les interactions consultation et drawer, démarrage/reprise/release/clôture/fermeture, sans test miroir des classes de spacing.
- Build production : compilation des templates et utilitaires Tailwind v4 ; i18n shell et diff sans erreurs.
- Recette visuelle ouverte : 360px et desktop, FR/EN, light/dark, clavier/zoom. Vérifier largeur égale des boutons, texte long lisible, footer accessible et corps défilable ; marges autour du bandeau patient et de la progression, étapes mobiles empilées.
- Backend/Flutter : tests non requis, aucune modification hors présentation Angular/documentation.

Résultats automatiques : 115 fichiers / 633 tests réussis (`npm test -- --watch=false`), build production réussi (`npm run build`), i18n shell 47 clés FR/EN (`npm run i18n:check`), diff sans erreurs. Vérification des utilitaires compilés de colonne/gap/grille/largeur/cible 44px : réussie. Aucun lint script disponible. Logs ignorés dans `.ai-tmp/clinical-layout-tests.log` et `.ai-tmp/clinical-layout-build.log`.

Les captures utilisateur montrent l'anomalie initiale ; elles ne prouvent pas le rendu après correction. Dette de template consultation existante 800 lignes, inchangée : extraction/ADR acceptée requise avant approbation de conformité.
