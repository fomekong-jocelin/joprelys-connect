# TECHNICAL DESIGN — MOB-2801 Fondation Flutter

## 1. Baseline

- Flutter CI piné : `3.44.6`.
- Dart attendu : `3.12.2`.
- `flutter_riverpod: ^3.4.1`.
- `go_router: ^17.3.0`.

Cette baseline est volontairement pinée pour éviter une CI dépendante du canal `stable` mouvant. Elle reste à confirmer par le gate réel.

## 2. Flux de démarrage

```text
main()
  -> bootstrap()
      -> WidgetsFlutterBinding.ensureInitialized()
      -> ProviderScope
          -> JoprelysApp
              -> appRouterProvider
              -> MaterialApp.router
```

## 3. Fichiers cibles

```text
mobile/lib/
  main.dart
  bootstrap.dart
  app/
    app.dart
    router/
      app_router.dart
      route_names.dart
  core/
    config/
      app_config.dart
  features/
    foundation/
      presentation/
        pages/
          foundation_page.dart
```

Les autres features ne sont pas créées artificiellement tant qu’elles ne contiennent aucun code : Git ne versionne pas d’arborescences vides et chaque story créera ses couches `domain/data/presentation` utiles.

## 4. Responsabilités

### `main.dart`
Point d’entrée uniquement. Aucune configuration métier.

### `bootstrap.dart`
Initialisation framework/DI. Aucun widget métier.

### `JoprelysApp`
Composition de `MaterialApp.router`. Aucun appel réseau ni règle clinique.

### `appRouterProvider`
Construit le `GoRouter`. Seule la route de fondation est exposée dans MOB-2801.

### `FoundationPage`
Surface de démarrage neutre. Pas de fausses données, pas de navigation vers des modules non livrés.

### `AppConfig`
Centralise au minimum le nom applicatif. MOB-2803 l’étendra pour locale, URLs publiques et paramètres d’environnement.

## 5. Thème et i18n

MOB-2801 ne livre pas le design system complet ni l’i18n complète. Pour ne pas introduire de dette :

- aucune couleur brand arbitraire n’est codée en dur ;
- `ThemeData.light/dark` standards peuvent servir uniquement au shell de fondation ;
- la seule chaîne visible autorisée dans la surface de fondation est le nom applicatif provenant de `AppConfig` ;
- MOB-2802 remplace le thème générique par les tokens `DESIGN.md` ;
- MOB-2803 introduit ARB/gen_l10n pour tous les libellés métier.

## 6. CI

Le job `changes` expose un troisième output `mobile`.

Détection :

```text
^mobile/
```

Un changement de `.github/workflows/ci.yml` force les trois stacks par sécurité.

Job mobile :

```bash
flutter pub get
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
flutter build apk --debug
```

Le workflow utilise `subosito/flutter-action@v2` avec `flutter-version: '3.44.6'` et cache activé.

Pour le premier passage, `pubspec.lock` généré est uploadé comme artifact afin de pouvoir versionner exactement le lockfile résolu avant le gate final.

## 7. Sécurité

- aucune donnée sensible ;
- aucune permission ;
- aucun accès réseau ;
- aucun secure storage dans ce lot ;
- aucun log clinique ;
- aucun secret de build.

## 8. Tests

- smoke test du `ProviderScope -> JoprelysApp -> FoundationPage` ;
- test de création du router et de sa route initiale ;
- analyse statique ;
- build APK debug.

## 9. Régression

Backend et Angular ne changent pas. La modification du workflow commun doit toutefois exécuter leurs gates sur le HEAD final, car `ci.yml` est une infrastructure partagée.

## 10. SemVer

Le code introduit une nouvelle capacité runtime mobile mais aucune release n’est demandée. Impact cible produit : MINOR lors de la première livraison mobile.
