# Revue technique — Isolation session patient / professionnel (#169)

## Périmètre

Branche : `fix/auth-patient-professional-isolation-169`

Objectif : garantir qu'une identité PATIENT et une identité professionnelle peuvent coexister dans deux onglets du même navigateur sans invalider ni importer silencieusement leurs sessions respectives.

## Contrôles statiques réalisés

- `PatientAuthController` ne dépend plus du gestionnaire de refresh cookie professionnel.
- La validation OTP patient ne modifie plus le cookie professionnel.
- Le logout PATIENT révoque son JTI mais ne clear pas le cookie professionnel.
- Le logout professionnel conserve le comportement de révocation + clear cookie.
- Le keep-alive ignore les JWT PATIENT.
- L'intercepteur n'appelle jamais `/api/auth/refresh` pour un PATIENT expiré ou rejeté en 401.
- Le service de recovery refuse explicitement tout refresh en contexte patient.
- Le BroadcastChannel de refresh est réservé au contexte professionnel.
- Le bootstrap patient ne restaure pas une identité professionnelle depuis le cookie HttpOnly.
- Les routes patient sans session basculent vers le login patient sans refresh professionnel.
- Le changement d'identité purge `sessionStorage` et les caches mémoire enregistrés, mais ne vide plus le `localStorage` partagé entre onglets.
- Le cache RBAC reste purgé via `registerSessionBoundaryCleanup`.
- Le patient actif reste purgé via `registerSessionBoundaryCleanup`.
- Le JWT patient utilise le DPU comme `sub`; le chemin de logout ne le traite donc pas comme un UUID `users.id` et ne crée pas d'audit staff artificiel.

## Tests ajoutés / renforcés

Frontend :
- keep-alive patient interdit ;
- refresh patient interdit ;
- expiration patient sans fallback professionnel ;
- 401 patient sans fallback professionnel ;
- bootstrap patient sans restauration professionnelle ;
- route patient sans session sans refresh professionnel ;
- session professionnelle non réutilisable comme session patient ;
- nettoyage tab-scoped et conservation du stockage partagé.

Backend :
- login patient sans mutation du cookie professionnel ;
- logout patient sans clear du cookie professionnel ;
- logout professionnel avec clear du cookie ;
- révocation JTI patient sans identité staff.

## CI / recette

GitHub Actions est actuellement susceptible de ne pas démarrer les jobs à cause du budget Actions signalé sur le dépôt. Une absence d'exécution CI ne doit pas être présentée comme un test vert.

Recette navigateur restant à confirmer après déploiement : Chrome desktop et Android, coexistence médecin/patient >30 minutes, refresh médecin, logout patient et coupure réseau.

## Verdict de revue

Le code implémente l'architecture attendue de #169 et supprime les chemins identifiés de déconnexion croisée patient/professionnel. La validation navigateur reste une recette post-déploiement, distincte de la complétude du correctif logiciel.
