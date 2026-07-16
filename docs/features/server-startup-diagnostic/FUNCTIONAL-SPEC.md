# Diagnostic de démarrage du serveur — Spécification fonctionnelle

Le serveur SSH est joignable depuis MobaXterm sur `161.97.181.177:22`. L'opérateur doit collecter sans modification distante systemd, journaux, ports, santé HTTP et ressources. Le script est non interactif, valide l'empreinte et produit un rapport sans secrets. Redémarrage, modification de configuration et réparation Flyway sont exclus.
