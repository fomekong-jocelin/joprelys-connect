# TEST PLAN — Branding adaptatif et headers mobiles fixes

## Tests automatisés

- [ ] Login light : wordmark officiel dans ses couleurs, sans surface blanche
  ajoutée par le widget.
- [ ] Login dark : apparence contrastée et transparente.
- [ ] Dashboard dark : pictogramme lisible et sémantique de marque présente.
- [ ] Dashboard 393 × 852 : aucun overflow.
- [ ] Dashboard après drag vertical : app bar à la même position, contenu déplacé.
- [ ] Pull-to-refresh toujours câblé au contrôleur de file.
- [ ] Header SOAP fixe : titre/actions visibles après défilement.
- [ ] Motif SOAP dans le contenu défilant.
- [ ] FR/EN et light/dark sans exception Flutter.
- [ ] Goldens sombres mis à jour et inspectés.

## Commandes

```powershell
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test
flutter test test/auth_ui_golden_test.dart --update-goldens
flutter test test/auth_ui_golden_test.dart
```

## Recette manuelle Android

- [ ] 360 × 800 et 393 × 852 ;
- [ ] thème light, dark et system ;
- [ ] français et anglais ;
- [ ] texte système 100 % puis 130 % ;
- [ ] clavier ouvert au login et dans la note ;
- [ ] scroll long dashboard : header toujours visible ;
- [ ] pull-to-refresh ;
- [ ] ouverture SOAP, scroll jusqu’au plan, fermeture et assistant vocal.

## Non-régression sécurité

- [ ] aucun token/mot de passe/OTP dans les logs ou fixtures ;
- [ ] logout, biométrie et guards inchangés ;
- [ ] aucune donnée patient réelle ajoutée aux tests ou goldens.

## Critères de sortie

Toutes les vérifications automatisées sont vertes. La recette Android réelle peut
rester ouverte uniquement si elle est tracée dans le ticket et le suivi projet.
