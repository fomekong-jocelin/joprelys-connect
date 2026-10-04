# Tests attendus

- Radios : manuel initial ; sélection dictée/écoute continue n'ouvre aucun microphone ; CTA seul active.
- Dictée indisponible expliquée, pending permission sans double ouverture, fermeture avant permission résolue libère les tracks.
- Analyse texte/correction → proposition, aucune mutation parent avant report explicite ; valeurs reportées non sauvegardées avant bouton final.
- Activité de capture/analyse interdit la sauvegarde, puis la rend disponible ; validations existantes conservées.
- Modale : header/footer hors scroll, labels associés, fermeture Échap et confinement du focus, valeurs conservées au changement de mode.
- Tests Angular, contrôle FR/EN et build ; recette visuelle responsive/light/dark/FR/EN et microphone réel reste distincte des mocks.

Résultats automatisés : suite complète 115 fichiers / 633 tests réussis (14 nouveaux cas constantes), contrôle shell 47 clés et contrôle constantes 26 nouvelles clés FR/EN + 2 libellés clarifiés / 24 tokens réussis. Build production final et `git diff --check` réussis. Logs dans `.ai-tmp/vitals-entry-tests-full.log` et `.ai-tmp/vitals-entry-build.log` (ignorés). Recette visuelle ouverte ; ne pas fermer la QA visuelle sur les seuls tests DOM.
