# FUNCTIONAL-SPEC — Cloisonnement permission-first de tous les rôles

## Besoin

Un utilisateur ne doit voir, ouvrir ou exécuter que les actions correspondant à ses permissions effectives, indépendamment d'un ancien compte utilisé dans le navigateur ou du nom de son rôle.

## Règles fonctionnelles

- RF-01 : les permissions effectives sont recalculées pour la session courante et ne survivent jamais à un changement de jeton.
- RF-02 : une permission déclarée sur une action est obligatoire ; le nom du rôle ne la remplace pas.
- RF-03 : le menu, les routes et l'API appliquent la même matrice.
- RF-04 : l'API refuse par défaut et reste l'autorité finale.
- RF-05 : les contrôles de permission ne remplacent pas l'isolation tenant, propriétaire ou patient.
- RF-06 : toute session contenant `PATIENT` est traitée comme patient-only jusqu'à correction de la donnée source.
- RF-07 : les rôles personnalisés sont supportés uniquement par leurs permissions.
- RF-08 : une déconnexion efface toutes les données du stockage navigateur, les cookies de l'origine et les états mémoire sensibles ; les préférences thème/langue sont volontairement réinitialisées.
- RF-09 : une connexion avec une autre identité purge l'état précédent avant d'enregistrer la nouvelle session.

## Parcours de recette

Pour chaque rôle système et personnalisé : connexion, contrôle du menu, tentative d'URL directe, appels API autorisés, appels API interdits, déconnexion, connexion avec un autre rôle et répétition sans rechargement complet.

## Périmètre de la généralisation

La généralisation couvre tous les contrôleurs contenant encore un contrôle de rôle ou un
fallback rôle, ainsi que toutes les routes et entrées de navigation professionnelles. La
matrice canonique est définie dans `AUTHORIZATION-MATRIX.md`.
