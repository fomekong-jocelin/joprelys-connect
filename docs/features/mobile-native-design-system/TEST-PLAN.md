# TEST PLAN — MOB-2802 Design system Flutter

## Objectif

Valider que le design system Flutter est centralisé, cohérent light/dark/system, accessible et réutilisable sans dépendance métier.

## Tests unitaires / widgets

### TH-01 — Mode initial
- créer un `ProviderContainer` ;
- vérifier que le mode initial est `ThemeMode.system`.

### TH-02 — Bascule light
- appeler `useLight()` ;
- vérifier `ThemeMode.light`.

### TH-03 — Bascule dark
- appeler `useDark()` ;
- vérifier `ThemeMode.dark`.

### TH-04 — Retour system
- changer de mode puis appeler `useSystem()` ;
- vérifier `ThemeMode.system`.

### TH-05 — App intégrée
- monter `JoprelysApp` sous `ProviderScope` ;
- vérifier que `MaterialApp` reçoit les thèmes Joprelys centralisés.

### DS-01 — AppButton
- rendu primary/secondary/destructive ;
- hauteur >= 44 px ;
- état disabled ;
- état loading ;
- rayon <= 6 px.

### DS-02 — AppTextField
- rayon 4 px ;
- support label/hint/error ;
- support multiline.

### DS-03 — AppCard
- border visible ;
- rayon <= 8 px ;
- surface différente en light/dark.

### DS-04 — AppBadge
- rendu des états sémantiques ;
- pas de rayon pilule arbitraire.

### DS-05 — Page header
- titre et sous-titre facultatif ;
- action facultative ;
- pas de débordement sur largeur mobile.

### DS-06 — Loading / Empty / Error
- rendu sans exception ;
- messages fournis par l’appelant ;
- retry facultatif sur erreur.

### DS-07 — Confirmation
- titre/message/actions fournis par l’appelant ;
- action destructive visuellement distincte ;
- fermeture propre.

## Non-régression

- tests router MOB-2801 toujours verts ;
- smoke test application toujours vert ;
- aucun changement backend/web requis.

## Gate CI

- `flutter pub get` ;
- `dart format --output=none --set-exit-if-changed .` ;
- `flutter analyze` ;
- `flutter test` ;
- `flutter build apk --debug`.

## Validation manuelle

Sur émulateur/appareil Android :
- vérifier light/dark/system ;
- vérifier lisibilité et contraste ;
- vérifier absence de contrôles pilule ;
- vérifier zones tactiles principales ;
- vérifier absence de faux contenu clinique.

## Critère de sortie

Aucun merge sans gate final vert sur le HEAD exact de la PR.
