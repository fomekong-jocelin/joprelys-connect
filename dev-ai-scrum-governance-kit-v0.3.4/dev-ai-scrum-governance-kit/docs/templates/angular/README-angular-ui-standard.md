# Angular UI Standard — Tailwind / Theme / i18n / Config

À copier/adaptater dans un projet Angular.

Fichiers fournis :

- `app.config.ts.example` : configuration centrale app/branding.
- `theme.service.ts.example` : gestion light/dark/system.
- `tailwind.config.js.example` : base Tailwind avec tokens CSS.
- `styles-theme.css.example` : variables CSS light/dark.
- `i18n-fr.json.example` et `i18n-en.json.example` : traductions minimales.

Règles :

- Aucun Angular Material sans ADR accepté.
- Aucun texte visible hardcodé.
- Aucun nom d’application ou logo hardcodé dans les composants.
- Toute UI doit utiliser le thème central et des composants réutilisables.
