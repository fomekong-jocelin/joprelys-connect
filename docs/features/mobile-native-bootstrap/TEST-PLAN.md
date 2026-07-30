# TEST PLAN — MOB-2801 Fondation Flutter

## Objectif

Prouver que la fondation Flutter démarre, route correctement et reste isolée des fonctionnalités métier non encore livrées.

## Tests unitaires / widgets

### T01 — Bootstrap application

- lancer `JoprelysApp` dans un `ProviderScope` ;
- vérifier que la surface `FoundationPage` est rendue ;
- vérifier l’absence du compteur Flutter généré.

### T02 — Router initial

- créer le router via `createAppRouter()` ;
- vérifier que la route initiale est `/` ;
- vérifier que le nom de route de fondation est stable.

### T03 — Configuration centrale

- vérifier que le titre de l’application provient de `AppConfig` ;
- aucune duplication de branding dans la page.

## Gate statique

```bash
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
```

## Gate build

```bash
flutter build apk --debug
```

## Gate dépendances

```bash
flutter pub get
git diff --exit-code -- pubspec.lock
```

Le second contrôle devient obligatoire après que le lockfile généré par la CI initiale a été versionné.

## CI / détection

- modification `mobile/**` => job mobile `true` ;
- modification uniquement `web/**` => mobile `false` ;
- modification uniquement `backend/**` => mobile `false` ;
- modification de `.github/workflows/ci.yml` => backend/frontend/mobile `true`.

## Sécurité

Vérifier par review :

- aucun token/secret ;
- aucune URL backend codée en dur ;
- aucune donnée patient ;
- aucun log sensible ;
- aucune permission Android/iOS nouvelle dans ce lot.

## Critère de sortie

MOB-2801 ne passe DONE qu’après gate final vert sur le HEAD exact contenant le `pubspec.lock` résolu et les mises à jour documentaires finales.
