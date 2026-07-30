# TECHNICAL DESIGN — MOB-2802 Design system Flutter

## 1. Objectif technique

Introduire une couche de thème Flutter centralisée et testable, consommée par `JoprelysApp` et les futurs écrans, sans dépendance métier ni réseau.

## 2. Arborescence

```text
mobile/lib/
  core/theme/
    app_design_tokens.dart
    app_theme.dart
    theme_controller.dart
  shared/widgets/
    app_button.dart
    app_text_field.dart
    app_card.dart
    app_badge.dart
    app_page_header.dart
    app_loading_state.dart
    app_empty_state.dart
    app_error_state.dart
    app_confirm_dialog.dart
```

## 3. AppDesignTokens

`AppDesignTokens` est la seule source Dart des valeurs visuelles structurantes de ce lot.

### Couleurs de référence

Light :
- primary `#0B91B2` ;
- onPrimary `#FFFFFF` ;
- background `#F7FAFC` ;
- surface `#FFFFFF` ;
- text `#0A1D3D` ;
- secondary `#40556F` ;
- outline `#D8E5E8`.

Dark :
- primary `#22A8C8` ;
- background `#071124` ;
- surface `#111C31` ;
- text principal clair ;
- secondary `#C6D2E1` ;
- outline `#21314B`.

Sémantique commune :
- success `#16A34A` ;
- error `#DC2626` ;
- warning et info centralisés dans le même fichier.

Aucun `Color(0x...)` n’est autorisé dans les widgets/features hors fichier de tokens.

### Géométrie

- spacing : 4 / 8 / 16 / 24 / 32 / 48 ;
- radius : 0 / 2 / 4 / 6 / 8 ;
- minTouchTarget : 44 ;
- borderWidth : 1 ;
- ombre panneau légère et neutre.

## 4. AppTheme

`AppTheme` expose :

```dart
ThemeData get light;
ThemeData get dark;
```

Les deux thèmes configurent au minimum :

- `ColorScheme` ;
- `ScaffoldBackgroundColor` ;
- `TextTheme` ;
- `InputDecorationTheme` ;
- `ElevatedButtonTheme` / `OutlinedButtonTheme` / `TextButtonTheme` ;
- `CardThemeData` ;
- `DialogThemeData` ;
- `DividerThemeData` ;
- focus/disabled states.

Les composants standards ne doivent pas prendre l’apparence pilule par défaut.

## 5. ThemeController

Riverpod expose un `ThemeMode` :

```text
system | light | dark
```

API minimale :

- lire le mode courant ;
- `setMode(ThemeMode mode)` ;
- helpers `useSystem()`, `useLight()`, `useDark()`.

Le contrôleur ne connaît ni UI ni feature métier. La persistance durable sera branchable derrière la même API lors de l’introduction du stockage non sensible.

## 6. JoprelysApp

`JoprelysApp` observe :

- `appRouterProvider` ;
- `themeModeProvider`.

`MaterialApp.router` reçoit exclusivement :

- `theme: AppTheme.light` ;
- `darkTheme: AppTheme.dark` ;
- `themeMode: mode`.

## 7. Widgets partagés

Tous les widgets :

- consomment `Theme.of(context)` et/ou `AppDesignTokens` ;
- n’introduisent aucune couleur métier locale ;
- acceptent libellés et callbacks depuis l’appelant ;
- gardent une surface compacte, testable et accessible.

### AppButton

- variantes primary / secondary / destructive ;
- hauteur minimale 44 px ;
- contenu sur une ligne ;
- radius 4–6 px ;
- état loading/disabled explicite.

### AppTextField

- bordure 1 px ;
- radius 4 px ;
- labels/error/assistive text fournis par l’appelant ;
- support multiline.

### AppCard

- radius 8 px maximum ;
- border + ombre légère ;
- aucun padding arbitraire hors tokens.

### AppBadge

- destiné aux états courts ;
- pas de pill surdimensionnée ;
- couleurs sémantiques centralisées.

### États génériques

`AppLoadingState`, `AppEmptyState`, `AppErrorState` restent purement présentationnels et ne font aucun appel réseau.

### Confirmation

`AppConfirmDialog` fournit une modale maison, cohérente light/dark, et ne dépend d’aucun domaine métier.

## 8. Page de fondation

La page de fondation est utilisée comme banc d’exercice visuel : titre Joprelys, aperçu de primitives et contrôle du mode de thème. Aucun faux patient ni faux indicateur clinique.

## 9. Tests

- thème par défaut = system ;
- changement light/dark/system ;
- `JoprelysApp` applique le mode ;
- couleurs de surface différentes light/dark ;
- boutons >= 44 px ;
- rayons boutons/inputs/cartes conformes ;
- widgets génériques se rendent sans exception ;
- aucun test métier clinique dans ce lot.

## 10. Sécurité

Aucun secret, token, donnée patient, cache clinique ou appel réseau.

## 11. Rollback

Le lot est isolé dans `core/theme` et `shared/widgets`. Un rollback consiste à revenir au thème minimal de MOB-2801 sans migration de données ni changement backend.
