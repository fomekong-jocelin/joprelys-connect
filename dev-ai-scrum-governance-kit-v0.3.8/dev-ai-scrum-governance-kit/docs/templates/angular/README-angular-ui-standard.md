# Angular UI Standard — Tailwind v4 / DESIGN.md / Theme / i18n / Config

À copier/adaptater dans un projet Angular.

Fichiers fournis :

- `app.config.ts.example` : configuration centrale app/branding.
- `theme.service.ts.example` : gestion light/dark/system avec `data-theme`.
- `tailwind-v4-theme.css.example` : base Tailwind CSS v4 en mode CSS-first avec `@theme`.
- `styles-theme.css.example` : fichier global Angular important Tailwind v4 + thème.
- `i18n-fr.json.example` et `i18n-en.json.example` : traductions minimales.

Règles :

- `DESIGN.md` doit être lu ou créé avant toute UI significative.
- Tailwind CSS v4 uniquement.
- Ne pas utiliser `@tailwind base`, `@tailwind components`, `@tailwind utilities` : syntaxe v3.
- Ne pas utiliser `tailwind.config.js` comme source de tokens applicatifs, sauf ADR.
- Aucun Angular Material sans ADR accepté.
- Aucun texte visible hardcodé.
- Aucun nom d’application ou logo hardcodé dans les composants.
- Toute UI doit utiliser le thème central et des composants réutilisables.

Commande recommandée si `@google/design.md` est disponible :

```bash
npx @google/design.md lint DESIGN.md
npx @google/design.md export DESIGN.md css-tailwind > src/styles/design-system.theme.css
```
