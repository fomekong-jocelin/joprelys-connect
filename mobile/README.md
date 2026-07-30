# Joprelys Connect Mobile

Application mobile professionnelle Flutter de Joprelys Connect.

## Statut

Le socle Flutter comprend désormais :

- bootstrap, Riverpod, `go_router` et CI ;
- design system light/dark/system ;
- internationalisation FR/EN ;
- client réseau central Dio, corrélation, erreurs et résilience bornée.

Documentation principale :

- `../docs/features/mobile-native-foundation/FUNCTIONAL-SPEC.md`
- `../docs/features/mobile-native-foundation/TECHNICAL-DESIGN.md`
- `../docs/features/mobile-native-foundation/TEST-PLAN.md`
- `../docs/features/mobile-native-network/FUNCTIONAL-SPEC.md`
- `../docs/features/mobile-native-network/TECHNICAL-DESIGN.md`
- `../docs/features/mobile-native-network/TEST-PLAN.md`
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
    theme/
  shared/
  features/
    <feature>/
      domain/
      data/
      presentation/
  l10n/

packages/
  joprelys_clinical_audio/
```

## Configuration réseau

La configuration publique est injectée au build :

```bash
flutter run \
  --dart-define=APP_ENV=dev \
  --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

Exemple recette :

```bash
flutter run \
  --dart-define=APP_ENV=recette \
  --dart-define=API_BASE_URL=https://recette-api.joprelys.com
```

Règles :

- `APP_ENV` accepte `dev`, `recette`, `prod` ;
- HTTPS est obligatoire en recette et prod ;
- ces valeurs sont publiques : ne jamais y placer token, clé API ou secret ;
- `apiClientProvider` est consommé par les data sources/repositories, pas par la présentation ;
- la session sécurisée réelle sera branchée dans MOB-2805.

## Vérifications standard

```bash
flutter pub get
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
flutter build apk --debug
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
