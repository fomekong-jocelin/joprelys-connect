# MOB-2803 — Internationalisation Flutter FR/EN et formats locale

## Statut

IN_PROGRESS — issue #250 / PR #251 ; branche créée depuis le `main` courant `5bf45b5c71d530e0fd73bfe28e9857a474e0b59f` après fusion de MOB-2802.

## Objectif

Mettre en place la fondation d’internationalisation native de Joprelys Connect avant les écrans métier : français et anglais, bascule à chaud, résolution de la locale système et formats date/heure/nombre cohérents.

## Dépendances

- MOB-2801 : DONE ;
- MOB-2802 : DONE — PR #246 fusionnée dans `main` au commit `311f60a123ea1ad278bdda84028342a2741031b5` ;
- `main` a ensuite évolué jusqu’à `5bf45b5c71d530e0fd73bfe28e9857a474e0b59f` avant création de la branche MOB-2803.

## Limite de centralisation du thème

MOB-2803 ne modifie ni `AppTheme` ni `AppDesignTokens`.

Le principe reste aligné sur le web :

- le thème global reste transversal et limité aux couleurs sémantiques, surfaces, géométrie et primitives réellement communes ;
- l’i18n est un mécanisme transversal distinct ;
- les choix visuels propres à une feature restent dans la feature ou ses composants ;
- aucune future feature ne doit pousser ses styles métier dans `AppTheme` pour obtenir une apparence uniforme.

## Implémentation

- [x] ajouter `flutter_localizations` et `intl` ;
- [x] activer `gen_l10n` ;
- [x] créer les ARB FR/EN ;
- [x] définir locale par défaut et locales supportées dans `AppConfig` ;
- [x] créer `AppLocaleController` Riverpod ;
- [x] résoudre la locale système avec fallback français ;
- [x] brancher delegates/locales/locale dans `MaterialApp.router` ;
- [x] localiser la page de fondation ;
- [x] ajouter les formats date/heure/nombre ;
- [x] ajouter les tests locale et formats ;
- [x] versionner le lockfile résolu par Flutter 3.44.6 ;
- [ ] obtenir format/analyze/tests/APK verts ;
- [ ] finaliser le suivi et le changelog ;
- [ ] gate final exact-HEAD vert.

## Décisions

### Locale initiale

- locale système `fr*` → `fr` ;
- locale système `en*` → `en` ;
- locale absente ou non supportée → `fr`.

La sélection utilisateur change la locale sans redémarrage. La persistance durable de la préférence n’est pas ajoutée tant que la couche de préférences non sensibles n’est pas définie ; aucun stockage sensible n’est utilisé pour cela.

### Formats

- français : données de format `fr_FR` ;
- anglais : données de format `en_GB` ;
- API de formatage centralisée uniquement pour éviter les divergences de date/heure/nombre ;
- aucun format monétaire métier n’est introduit dans ce ticket.

## Critères d’acceptation

- [x] FR/EN déclarés dans ARB ;
- [x] fallback FR déterministe ;
- [x] bascule FR/EN à chaud ;
- [x] dates/heures/nombres dépendants de la locale ;
- [x] page fondation sans libellés utilisateur de langue/thème codés en dur ;
- [x] `AppTheme` inchangé ;
- [x] `AppDesignTokens` inchangé ;
- [x] aucun secret/PII ;
- [ ] CI mobile finale verte sur HEAD exact.

## Estimation

3 SP — 1 à 2 jours senior / 2 à 3 jours intermédiaire.

## Reviewer

Tech Lead + QA mobile + reviewer UX/i18n.

## Impact SemVer

Pas de tag ni de release dans ce ticket. La première livraison mobile publique reste une cible MINOR.
