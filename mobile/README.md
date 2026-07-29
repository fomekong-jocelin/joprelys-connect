# Joprelys Connect Mobile

Application mobile professionnelle Flutter de Joprelys Connect.

## Statut

Le dossier `mobile/` contient actuellement le squelette Flutter initial. L’architecture cible est définie dans :

- `../docs/features/mobile-native-foundation/FUNCTIONAL-SPEC.md`
- `../docs/features/mobile-native-foundation/TECHNICAL-DESIGN.md`
- `../docs/features/mobile-native-foundation/TEST-PLAN.md`
- `../docs/ai/adr/ADR-0004-mobile-flutter-native-architecture.md`
- `../docs/pm/backlog/EPIC-0028-joprelys-connect-mobile-native.md`

Ne pas développer les features directement dans le `main.dart` généré.

## Principes non négociables

- Android-first, iOS-ready ;
- architecture feature-first `domain / data / presentation` ;
- backend Joprelys maître de la vérité métier ;
- thème centralisé `light / dark / system` ;
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

## Vérifications standard

```bash
flutter pub get
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
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
