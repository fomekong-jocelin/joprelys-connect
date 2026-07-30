# Joprelys Connect Mobile

Application mobile professionnelle Flutter de Joprelys Connect.

## Statut

Le socle Flutter comprend désormais :

- bootstrap, Riverpod, `go_router` et CI ;
- design system light/dark/system ;
- internationalisation FR/EN ;
- client réseau central Dio, corrélation, erreurs et résilience bornée ;
- authentification professionnelle, OTP, session sécurisée, refresh par cookie HttpOnly, biométrie locale et guards de navigation.

MOB-2805 est fusionné dans `main`. Les APK de validation ciblent la recette par défaut ; les builds de développement et de production doivent toujours fournir explicitement leur environnement.

Documentation principale :

- `../docs/features/mobile-native-foundation/FUNCTIONAL-SPEC.md`
- `../docs/features/mobile-native-foundation/TECHNICAL-DESIGN.md`
- `../docs/features/mobile-native-foundation/TEST-PLAN.md`
- `../docs/features/mobile-native-network/FUNCTIONAL-SPEC.md`
- `../docs/features/mobile-native-network/TECHNICAL-DESIGN.md`
- `../docs/features/mobile-native-network/TEST-PLAN.md`
- `../docs/features/mobile-native-auth/FUNCTIONAL-SPEC.md`
- `../docs/features/mobile-native-auth/TECHNICAL-DESIGN.md`
- `../docs/features/mobile-native-auth/TEST-PLAN.md`
- `../docs/ai/adr/ADR-0004-mobile-flutter-native-architecture.md`
- `../docs/pm/backlog/EPIC-0028-joprelys-connect-mobile-native.md`

Ne pas développer les features directement dans `main.dart` et ne pas appeler Dio depuis les widgets.

## Principes non négociables

- Android-first, iOS-ready ;
- architecture feature-first `domain / data / presentation` ;
- backend Joprelys maître de la vérité métier ;
- thème light/dark/system limité aux primitives visuelles communes ;
- internationalisation FR/EN via ARB ;
- aucun texte utilisateur hardcodé ;
- aucun secret dans le code ou `--dart-define` ;
- données sensibles uniquement dans un stockage sécurisé ;
- mot de passe et OTP jamais persistés ;
- refresh token exclusivement dans le cookie HttpOnly ;
- biométrie limitée au déverrouillage local ;
- séparation stricte des sessions professionnelle et patient ;
- aucun cache local générique du DPU ;
- design system aligné avec `../DESIGN.md` ;
- audio clinique natif isolé du reste de l’application ;
- tests et CI obligatoires avant merge.

## Architecture cible

```text
lib/
  app/
  core/
    config/
    i18n/
    network/
    security/
    theme/
  shared/
  features/
    <feature>/
      domain/
      data/
      application/
      presentation/
  l10n/

packages/
  joprelys_clinical_audio/
```

## Configuration réseau

La configuration publique est injectée au build.

Par défaut, un APK lancé sans `--dart-define` cible la recette :

```text
APP_ENV=recette
API_BASE_URL=https://recette.joprelys.com
```

Développement local explicite :

```bash
flutter run \
  --dart-define=APP_ENV=dev \
  --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

Build de validation recette explicite :

```bash
flutter build apk --debug \
  --dart-define=APP_ENV=recette \
  --dart-define=API_BASE_URL=https://recette.joprelys.com
```

Règles :

- `APP_ENV` accepte `dev`, `recette`, `prod` ;
- HTTPS est obligatoire en recette et prod ;
- ne jamais utiliser l’URL de production dans un APK de validation ;
- ces valeurs sont publiques : ne jamais y placer token, clé API ou secret ;
- `apiClientProvider` est consommé par les data sources/repositories, pas par la présentation ;
- `apiSessionAccessProvider` est branché sur `AuthSessionManager` au bootstrap.

## Authentification professionnelle

Contrats consommés :

- `POST /api/auth/login` ;
- `POST /api/auth/verify-otp` ;
- `POST /api/auth/refresh` ;
- `POST /api/auth/logout`.

La session et les cookies sont persistés via `flutter_secure_storage`. Les features n’accèdent jamais au refresh token. Le Dio d’authentification ne contient pas l’intercepteur de recovery afin d’éviter toute récursion du refresh.

La biométrie est optionnelle. Elle verrouille localement une session déjà authentifiée lorsque l’application passe en arrière-plan ; elle ne remplace jamais le login serveur.

## Vérifications standard

```bash
flutter pub get
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test
flutter build apk --debug \
  --dart-define=APP_ENV=recette \
  --dart-define=API_BASE_URL=https://recette.joprelys.com
```

## Références

Lire avant toute intervention mobile :

- `../AGENTS.md`
- `../SKILL.md`
- `../DESIGN.md`
- `../docs/standards/FRONTEND-MOBILE-STANDARDS.md`
- `../docs/standards/DESIGN-SYSTEM-STANDARDS.md`
- `../docs/standards/ARCHITECTURE-SOLID-RESPONSIBILITY-STANDARDS.md`
- `../docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md`
