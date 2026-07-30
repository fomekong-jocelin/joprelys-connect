# MOB-2804 — Client API mobile, erreurs, corrélation, refresh et résilience réseau

## Statut

IN_REVIEW — issue #252 / PR #253. Branche créée depuis `main` au commit `43e3d0538e5af7873954b49ea273e4ee0e9cf726`, après fusion de MOB-2803.

Gate runtime #2046 vert sur `0c2670a3545bedc60d2f241e9452bc7a6a594de8`. Clôture documentaire et gate final exact-HEAD requis avant fusion.

## Objectif

Fournir la fondation réseau Flutter centralisée et testable nécessaire à MOB-2805 et aux futures features métier, en respectant les contrats backend Joprelys existants.

## Dépendances

- MOB-2801 : DONE ;
- MOB-2802 : DONE ;
- MOB-2803 : DONE — PR #251 fusionnée dans `main` au commit `43e3d053` ;
- backend Joprelys : source de vérité pour authentification, erreurs, RBAC et données métier.

## Implémentation

- [x] environnement `dev/recette/prod` et base URL par `--dart-define` ;
- [x] validation stricte de l’URL et HTTPS hors dev ;
- [x] Dio encapsulé par `ApiClient` ;
- [x] providers Riverpod ;
- [x] timeouts explicites ;
- [x] headers JSON, locale, trace, bearer et idempotence ;
- [x] port `ApiSessionAccess` ;
- [x] distinction session professionnelle / patient ;
- [x] refresh concurrent coordonné ;
- [x] replay d’authentification borné à une fois ;
- [x] retry transitoire borné aux opérations rejouables ;
- [x] mapping enveloppe Joprelys, ProblemDetail et erreurs Dio ;
- [x] CookieJar uniquement en mémoire ;
- [x] tests d’environnement, erreurs, headers, refresh, concurrence, patient, 403 et retry ;
- [x] format/analyze/tests/APK verts dans le gate runtime #2046 ;
- [x] documentation fonctionnelle, technique et tests ;
- [ ] suivi central et changelog ;
- [ ] gate final exact-HEAD.

## Contrat backend vérifié

Le backend actuel :

- accepte le bearer pour les API protégées ;
- expose `/api/auth/refresh` ;
- transporte le refresh professionnel via cookie HttpOnly ;
- émet `X-Trace-Id` ;
- utilise une enveloppe d’erreur canonique pour les APIs générales ;
- conserve également des réponses RFC 7807 pour une partie de l’authentification.

MOB-2804 s’aligne sur ce contrat. Aucun endpoint alternatif ni refresh token lisible par les features n’est inventé.

## Décisions de sécurité

- aucun secret dans `APP_ENV` ou `API_BASE_URL` ;
- aucun stockage persistant dans ce lot ;
- aucune donnée patient/clinique dans les logs ou tests ;
- aucun retry automatique d’une écriture sans idempotence ;
- un 403 ne modifie pas la session ;
- le patient n’utilise pas le refresh professionnel ;
- la persistance sécurisée et le nettoyage de session sont réservés à MOB-2805.

## Limite de centralisation

`core/network` contient seulement les primitives réseau transversales. Les DTO et règles d’une feature restent sous `features/<feature>/data`.

Aucune modification de :

- `AppTheme` ;
- `AppDesignTokens` ;
- `shared/widgets` ;
- composition visuelle métier.

## Preuves runtime

### Gate #2033

Échec de formatage uniquement ; aucun test exécuté.

### Gate #2037

Format vert, analyse ayant détecté les évolutions récentes de Dio (`transformTimeout`), des règles d’initialisation et un statut nullable. Correctifs appliqués sans désactiver le lint.

### Gate #2046

Run ID `30522369209`, HEAD `0c2670a3545bedc60d2f241e9452bc7a6a594de8` :

- `flutter pub get` : vert ;
- lockfile : résolu ;
- Dart format : vert ;
- `flutter analyze` : vert ;
- tests Flutter : verts ;
- `flutter build apk --debug` : vert.

## Critères d’acceptation

- [x] configuration publique et validée ;
- [x] client injectable ;
- [x] trace/locale systématiques ;
- [x] bearer conditionnel ;
- [x] refresh professionnel unique ;
- [x] aucune récupération professionnelle patient ;
- [x] 403 sans refresh/logout ;
- [x] retry borné ;
- [x] erreurs normalisées ;
- [x] aucune fuite PII/secrets ;
- [x] thème inchangé ;
- [x] gate runtime complet vert ;
- [ ] gate final complet vert sur HEAD documentaire exact.

## Estimation

5 SP — 2 à 3 jours senior / 3 à 4 jours intermédiaire.

## Reviewer

Tech Lead Flutter + backend/auth + sécurité mobile + QA.

## Impact SemVer

Fondation interne d’une nouvelle capacité mobile, sans tag ni release dans cette PR. La première livraison mobile publique reste une cible MINOR.

## Suite

MOB-2805 doit implémenter :

- login professionnel ;
- login patient selon le contrat existant ;
- stockage sécurisé de session ;
- refresh réel via le cookie HttpOnly ;
- expiration/nettoyage ;
- biométrie de déverrouillage local ;
- guards de navigation.
