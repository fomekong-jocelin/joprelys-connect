# Button label layout — Technical design

## Stack concernée

- Angular ;
- Tailwind CSS v4 CSS-first ;
- design system interne Joprelys Connect.

## Architecture cible

La règle est portée par une feuille globale dédiée :

```text
web/src/styles/button-layout.css
```

Elle est enregistrée après `src/styles.css` dans la liste globale `architect.build.options.styles` de `web/angular.json`.

Ce découpage suit l'organisation recommandée `src/styles/` du standard design system et évite :

- les corrections locales dans chaque template ;
- les styles encapsulés dupliqués ;
- une dépendance à Angular Material ;
- l'injection de styles au runtime.

## Règle CSS

```css
.ui-button {
  flex-shrink: 0;
  line-height: 1.25rem;
  white-space: nowrap;
}
```

### Justification

- `white-space: nowrap` interdit la coupure interne du libellé ;
- `flex-shrink: 0` empêche le conteneur flex de compresser le bouton jusqu'à casser son contenu ;
- `line-height: 1.25rem` stabilise l'alignement vertical avec les icônes et les variantes textuelles.

Le retour à la ligne reste la responsabilité du conteneur d'actions (`flex-wrap`, grille responsive ou empilement mobile). Cette distinction respecte SRP : le bouton garantit son intégrité, le layout décide de la disposition des actions.

## Fichiers impactés

- `web/angular.json` ;
- `web/src/styles/button-layout.css` ;
- `DESIGN.md` ;
- documentation et suivi IA.

## Sécurité

Aucun style inline, aucune dépendance externe et aucun assouplissement CSP ne sont introduits.

## Accessibilité

- cible tactile minimale existante de 42 px conservée ;
- focus visible existant conservé ;
- aucune information transmise uniquement par la couleur ;
- libellé complet conservé, sans troncature.

## Performance

La feuille contient une seule règle globale et est incluse dans le bundle CSS hashé par Angular. L'impact de taille est négligeable.

## Tests

- build Angular production ;
- tests unitaires Angular ;
- recette responsive 320/375/768/1440 px ;
- light/dark ;
- FR/EN ;
- boutons texte, icône + texte, icône seule, disabled.

## Impact SemVer

PATCH : correction visuelle sans changement de contrat métier ou API.
