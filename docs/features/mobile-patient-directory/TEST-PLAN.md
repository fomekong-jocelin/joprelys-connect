# Plan de test — Annuaire patient mobile (MOB-2809)

## Automatisé

- mapper une liste de patients valide ;
- encoder correctement une recherche avec espaces et caractères spéciaux ;
- propager les erreurs API sans afficher d'exception brute ;
- conserver le dashboard sans overflow sur une largeur Android compacte ;
- préserver l'accès à la file active et au dossier patient.

## Recette

- FR et EN ;
- thèmes light et dark ;
- résultat, aucun résultat, erreur réseau et accès refusé ;
- recherche par nom, DPU, téléphone et numéro temporaire ;
- ouverture du bon patient dans MOB-2810 ;
- vérification qu'aucune donnée patient ou recherche n'apparaît dans les logs.

## Gate

Le ticket ne passe DONE qu'après `flutter analyze`, `flutter test`, build APK et
recette backend/appareil sur le même HEAD.
