# FRONTEND & MOBILE STANDARDS — Angular / Flutter

> Objectif : garantir des interfaces cohérentes, maintenables, personnalisables, internationalisées et prêtes pour une vraie exploitation produit.

## 1. Règles non négociables

Toute fonctionnalité Angular ou Flutter doit respecter :

- `DESIGN.md` comme source de vérité design pour toute UI significative ;
- thème centralisé ;
- deux thèmes minimum : `light` et `dark` ;
- composants/widgets réutilisables ;
- internationalisation minimum : français (`fr`) et anglais (`en`) ;
- configuration applicative centralisée : nom de l’app, logo, slogan, éditeur, paramètres publics, liens, thème par défaut ;
- aucun texte visible codé en dur ;
- aucun branding codé en dur dans un écran.

## 2. Angular — Standard obligatoire

### 2.1 Structure recommandée

```text
src/
  app/
    core/
      config/
        app.config.ts
      theme/
        theme.service.ts
        theme.tokens.ts
      i18n/
        i18n.service.ts
    shared/
      ui/
        button/
        input/
        modal/
        card/
        empty-state/
        page-header/
        loading-state/
    features/
  assets/
    branding/
      logo.svg
      logo-dark.svg
    i18n/
      fr.json
      en.json
```

### 2.2 Tailwind CSS v4 et DESIGN.md

- Tailwind CSS v4 est obligatoire.
- L’approche Tailwind doit être CSS-first avec `@import "tailwindcss"` et `@theme`.
- La syntaxe Tailwind v3 `@tailwind base/components/utilities` est interdite.
- `tailwind.config.js` ne doit pas être utilisé comme source des tokens applicatifs, sauf ADR acceptée.
- Angular Material est interdit sauf ADR accepté.
- Les tokens Tailwind et CSS variables doivent être alignés avec `DESIGN.md`.
- Éviter les classes arbitraires répétées si elles représentent un composant réutilisable.
- Si `@google/design.md` est disponible, générer le thème Tailwind v4 avec `npx @google/design.md export DESIGN.md css-tailwind`.

### 2.3 Thème

- Le thème doit être géré par un service central.
- Le thème doit pouvoir être `light`, `dark` ou `system` si le projet le prévoit.
- Le choix du thème doit pouvoir être persisté si l’application a une zone utilisateur.
- Les composants ne doivent pas implémenter leur propre logique dark/light isolée.

### 2.4 Internationalisation

- Minimum obligatoire : `fr` et `en`.
- Les chaînes visibles doivent être dans `assets/i18n/fr.json` et `assets/i18n/en.json` ou système équivalent validé.
- Aucune phrase utilisateur ne doit rester hardcodée dans les templates Angular.
- Les clés de traduction doivent être nommées par domaine fonctionnel.

Exemple :

```json
{
  "common": {
    "save": "Enregistrer",
    "cancel": "Annuler"
  },
  "auth": {
    "loginTitle": "Connexion"
  }
}
```

### 2.5 Configuration applicative

- `appName`, `logoPath`, `supportedLocales`, `defaultTheme`, `publisherName` et liens publics doivent venir de `app.config.ts` ou d’un service équivalent.
- Ne jamais dupliquer le nom ou le logo de l’application dans plusieurs composants.

## 3. Flutter — Standard obligatoire

### 3.1 Structure recommandée

```text
lib/
  core/
    config/
      app_config.dart
    theme/
      app_theme.dart
      app_theme_tokens.dart
      theme_controller.dart
    l10n/
      localization_extensions.dart
  shared/
    widgets/
      app_button.dart
      app_text_field.dart
      app_card.dart
      app_empty_state.dart
      app_page_header.dart
  features/
  l10n/
    app_fr.arb
    app_en.arb
l10n.yaml
```

### 3.2 Thème

- Prévoir `ThemeData` light et dark ou équivalent projet.
- Centraliser les couleurs, typographies, espacements et radius.
- Ne pas mettre des couleurs arbitraires directement dans les widgets métier.
- Le thème doit être appliqué globalement au `MaterialApp` ou équivalent.

### 3.3 Internationalisation

- Minimum obligatoire : `fr` et `en`.
- Prévoir `l10n.yaml` et des fichiers ARB ou une alternative documentée.
- Aucun texte utilisateur ne doit être hardcodé dans les widgets.
- Les formats de date, nombre et devise doivent être compatibles locale.

