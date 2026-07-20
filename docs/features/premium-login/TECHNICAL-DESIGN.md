# Page de connexion premium — Conception technique

## Architecture existante conservée

- `LoginComponent` reste le composant de présentation et d'orchestration des formulaires.
- `AuthApiService`, `PatientPortalService` et `AuthTokenStorageService` restent inchangés.
- `I18nService` continue de piloter FR/EN.
- `ThemeService` devient disponible sur la page publique afin d'exposer le thème courant et de le modifier.
- `AppLogoComponent` reste l'unique source du logo Joprelys.

## Structure responsive

### Mobile

- grille à une colonne ;
- barre de contrôles langue/thème en haut ;
- logo et introduction centrés ;
- carte de formulaire pleine largeur avec marges latérales ;
- pied de page sous le formulaire ;
- aucun panneau décoratif secondaire obligatoire.

### Tablette et desktop

- conteneur centré avec largeur maximale ;
- panneau de marque institutionnel visible à partir du breakpoint `lg` ;
- formulaire conservé dans une colonne dédiée ;
- aucune donnée clinique fictive ;
- décor réalisé en CSS avec les tokens existants.

## Thème

`LoginComponent` injecte `ThemeService` et expose :

- `theme` : signal partagé ;
- `toggleTheme()` : bascule `light` / `dark` via `ThemeService.setTheme()` ;
- `themeTooltip()` : texte traduit avec les clés existantes du shell.

La persistance reste gérée par `ThemeService` dans `localStorage` sous la clé existante `joprelys.theme`. Aucune donnée sensible n'est ajoutée au stockage.

## Internationalisation

Le template réutilise les clés existantes `login.*`, `shell.*`, `common.footer.*` et `auth.forgotPassword.*`. Les drapeaux sont des indicateurs visuels ; les noms accessibles proviennent de `shell.language.label` et de la locale ciblée.

Aucun nouveau texte métier ne doit être codé en dur.

## Style

- Tailwind CSS v4 pour la structure et les états simples ;
- `login.component.css` pour les formes décoratives, le fond premium, les adaptations de thème et les détails spécifiques à l'écran ;
- couleurs uniquement via les variables du design system ;
- radius `4px`, `6px` ou `8px` ;
- ombres via `var(--shadow-panel)` et `var(--shadow-panel-subtle)` ;
- aucun Angular Material.

## Sécurité

- aucune modification des appels API ;
- aucune exposition supplémentaire de données ;
- conservation des attributs `autocomplete`, `inputmode`, `required` et `aria-required` ;
- conservation du contrôle de `returnUrl` existant ;
- aucun contenu HTML dynamique ni bypass de sanitization.

## Tests automatisés

Les tests composants doivent vérifier :

- présence de `app-logo` ;
- présence des deux boutons de langue avec drapeaux ;
- bascule de locale ;
- présence du contrôle de thème et changement du signal ;
- bascule Personnel / Patient ;
- conservation des tests de connexion et OTP existants.

## Risque et rollback

Risque limité au rendu Angular et aux interactions de présentation. Le rollback consiste à restaurer les trois fichiers `login.component.*` et le test associé. Aucun rollback backend ou base n'est nécessaire.
