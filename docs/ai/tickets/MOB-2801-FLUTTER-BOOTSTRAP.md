# MOB-2801 — Bootstrap Flutter, architecture feature-first, router et CI mobile

## Statut

IN_REVIEW — issue #243 / PR #244 ; gate complet #1962 vert sur le HEAD runtime `476191772ef368bec0fa9346a13d723d9e7e08d6`, gate final post-documentation requis.

## Objectif

Transformer `mobile/` du squelette Flutter généré en fondation applicative Joprelys testable, sans implémenter les écrans métier des stories suivantes.

## Contexte vérifié

- ADR-0004 et EPIC-0028 sont fusionnés via PR #242.
- le compteur Flutter généré a été retiré ;
- Flutter est piné en CI à `3.44.6`, avec Dart `3.12.2` ;
- Riverpod et go_router sont intégrés ;
- la CI sait désormais détecter et valider `mobile/**`.

## Baseline livrée

- `flutter_riverpod: ^3.4.1` — lock résolu `3.4.2` ;
- `go_router: ^17.3.0` — lock résolu `17.3.0` ;
- `pubspec.lock` régénéré par Flutter puis versionné ;
- `main -> bootstrap -> ProviderScope -> JoprelysApp -> GoRouter` ;
- une seule route neutre de fondation `/`, sans faux parcours clinique.

## Plan d’action

- [x] Lire les standards projet et ADR-0004.
- [x] Vérifier l’état réel du squelette Flutter et de la CI.
- [x] Créer l’issue #243 et la documentation initiale.
- [x] Mettre à jour la baseline SDK et les dépendances structurantes.
- [x] Remplacer le compteur par `main -> bootstrap -> JoprelysApp`.
- [x] Créer la navigation centralisée et une feature `foundation` minimale.
- [x] Ajouter les tests smoke/router.
- [x] Ajouter la détection/job Flutter dans `ci.yml`.
- [x] Régénérer et versionner `pubspec.lock` via CI.
- [x] Exécuter un gate complet runtime : run #1962 vert, backend + frontend + Flutter.
- [x] Mettre à jour tracking/changelog/docs.
- [ ] Exécuter le gate final sur le HEAD documentaire exact de la PR #244.

## Critères d’acceptation

- [x] aucun compteur Flutter généré ;
- [x] bootstrap séparé ;
- [x] `ProviderScope` racine et router central ;
- [x] aucune logique métier dans la présentation ;
- [x] aucun secret/PII ;
- [x] Flutter 3.44.6 piné dans la CI ;
- [x] format/analyze/test/build APK debug verts dans #1962 ;
- [x] lockfile cohérent et parsable avec `pubspec.yaml` ;
- [x] documentation et suivi mis à jour dans le lot final ;
- [ ] gate final post-documentation vert.

## Preuves techniques

Run GitHub Actions #1962 / ID `30501163481` sur `476191772ef368bec0fa9346a13d723d9e7e08d6` :

- change detection : vert ;
- backend Maven : vert ;
- frontend Angular tests + build : vert ;
- Flutter `pub get` : vert ;
- Dart format : vert ;
- `flutter analyze` : vert ;
- `flutter test` : vert ;
- `flutter build apk --debug` : vert.

Les deux échecs mobiles précédents étaient des problèmes de mise en forme puis de copie du lockfile ; aucune baisse de couverture ni suppression de test n’a été utilisée pour les résoudre.

## Estimation

5 SP — 2 à 3 jours senior / 3 à 4 jours intermédiaire.

## Reviewer

Tech Lead + QA mobile.

## Impact SemVer

MINOR lorsque la fondation runtime mobile devient livrable. Aucun tag/release dans ce ticket.