### 3.4 Configuration applicative

- Centraliser le nom de l’app, les assets de logo, le thème par défaut, les langues supportées et les liens publics dans `AppConfig`.
- Ne pas coder en dur ces valeurs dans les écrans.
- Les valeurs variables par environnement doivent être injectées via `--dart-define`, fichiers de configuration ou mécanisme validé.

## 4. Composants réutilisables

Créer ou réutiliser des composants/widgets pour :

- boutons ;
- champs de saisie ;
- cartes ;
- modales/dialogs ;
- états vide/erreur/chargement ;
- en-têtes de page ;
- badges/statuts ;
- tableaux/listes ;
- navigation ;
- layout de page.

Une interface ne doit pas dupliquer ces éléments dans chaque écran.

## 5. Règle DESIGN.md obligatoire

Avant toute UI Angular ou Flutter, lire ou créer `DESIGN.md`. Le fichier doit contenir les tokens couleurs, typographie, spacing, radius et composants, ainsi que les sections Overview, Colors, Typography, Layout, Elevation & Depth, Shapes, Components et Do's and Don'ts.

Voir `docs/standards/DESIGN-SYSTEM-STANDARDS.md`.

## 6. Critères de rejet en review

Bloquer la PR/MR si :

| Violation | Décision |
|---|---|
| Pas de thème centralisé pour une nouvelle UI | Rejet |
| Pas de dark/light | Rejet sauf ADR |
| Textes visibles hardcodés | Rejet |
| Pas de traduction `fr` / `en` | Rejet |
| Nom app/logo hardcodés dans un écran | Rejet |
| Angular Material introduit sans ADR | Rejet |
| Tailwind CSS v3 ou configuration v3 introduite | Rejet |
| UI significative sans `DESIGN.md` | Rejet |
| Composant dupliqué au lieu d’un shared component | Correction demandée |
| Couleurs/spacing arbitraires non justifiés | Correction demandée |

## 7. Definition of Done UI

Une tâche UI Angular/Flutter est terminée seulement si :

- le thème light est vérifié ;
- le thème dark est vérifié ;
- les textes sont traduits en `fr` et `en` ;
- les composants réutilisables sont utilisés ou créés ;
- la configuration app/branding est centralisée ;
- les états loading/empty/error sont prévus ;
- les tests nécessaires sont listés ou ajoutés ;
- les impacts accessibilité sont vérifiés.


## Règle obligatoire sur les arrondis et ombres

Les interfaces web et mobile doivent privilégier des coins carrés ou légèrement arrondis, avec des ombres sobres.

- Rayon recommandé : 4px à 6px pour cards, formulaires, inputs et boutons.
- Maximum sans ADR : 8px pour les surfaces standards.
- `rounded-full`, `rounded-2xl`, `rounded-3xl` ou équivalents sont interdits par défaut pour cards, formulaires, inputs et boutons.
- Les exceptions doivent être documentées dans `DESIGN.md`.
- La profondeur doit venir d’ombres légères et cohérentes, pas d’un style trop arrondi type mobile/iPhone.

Voir : `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md`.


## Règle obligatoire — SOLID, responsabilités et backend maître

Toute intervention Spring Boot, Angular ou Flutter doit appliquer `docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`.

Règles courtes :
- SOLID doit être respecté à la lettre.
- Le backend est le maître de la vérité métier : validation, décisions, sécurité, persistance et règles critiques.
- Le front web/mobile affiche, collecte et orchestre l’expérience ; il ne porte pas de logique métier complexe ni de vérité critique.
- Les controllers Spring Boot ne contiennent aucune logique métier et ne doivent jamais appeler directement repositories, SQL ou clients externes.
- Les controllers délèguent à des services/use cases ; les services exposés aux controllers doivent avoir un contrat clair et une implémentation dédiée.
- Les composants Angular/Flutter ne doivent pas être monolithiques ; extraire services, facades, use cases, widgets/composants réutilisables.
- Privilégier composition, interfaces et abstractions utiles ; éviter héritage profond et abstractions spéculatives.
- Aucune classe, composant ou widget ne doit dépasser 500 lignes ; alerte dès 300 lignes.
