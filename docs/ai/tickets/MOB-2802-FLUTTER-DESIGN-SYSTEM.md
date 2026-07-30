# MOB-2802 — Design system Flutter, thèmes light/dark/system et widgets partagés

## Statut

IN_REVIEW — issue #245 / PR #246 ; implémentation et documentation terminées, gate runtime #1983 vert sur `ab44982d286c2a4e9638aa85c5a704f480385a3f`, gate final exact-HEAD requis avant fusion.

## Objectif

Construire le design system Flutter partagé de Joprelys Connect en appliquant `DESIGN.md`, avant toute UI métier mobile considérée DONE.

## Dépendance

MOB-2801 : DONE — PR #244 fusionnée par squash dans `main` au commit `ca245a74194d7ba7075084f6aa03465d70d851d1`.

## Plan d’action

- [x] créer l’issue #245 ;
- [x] créer la branche depuis le commit de fusion MOB-2801 ;
- [x] rédiger la spec fonctionnelle ;
- [x] rédiger le technical design ;
- [x] rédiger le plan de tests ;
- [x] créer `AppDesignTokens` ;
- [x] créer `AppTheme.light` / `AppTheme.dark` ;
- [x] créer le `ThemeController` Riverpod `system/light/dark` ;
- [x] brancher `JoprelysApp` ;
- [x] créer les widgets partagés ;
- [x] adapter la page de fondation ;
- [x] ajouter les tests ;
- [x] mettre à jour tracking/changelog ;
- [x] ouvrir la PR Draft ;
- [x] obtenir un gate runtime complet vert (#1983) ;
- [ ] passer Ready sur le HEAD documentaire final ;
- [ ] obtenir le gate CI final vert sur ce HEAD exact.

## Critères d’acceptation

- [x] tokens centralisés ;
- [x] thèmes Joprelys light/dark ;
- [x] contrôleur system/light/dark ;
- [x] rayons sobres conformes à `DESIGN.md` ;
- [x] actions tactiles principales >= 44 px ;
- [x] composants partagés sans palette locale ;
- [x] aucun faux contenu clinique ;
- [x] aucun secret/PII ;
- [x] format/analyze/test/APK verts dans le gate runtime #1983 ;
- [x] documentation et suivi à jour ;
- [ ] gate final exact-HEAD vert.

## Preuves runtime

Run GitHub Actions #1983 / ID `30512157357` sur `ab44982d286c2a4e9638aa85c5a704f480385a3f` :

- détection des stacks : verte ;
- Dart format : vert ;
- `flutter analyze` : vert ;
- 9 tests Flutter : verts ;
- `flutter build apk --debug` : vert.

Les itérations précédentes ont corrigé un formatage Dart puis un finder de test ambigu sur le libellé `Confirm`. Aucun test n’a été supprimé ou désactivé et aucune couverture n’a été affaiblie.

## Estimation

5 SP — 2 à 3 jours senior / 3 à 4 jours intermédiaire.

## Reviewer

Tech Lead + QA mobile + UX.

## Impact SemVer

Pas de release/tag dans ce ticket. La première livraison mobile publique reste une cible MINOR.
