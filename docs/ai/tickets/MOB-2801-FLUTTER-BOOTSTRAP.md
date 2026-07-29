# MOB-2801 — Bootstrap Flutter, architecture feature-first, router et CI mobile

## Statut

IN_PROGRESS — issue #243

## Objectif

Transformer `mobile/` du squelette Flutter généré en fondation applicative Joprelys testable, sans implémenter les écrans métier des stories suivantes.

## Contexte vérifié

- ADR-0004 et EPIC-0028 sont fusionnés via PR #242.
- `mobile/lib/main.dart` contient encore le compteur Flutter généré.
- `mobile/pubspec.yaml` cible Dart `^3.11.5` et ne contient pas Riverpod/go_router.
- la CI actuelle ne détecte que `backend/` et `web/`.
- `pubspec.lock` correspond encore au squelette initial.

## Décision de baseline

MOB-2801 fixe la baseline CI mobile à Flutter 3.44.6 / Dart 3.12.2, version stable officielle vérifiée dans l’archive Flutter. Cela permet d’utiliser les versions stables compatibles retenues au démarrage de l’implémentation :

- `flutter_riverpod: ^3.4.1` ;
- `go_router: ^17.3.0`.

Le lockfile doit être régénéré par `flutter pub get` dans l’environnement Flutter piné puis versionné avant la fusion finale.

## Plan d’action

- [x] Lire les standards projet et ADR-0004.
- [x] Vérifier l’état réel du squelette Flutter et de la CI.
- [x] Créer l’issue #243 et la documentation initiale.
- [ ] Mettre à jour la baseline SDK et les dépendances structurantes.
- [ ] Remplacer le compteur par `main -> bootstrap -> JoprelysApp`.
- [ ] Créer la navigation centralisée et une feature `foundation` minimale.
- [ ] Ajouter les tests smoke/router.
- [ ] Ajouter la détection/job Flutter dans `ci.yml`.
- [ ] Régénérer et versionner `pubspec.lock` via CI.
- [ ] Exécuter le gate final Flutter sur le HEAD exact.
- [ ] Mettre à jour tracking/changelog/docs.

## Critères d’acceptation

- [ ] aucun compteur Flutter généré ;
- [ ] bootstrap séparé ;
- [ ] `ProviderScope` racine et router central ;
- [ ] aucune logique métier dans la présentation ;
- [ ] aucun secret/PII ;
- [ ] Flutter 3.44.6 piné dans la CI ;
- [ ] format/analyze/test/build APK debug verts ;
- [ ] lockfile cohérent avec `pubspec.yaml` ;
- [ ] documentation et suivi à jour.

## Estimation

5 SP — 2 à 3 jours senior / 3 à 4 jours intermédiaire.

## Reviewer

Tech Lead + QA mobile.

## Impact SemVer

MINOR lorsque la fondation runtime mobile devient livrable. Aucun tag/release dans ce ticket.
