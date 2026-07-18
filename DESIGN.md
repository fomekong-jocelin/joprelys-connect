---
version: 1.0.0
name: Joprelys Connect Design System
description: Design system central de Joprelys Connect (Google/Material 3 & Tailwind CSS v4 CSS-first).
colors:
  primary: '#0b91b2'       # Cyan Médical (Light) / #22a8c8 (Dark)
  on-primary: '#ffffff'
  secondary: '#40556f'     # Slate Blue (Light) / #c6d2e1 (Dark)
  on-secondary: '#ffffff'
  background: '#f7fafc'    # Light Gray (Light) / #071124 (Dark)
  on-background: '#0a1d3d' # Brand Night
  surface: '#ffffff'       # White (Light) / #111c31 (Dark)
  on-surface: '#0a1d3d'
  outline: '#d8e5e8'       # Light border (Light) / #21314b (Dark)
  error: '#dc2626'
  on-error: '#ffffff'
typography:
  headline-lg:
    fontFamily: Montserrat, sans-serif
    fontSize: 32px
    fontWeight: 800
    lineHeight: 40px
  headline-md:
    fontFamily: Montserrat, sans-serif
    fontSize: 24px
    fontWeight: 700
    lineHeight: 32px
  body-lg:
    fontFamily: Inter, sans-serif
    fontSize: 18px
    fontWeight: 400
    lineHeight: 28px
  body-md:
    fontFamily: Inter, sans-serif
    fontSize: 16px
    fontWeight: 400
    lineHeight: 24px
  body-sm:
    fontFamily: Inter, sans-serif
    fontSize: 14px
    fontWeight: 400
    lineHeight: 20px
  label-lg:
    fontFamily: Inter, sans-serif
    fontSize: 14px
    fontWeight: 600
    lineHeight: 20px
  label-md:
    fontFamily: Inter, sans-serif
    fontSize: 12px
    fontWeight: 600
    lineHeight: 16px
  label-sm:
    fontFamily: Inter, sans-serif
    fontSize: 11px
    fontWeight: 600
    lineHeight: 16px
rounded:
  none: 0px
  xs: 2px
  sm: 4px
  md: 6px
  lg: 8px
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  2xl: 48px
components:
  button-primary:
    backgroundColor: 'var(--brand-primary)'
    textColor: 'var(--on-primary)'
    typography: '{typography.label-lg}'
    rounded: '{rounded.sm}'
    padding: '{spacing.sm} {spacing.md}'
  card-default:
    backgroundColor: 'var(--app-surface)'
    textColor: 'var(--text-primary)'
    rounded: '{rounded.lg}'
    padding: '{spacing.lg}'
  input-default:
    backgroundColor: 'var(--bg-input)'
    textColor: 'var(--text-primary)'
    rounded: '{rounded.sm}'
    padding: '{spacing.sm} {spacing.md}'
---

## Overview

Joprelys Connect est un outil de santé destiné aux cliniques pilotes et aux patients (Unified Patient Record / DPU). L'interface doit véhiculer confiance, crédibilité médicale, et simplicité d'usage, tout en évitant de surcharger cognitivement les praticiens.

## Colors

Les couleurs s'appuient sur :
- **Brand Night (#0A1D3D)** : pour le texte principal, renvoie au sérieux institutionnel.
- **Brand Cyan (#0B91B2)** : pour les actions principales (primaire), renvoie au monde médical et de la santé.
- **Brand Green (#16A34A)** : pour les indicateurs de succès et de validité.
- **Thème Light / Dark** : géré de manière globale par la classe `html[data-theme='dark']` et les variables CSS centralisées dans `styles.css`.

## Typography

Le projet utilise deux familles de polices :
1. **Montserrat** : pour les titres importants (Headline) afin de conférer de l'autorité et du caractère.
2. **Inter** : pour les textes courants (Body, Labels, Formulaires) assurant une excellente lisibilité à petite taille.

## Layout

Grille responsive basée sur 12 colonnes standard (desktop) et comportement empilé (mobile) avec une largeur de page maximale de `1120px` centrée (`.app-container`).

## Elevation & Depth

Le projet utilise des bordures discrètes (`1px solid var(--app-border)`) combinées avec une ombre très légère (`var(--shadow-panel)`) pour séparer les plans, au lieu d'ombres portées massives ou colorées.

## Shapes

Conformément à la règle des arrondis sobres :
- Coins carrés ou très légèrement arrondis.
- Boutons et formulaires : `4px` (`rounded-sm`).
- Cartes / Fiche Patient / Modales : `8px` max (`rounded-lg`).
- **Strictement interdit** : les boutons ou inputs en forme de pilule (`rounded-full`) hors avatars circulaires ou petits badges d'état circulaires approuvés.

## Components

### Buttons
Les boutons sont définis dans `shared/ui/button.component.ts`. Ils héritent de la charte de couleurs dynamiques et ont un arrondi de `4px` à `6px`.

### Inputs
Les champs texte utilisent un fond gris clair `var(--bg-input)` avec bordure fine, basculant vers un fond sombre en mode dark, et un arrondi de `4px`.

### Cards
Les conteneurs de cartes utilisent la classe `.ui-card` avec `8px` d'arrondi et l'ombre légère centralisée.

### Confirmation dialogs

Les actions financières destructives utilisent une modale maison, jamais le `confirm()` du navigateur. La surface conserve un rayon sobre compris entre `4px` et `8px`, l’overlay est centralisé, le focus reste visible, la fermeture par Échap est disponible et l’action destructive est clairement distincte dans les thèmes light et dark.

### Structure hospitalière

La configuration Service → Chambre → Lit utilise une hiérarchie de panneaux à bordure fine, avec un rayon maximal de `6px` et les ombres légères du design system. Les actions de création restent au niveau de leur parent, les opérations destructives passent par la modale de confirmation partagée et les états de lits conservent les couleurs sémantiques communes aux thèmes light/dark.

### Disponibilités médecin (STORY-2602)

La page « Mes disponibilités » (`clinic/availability`) introduit la grille hebdomadaire `shared/ui/weekly-availability-grid` (7 colonnes Lun → Dim, défilement horizontal sur mobile) et l'aperçu des créneaux. Les plages horaires sont des cartes compactes à rayon sobre (≤ `8px`, tokens `--radius-brand-*`) avec ombre légère `var(--shadow-panel)` ; le jour sélectionné est souligné par `var(--brand-primary)` et les plages désactivées sont atténuées (`opacity-50`). Toutes les couleurs passent par les tokens centralisés (`--app-surface`, `--app-border`, `--brand-primary`, `--text-*`) — aucune couleur en dur, thèmes light/dark automatiques. La désactivation d'une plage et la suppression d'une indisponibilité passent par la modale de confirmation partagée.

## Do's and Don'ts

### Do
- Toujours utiliser les variables CSS centrales de `styles.css`.
- Respecter les contrastes légaux en mode light et dark.
- Maintenir l'internationalisation FR/EN de chaque libellé.

### Don't
- **Interdiction d'importer `@angular/material`** ou d'utiliser ses directives.
- Interdiction de Tailwind v3 ou d'utiliser un fichier de configuration externe `tailwind.config.js`.
- Ne pas utiliser d'arrondis supérieurs à `8px` sur les cartes et boutons.
