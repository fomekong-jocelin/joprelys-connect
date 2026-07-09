# Correction technique du démarrage frontend lié aux icônes

## Stack concernée

- Angular 22.
- TypeScript strict.
- Tailwind CSS v4 CSS-first.
- Design system interne via `DESIGN.md`.

## Approche

1. Reproduire l'échec avec `npm run build`.
2. Identifier si l'échec vient :
   - du sélecteur Angular utilisé pour le composant d'icône ;
   - d'un nom d'icône absent du type `UiIconName` ;
   - d'un template non aligné avec le composant partagé ;
   - d'une erreur de compilation TypeScript.
3. Corriger au plus proche de la source du problème.
4. Valider par build Angular.

## Fichiers probablement impactés

- `web/src/app/shared/ui/icon.component.ts`
- Templates ou composants Angular utilisant le composant d'icône.

## Architecture et responsabilités

Le composant d'icône reste un composant UI partagé. La correction ne doit pas déplacer de logique métier dans le frontend et ne doit pas introduire de dépendance externe.

## Sécurité

Aucun contenu utilisateur ou HTML dynamique n'est introduit. Aucun impact AuthN/AuthZ, stockage de token ou XSS attendu.

## Configuration

`proxy.conf.json` doit rester présent et référencé par `angular.json`.

Le build de production Angular ne doit pas dépendre d'un accès Internet au moment de compiler. L'optimisation `fonts.inline` est désactivée pour éviter l'échec de build quand Google Fonts est inaccessible depuis l'environnement CI/local restreint, tout en conservant les optimisations scripts, styles et CSS critique.

## Tests

- `npm run build` : OK.
- `npm run build -- --configuration development` : OK.
- `npm run test -- --watch=false` : OK, 18 fichiers / 101 tests.
- `npm start -- --host 127.0.0.1 --port 4200` : compilation OK, URL locale annoncée ; la commande reste en watch mode.

## Impact SemVer prévu

PATCH : correction de bug frontend rétrocompatible.
