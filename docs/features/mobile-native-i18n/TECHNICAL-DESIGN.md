# TECHNICAL DESIGN — MOB-2803 Internationalisation Flutter

## 1. Objectif technique

Utiliser le mécanisme standard Flutter `gen_l10n` pour fournir une i18n typée FR/EN et une locale applicative pilotée par Riverpod, sans introduire de dépendance métier ni étendre le thème global.

## 2. Arborescence

```text
mobile/
  l10n.yaml
  lib/
    l10n/
      app_fr.arb
      app_en.arb
    core/
      config/app_config.dart
      i18n/
        locale_controller.dart
        app_locale_formatters.dart
    app/app.dart
```

Le fichier `app_localizations.dart` est généré par Flutter à partir des ARB.

## 3. Configuration Flutter

`pubspec.yaml` active :

- `flutter_localizations` depuis le SDK Flutter ;
- `intl` résolu sous la contrainte du SDK ;
- `flutter.generate: true`.

Le lockfile résolu par le gate Flutter 3.44.6 est versionné afin de garder une baseline reproductible. Le premier `flutter pub get` du ticket a résolu `intl 0.20.2`.

## 4. AppConfig

`AppConfig` ne devient pas un service global de présentation. Il contient uniquement les constantes applicatives transversales nécessaires ici :

- `appName` ;
- `defaultLocale = fr` ;
- `supportedLocales = [fr, en]` ;
- résolution d’une locale supportée.

Une locale avec région (`fr_CM`, `en_GB`, `en_US`) est ramenée au code langue supporté. Toute langue non supportée retombe sur `fr`.

## 5. AppLocaleController

Riverpod expose un état `Locale` indépendant du thème :

```text
platform locale
      ↓
resolveSupportedLocale
      ↓
appLocaleProvider
      ↓
MaterialApp.router(locale)
```

API minimale :

- `setLocale(Locale)` ;
- `useFrench()` ;
- `useEnglish()`.

La préférence n’est pas persistée dans ce lot. Aucun `SharedPreferences`, secure storage ou stockage clinique n’est introduit.

## 6. MaterialApp

`JoprelysApp` observe séparément :

- le router ;
- `themeModeProvider` ;
- `appLocaleProvider`.

Il fournit :

- `locale` ;
- `supportedLocales` ;
- `AppLocalizations.localizationsDelegates` ;
- `onGenerateTitle` localisé.

Le branchement i18n ne modifie ni `AppTheme.light/dark` ni le contrôleur de thème.

## 7. ARB / génération

`app_fr.arb` est le template. `app_en.arb` expose les mêmes clés.

Les clés du ticket ne concernent que la fondation actuellement visible. Les futures features ajoutent leurs propres libellés au mécanisme généré au moment de leur développement ; aucun dictionnaire runtime parallèle n’est ajouté.

## 8. Formats locale

`AppLocaleFormatters` centralise uniquement la conversion d’une valeur typée en texte localisé :

- `formatDate` ;
- `formatTime` ;
- `formatDateTime` ;
- `formatNumber`.

Mapping de données de format :

- `fr` → `fr_FR` ;
- `en` → `en_GB` ;
- autre → fallback produit `fr_FR`.

Les données de dates sont initialisées au bootstrap avec `initializeDateFormatting`.

## 9. Frontière stricte du thème

MOB-2803 n’ajoute aucun token, aucune couleur, aucun rayon, aucune taille et aucune règle de composition à `AppTheme` ou `AppDesignTokens`.

La règle d’architecture est :

```text
core/theme     = primitives visuelles transversales limitées
core/i18n      = locale + formatage transversal
features/*     = composition UX et styles métier propres à la feature
```

Cette séparation reproduit l’intention du web, où `ThemeService` reste petit et ne devient pas un registre de styles de chaque écran.

## 10. Tests

### Locale
- `fr_CM` → `fr` ;
- `en_GB` / `en_US` → `en` ;
- `de_DE` → `fr` ;
- changement explicite FR/EN.

### Widgets
- page fondation en FR ;
- passage EN sans redémarrage ;
- retour FR ;
- thèmes system/light/dark continuent de fonctionner sous la localisation.

### Formats
- dates FR et EN distinctes ;
- heure cohérente ;
- séparateur décimal FR `,` / EN `.` ;
- fallback non supporté = FR.

## 11. Sécurité

Aucun secret, JWT, donnée patient, transcript, audio ou identifiant clinique n’est traité par cette couche.

## 12. Rollback

Le rollback retire le contrôleur i18n, les ARB, les delegates et les helpers de formatage, puis revient à la fondation MOB-2802. Aucune migration DB ou backend n’est impliquée.
