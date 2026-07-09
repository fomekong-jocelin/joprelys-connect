# BUG-20260709-frontend-icon-startup-fix

## Type

Bug / Frontend Angular

## Contexte

Le frontend Angular ne démarre plus à cause d'un problème lié aux icônes. Le symptôme attendu est une erreur de compilation ou de rendu bloquante autour du composant d'icône partagé.

## Objectif

Rétablir le démarrage du frontend avec une correction minimale, sans introduire Angular Material, Tailwind v3, URL backend hardcodée ou changement de contrat API.

## Critères d'acceptation

- [x] L'erreur de démarrage liée aux icônes est identifiée.
- [x] Le composant ou les usages d'icônes invalides sont corrigés.
- [x] `npm run build` passe dans `web`.
- [x] Le proxy Angular reste versionné et référencé dans `angular.json`.
- [x] Aucun changement de comportement API, DB ou sécurité n'est introduit.

## Action plan

- [x] Lire les consignes IA, standards Angular/UI/configuration et SemVer.
- [x] Créer le ticket et la documentation minimale.
- [x] Reproduire l'erreur de démarrage/build Angular.
- [x] Identifier les fichiers impactés.
- [x] Appliquer une correction minimale.
- [x] Exécuter les vérifications Angular.
- [x] Mettre à jour le changelog et le suivi global.

## Diagnostic

- `npm run build` échouait sur `BillingEstimatesComponent` car le template utilisait l'ancien sélecteur `<app-icon>` alors que le composant partagé disponible est `app-ui-icon`.
- Après correction des icônes, le build de production échouait encore sur l'inlining Google Fonts, car cette optimisation Angular nécessite Internet et l'environnement local retourne `connect EACCES`.
- `ng serve --host 127.0.0.1 --port 4200` compile correctement et annonce l'URL locale `http://127.0.0.1:4200/`; le processus reste ensuite en watch mode.

## Impacts

- Stack : Angular frontend.
- API : aucun impact attendu.
- Base de données : aucun impact attendu.
- Sécurité : aucun affaiblissement attendu.
- Configuration : désactivation ciblée de `optimization.fonts.inline` en production pour rendre le build indépendant du réseau.

## Tests attendus

- [x] `npm run build` : OK.
- [x] `npm run build -- --configuration development` : OK.
- [x] `npm run test -- --watch=false` : OK, 18 fichiers / 101 tests.
- [x] `npm start -- --host 127.0.0.1 --port 4200` : compilation OK, URL locale annoncée ; arrêt par timeout de l'outil car watch mode.

## Reste à faire

- Relancer `npm start -- --host 127.0.0.1 --port 4200` manuellement pour conserver le serveur actif, car le sandbox a refusé le lancement en arrière-plan via `Start-Process`.

## Impact version / SemVer

- Version actuelle : non modifiée.
- Bump applicatif recommandé : PATCH.
- Justification : correction de bug frontend rétrocompatible et configuration de build production offline-safe.
- Breaking changes : non.
